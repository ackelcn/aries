/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.transaction.jms.internal;

import java.io.Serializable;
import java.lang.IllegalStateException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jms.*;
import javax.transaction.xa.XAResource;

import org.apache.commons.pool.KeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PooledSession implements Session, TopicSession, QueueSession, XASession {
    private static final transient Logger LOG = LoggerFactory.getLogger(PooledSession.class);

    private final SessionKey key;
    private final KeyedObjectPool<SessionKey, PooledSession> sessionPool;
    private final CopyOnWriteArrayList<MessageConsumer> consumers = new CopyOnWriteArrayList<MessageConsumer>();
    private final CopyOnWriteArrayList<QueueBrowser> browsers = new CopyOnWriteArrayList<QueueBrowser>();
    private final CopyOnWriteArrayList<PooledSessionEventListener> sessionEventListeners = new CopyOnWriteArrayList<PooledSessionEventListener>();

    private MessageProducer producer;
    private TopicPublisher publisher;
    private QueueSender sender;

    private Session session;
    private boolean transactional = true;
    private boolean ignoreClose;
    private boolean isXa;
    private boolean useAnonymousProducers = true;

    public PooledSession(SessionKey key, Session session, KeyedObjectPool<SessionKey, PooledSession> sessionPool, boolean transactional, boolean anonymous) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1158
        this.key = key;
        this.session = session;
        this.sessionPool = sessionPool;
        this.transactional = transactional;
        this.useAnonymousProducers = anonymous;
    }

    public void addSessionEventListener(PooledSessionEventListener listener) {
        // only add if really needed
        if (!sessionEventListeners.contains(listener)) {
            this.sessionEventListeners.add(listener);
        }
    }

    protected boolean isIgnoreClose() {
        return ignoreClose;
    }

    protected void setIgnoreClose(boolean ignoreClose) {
        this.ignoreClose = ignoreClose;
    }

    @Override
    public void close() throws JMSException {
        if (!ignoreClose) {
            boolean invalidate = false;
            try {
                // lets reset the session
                getInternalSession().setMessageListener(null);

                // Close any consumers and browsers that may have been created.
                for (Iterator<MessageConsumer> iter = consumers.iterator(); iter.hasNext();) {
                    MessageConsumer consumer = iter.next();
                    consumer.close();
                }

                for (Iterator<QueueBrowser> iter = browsers.iterator(); iter.hasNext();) {
                    QueueBrowser browser = iter.next();
                    browser.close();
                }

                if (transactional && !isXa) {
                    try {
                        getInternalSession().rollback();
                    } catch (JMSException e) {
                        invalidate = true;
                        LOG.warn("Caught exception trying rollback() when putting session back into the pool, will invalidate. " + e, e);
                    }
                }
            } catch (JMSException ex) {
                invalidate = true;
                LOG.warn("Caught exception trying close() when putting session back into the pool, will invalidate. " + ex, ex);
            } finally {
                consumers.clear();
                browsers.clear();
//IC see: https://issues.apache.org/jira/browse/ARIES-1158
                for (PooledSessionEventListener listener : this.sessionEventListeners) {
                    listener.onSessionClosed(this);
                }
                sessionEventListeners.clear();
            }

            if (invalidate) {
                // lets close the session and not put the session back into the pool
                // instead invalidate it so the pool can create a new one on demand.
                if (session != null) {
                    try {
                        session.close();
                    } catch (JMSException e1) {
                        LOG.trace("Ignoring exception on close as discarding session: " + e1, e1);
                    }
                    session = null;
                }
//IC see: https://issues.apache.org/jira/browse/ARIES-1158
                try {
                    sessionPool.invalidateObject(key, this);
                } catch (Exception e) {
                    LOG.trace("Ignoring exception on invalidateObject as discarding session: " + e, e);
                }
            } else {
                try {
                    sessionPool.returnObject(key, this);
                } catch (Exception e) {
                    javax.jms.IllegalStateException illegalStateException = new javax.jms.IllegalStateException(e.toString());
                    illegalStateException.initCause(e);
                    throw illegalStateException;
                }
            }
        }
    }

    @Override
    public void commit() throws JMSException {
        getInternalSession().commit();
    }

    @Override
    public BytesMessage createBytesMessage() throws JMSException {
        return getInternalSession().createBytesMessage();
    }

    @Override
    public MapMessage createMapMessage() throws JMSException {
        return getInternalSession().createMapMessage();
    }

    @Override
    public Message createMessage() throws JMSException {
        return getInternalSession().createMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() throws JMSException {
        return getInternalSession().createObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException {
        return getInternalSession().createObjectMessage(serializable);
    }

    @Override
    public Queue createQueue(String s) throws JMSException {
        return getInternalSession().createQueue(s);
    }

    @Override
    public StreamMessage createStreamMessage() throws JMSException {
        return getInternalSession().createStreamMessage();
    }

    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        TemporaryQueue result;

        result = getInternalSession().createTemporaryQueue();

        // Notify all of the listeners of the created temporary Queue.
        for (PooledSessionEventListener listener : this.sessionEventListeners) {
            listener.onTemporaryQueueCreate(result);
        }

        return result;
    }

    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        TemporaryTopic result;

        result = getInternalSession().createTemporaryTopic();

        // Notify all of the listeners of the created temporary Topic.
        for (PooledSessionEventListener listener : this.sessionEventListeners) {
            listener.onTemporaryTopicCreate(result);
        }

        return result;
    }

    @Override
    public void unsubscribe(String s) throws JMSException {
        getInternalSession().unsubscribe(s);
    }

    @Override
    public TextMessage createTextMessage() throws JMSException {
        return getInternalSession().createTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String s) throws JMSException {
        return getInternalSession().createTextMessage(s);
    }

    @Override
    public Topic createTopic(String s) throws JMSException {
        return getInternalSession().createTopic(s);
    }

    @Override
    public int getAcknowledgeMode() throws JMSException {
        return getInternalSession().getAcknowledgeMode();
    }

    @Override
    public boolean getTransacted() throws JMSException {
        return getInternalSession().getTransacted();
    }

    @Override
    public void recover() throws JMSException {
        getInternalSession().recover();
    }

    @Override
    public void rollback() throws JMSException {
        getInternalSession().rollback();
    }

    @Override
    public XAResource getXAResource() {
        if (session instanceof XASession) {
            return ((XASession) session).getXAResource();
        }
        return null;
    }

    @Override
    public Session getSession() {
        return this;
    }

    @Override
    public void run() {
        if (session != null) {
            session.run();
        }
    }

    // Consumer related methods
    // -------------------------------------------------------------------------
    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        return addQueueBrowser(getInternalSession().createBrowser(queue));
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String selector) throws JMSException {
        return addQueueBrowser(getInternalSession().createBrowser(queue, selector));
    }

    @Override
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        return addConsumer(getInternalSession().createConsumer(destination));
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String selector) throws JMSException {
        return addConsumer(getInternalSession().createConsumer(destination, selector));
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String selector, boolean noLocal) throws JMSException {
        return addConsumer(getInternalSession().createConsumer(destination, selector, noLocal));
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String selector) throws JMSException {
        return addTopicSubscriber(getInternalSession().createDurableSubscriber(topic, selector));
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String selector, boolean noLocal) throws JMSException {
        return addTopicSubscriber(getInternalSession().createDurableSubscriber(topic, name, selector, noLocal));
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        return getInternalSession().getMessageListener();
    }

    @Override
    public void setMessageListener(MessageListener messageListener) throws JMSException {
        getInternalSession().setMessageListener(messageListener);
    }

    @Override
    public TopicSubscriber createSubscriber(Topic topic) throws JMSException {
        return addTopicSubscriber(((TopicSession) getInternalSession()).createSubscriber(topic));
    }

    @Override
    public TopicSubscriber createSubscriber(Topic topic, String selector, boolean local) throws JMSException {
        return addTopicSubscriber(((TopicSession) getInternalSession()).createSubscriber(topic, selector, local));
    }

    @Override
    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        return addQueueReceiver(((QueueSession) getInternalSession()).createReceiver(queue));
    }

    @Override
    public QueueReceiver createReceiver(Queue queue, String selector) throws JMSException {
        return addQueueReceiver(((QueueSession) getInternalSession()).createReceiver(queue, selector));
    }

    // Producer related methods
    // -------------------------------------------------------------------------
    @Override
    public MessageProducer createProducer(Destination destination) throws JMSException {
//IC see: https://issues.apache.org/jira/browse/ARIES-1158
        return new PooledProducer(getMessageProducer(destination), destination);
    }

    @Override
    public QueueSender createSender(Queue queue) throws JMSException {
        return new PooledQueueSender(getQueueSender(queue), queue);
    }

    @Override
    public TopicPublisher createPublisher(Topic topic) throws JMSException {
        return new PooledTopicPublisher(getTopicPublisher(topic), topic);
    }

    public Session getInternalSession() throws IllegalStateException {
        if (session == null) {
            throw new IllegalStateException("The session has already been closed");
        }
        return session;
    }

    public MessageProducer getMessageProducer() throws JMSException {
        return getMessageProducer(null);
    }

    public MessageProducer getMessageProducer(Destination destination) throws JMSException {
        MessageProducer result = null;

        if (useAnonymousProducers) {
            if (producer == null) {
                // Don't allow for duplicate anonymous producers.
                synchronized (this) {
                    if (producer == null) {
                        producer = getInternalSession().createProducer(null);
                    }
                }
            }

            result = producer;
        } else {
            result = getInternalSession().createProducer(destination);
        }

        return result;
    }

    public QueueSender getQueueSender() throws JMSException {
        return getQueueSender(null);
    }

    public QueueSender getQueueSender(Queue destination) throws JMSException {
        QueueSender result = null;

        if (useAnonymousProducers) {
            if (sender == null) {
                // Don't allow for duplicate anonymous producers.
                synchronized (this) {
                    if (sender == null) {
                        sender = ((QueueSession) getInternalSession()).createSender(null);
                    }
                }
            }

            result = sender;
        } else {
            result = ((QueueSession) getInternalSession()).createSender(destination);
        }

        return result;
    }

    public TopicPublisher getTopicPublisher() throws JMSException {
        return getTopicPublisher(null);
    }

    public TopicPublisher getTopicPublisher(Topic destination) throws JMSException {
        TopicPublisher result = null;

        if (useAnonymousProducers) {
            if (publisher == null) {
                // Don't allow for duplicate anonymous producers.
                synchronized (this) {
                    if (publisher == null) {
                        publisher = ((TopicSession) getInternalSession()).createPublisher(null);
                    }
                }
            }

            result = publisher;
        } else {
            result = ((TopicSession) getInternalSession()).createPublisher(destination);
        }

        return result;
    }

    private QueueBrowser addQueueBrowser(QueueBrowser browser) {
        browsers.add(browser);
        return browser;
    }

    private MessageConsumer addConsumer(MessageConsumer consumer) {
        consumers.add(consumer);
        // must wrap in PooledMessageConsumer to ensure the onConsumerClose
        // method is invoked when the returned consumer is closed, to avoid memory
        // leak in this session class in case many consumers is created
        return new PooledMessageConsumer(this, consumer);
    }

    private TopicSubscriber addTopicSubscriber(TopicSubscriber subscriber) {
        consumers.add(subscriber);
        return subscriber;
    }

    private QueueReceiver addQueueReceiver(QueueReceiver receiver) {
        consumers.add(receiver);
        return receiver;
    }

    public void setIsXa(boolean isXa) {
        this.isXa = isXa;
    }

    @Override
    public String toString() {
        return "PooledSession { " + session + " }";
    }

    /**
     * Callback invoked when the consumer is closed.
     * <p/>
     * This is used to keep track of an explicit closed consumer created by this
     * session, by which we know do not need to keep track of the consumer, as
     * its already closed.
     *
     * @param consumer
     *            the consumer which is being closed
     */
    protected void onConsumerClose(MessageConsumer consumer) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1158
        consumers.remove(consumer);
    }
}
