/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.model;


import org.apache.aries.blueprint.plugin.handlers.Handlers;
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;

import java.lang.reflect.Method;

class BeanFromFactory extends Bean {
    private static final String BLUEPRINT_BEAN_FROM_FACTORY_NAME_PROPERTY = "blueprint.beanFromFactory.nameFromFactoryMethodName";
    private final Method producingMethod;

    BeanFromFactory(Bean factoryBean, Method factoryMethod, ContextEnricher contextEnricher) {
        super(factoryMethod.getReturnType(), contextEnricher);
        String forcedId = AnnotationHelper.findName(factoryMethod.getAnnotations());
        if (forcedId != null) {
            this.id = forcedId;
        }
//IC see: https://issues.apache.org/jira/browse/ARIES-1745
        if (forcedId == null && shouldGetBeanNameFromMethodName(contextEnricher)) {
            this.id = factoryMethod.getName();
        }
        this.producingMethod = factoryMethod;
        setScope(factoryMethod);
        handleCustomBeanAnnotations();
        attributes.put("factory-ref", factoryBean.id);
        attributes.put("factory-method", producingMethod.getName());
    }

    private boolean shouldGetBeanNameFromMethodName(ContextEnricher contextEnricher) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1745
        String value = contextEnricher.getBlueprintConfiguration().getCustomParameters().get(BLUEPRINT_BEAN_FROM_FACTORY_NAME_PROPERTY);
        return Boolean.parseBoolean(value);
    }

    private void setScope(Method factoryMethod) {
        if (AnnotationHelper.findSingletons(factoryMethod.getAnnotations())) {
            attributes.put("scope", "singleton");
        }
    }

    private void handleCustomBeanAnnotations() {
        for (BeanAnnotationHandler beanAnnotationHandler : Handlers.BEAN_ANNOTATION_HANDLERS) {
            Object annotation = AnnotationHelper.findAnnotation(producingMethod.getAnnotations(), beanAnnotationHandler.getAnnotation());
            if (annotation != null) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1602
                beanAnnotationHandler.handleBeanAnnotation(producingMethod, id, contextEnricher, this);
            }
        }
    }

    @Override
    protected void resolveArguments(BlueprintRegistry matcher) {
        resolveArguments(matcher, producingMethod.getParameterTypes(), producingMethod.getParameterAnnotations());
    }

    @Override
//IC see: https://issues.apache.org/jira/browse/ARIES-1681
    BeanRef toBeanRef() {
        return new BeanRef(clazz, id, producingMethod.getAnnotations());
    }
}
