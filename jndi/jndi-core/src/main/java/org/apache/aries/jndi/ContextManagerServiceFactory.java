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
package org.apache.aries.jndi;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class ContextManagerServiceFactory implements ServiceFactory<ContextManagerService> {

    public ContextManagerService getService(Bundle bundle, ServiceRegistration<ContextManagerService> registration) {
//IC see: https://issues.apache.org/jira/browse/ARIES-417
        return new ContextManagerService(bundle.getBundleContext());
    }

    public void ungetService(Bundle bundle, ServiceRegistration<ContextManagerService> registration, ContextManagerService service) {
        service.close();
    }

}
