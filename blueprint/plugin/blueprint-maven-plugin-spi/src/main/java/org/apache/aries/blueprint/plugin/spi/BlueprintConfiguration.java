/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.spi;

import java.util.Map;
import java.util.Set;

/**
 * Blueprint plugin configuration from pom.xml
 */
public interface BlueprintConfiguration {
    /**
     * @return values of namespaces parameter
     */
    Set<String> getNamespaces();
//IC see: https://issues.apache.org/jira/browse/ARIES-1602

    /**
     * @return value of default activation parameter
     */
    Activation getDefaultActivation();

    /**
     * @return value of default availability parameter
     */
    Availability getDefaultAvailability();
//IC see: https://issues.apache.org/jira/browse/ARIES-1736

    /**
     * @return value of default timeout parameter
     */
    Long getDefaultTimeout();

    /**
     * @return custom parameters
     */
//IC see: https://issues.apache.org/jira/browse/ARIES-1605
    Map<String, String> getCustomParameters();
}
