/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.async.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.async.Async;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class AsyncServiceFactory implements ServiceFactory<Async> {

	private final ExecutorService executor;
	
	private final ScheduledExecutorService ses;
	
	private final ServiceTracker<LogService, LogService> logServiceTracker;
	
	public AsyncServiceFactory(ExecutorService executor, ScheduledExecutorService ses, 
			ServiceTracker<LogService, LogService> logServiceTracker) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1318
		this.logServiceTracker = logServiceTracker;
		this.executor = executor;
		this.ses = ses;
	}

	public Async getService(Bundle bundle,
			ServiceRegistration<Async> registration) {
		
		return new AsyncService(bundle, executor, ses, logServiceTracker);
	}

	public void ungetService(Bundle bundle,
//IC see: https://issues.apache.org/jira/browse/ARIES-1603
			ServiceRegistration<Async> registration, Async service) {
		((AsyncService) service).clear();
	}

}
