/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jmx;

import javax.management.MBeanServer;

import org.apache.aries.jmx.agent.JMXAgentContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * <p>This class <tt>MBeanServiceTracker</tt> represents {@link ServiceTracker} for {@link MBeanServer}'s
 * registered as services.
 * Tracking all registered MBeanServers in ServiceRegistry.</p>
 * @see ServiceTracker
 * @version $Rev$ $Date$
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MBeanServiceTracker extends ServiceTracker {

    private JMXAgentContext agentContext;

    /**
     * Constructs new MBeanServiceTracker.
     * @param agentContext agent context.
     */
	public MBeanServiceTracker(JMXAgentContext agentContext) {
        super(agentContext.getBundleContext(), MBeanServer.class.getName(), null);
        this.agentContext = agentContext;
    }

    /**
     * <p>Register MBeans using {@link JMXAgentContext#registerMBeans(MBeanServer)} 
     * when MBeanServer service is discovered</p> 
     * @see ServiceTracker#addingService(ServiceReference)
     */
    public Object addingService(final ServiceReference reference) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1169
        final MBeanServer mbeanServer = (MBeanServer) super.addingService(reference);
        Logger logger = agentContext.getLogger();
        logger.log(LogService.LOG_DEBUG, "Discovered MBean server " + mbeanServer);
        agentContext.registerMBeans(mbeanServer);
        return mbeanServer;
    }

    /**
     * <p>Unregister MBeans using {@link JMXAgentContext#unregisterMBeans(MBeanServer)} 
     * when MBeanServer service is removed (unregistered from ServiceRegistry) or
     * tracker is closed</p> 
     * @see ServiceTracker#removedService(ServiceReference, Object)
     */
    public void removedService(final ServiceReference reference, Object service) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1169
        final MBeanServer mbeanServer = (MBeanServer) service;
        Logger logger = agentContext.getLogger();
        logger.log(LogService.LOG_DEBUG, "MBean server " + mbeanServer+ " is unregistered from ServiceRegistry");
        agentContext.unregisterMBeans(mbeanServer);
        super.removedService(reference, service);
    }

    
}
