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
package org.apache.aries.blueprint.spring;

import java.net.URI;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;

public class SpringNamespaceHandlerResolver implements NamespaceHandlerResolver {

    private final ParserContext parserContext;

    public SpringNamespaceHandlerResolver(ParserContext parserContext) {
        this.parserContext = parserContext;
    }

    @Override
    public org.springframework.beans.factory.xml.NamespaceHandler resolve(String namespaceUri) {
        try {
            NamespaceHandler handler = parserContext.getNamespaceHandler(URI.create(namespaceUri));
            if (handler instanceof BlueprintNamespaceHandler) {
                return ((BlueprintNamespaceHandler) handler).getSpringHandler();
            }
//IC see: https://issues.apache.org/jira/browse/ARIES-1456
            else if (handler != null) {
                return new SpringNamespaceHandler(parserContext, handler);
            }
        } catch (ComponentDefinitionException e) {
            // Ignore
        }
        return null;
    }
}
