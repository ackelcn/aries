/**
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
package org.apache.aries.blueprint.ext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.aries.blueprint.ExtendedValueMetadata;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanArgument;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanProperty;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableMapEntry;
import org.apache.aries.blueprint.mutable.MutableMapMetadata;
import org.apache.aries.blueprint.mutable.MutablePropsMetadata;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for property placeholders.
 *
 * @version $Rev: 1211548 $, $Date: 2011-12-07 17:26:22 +0000 (Wed, 07 Dec 2011) $
 */
public abstract class AbstractPropertyPlaceholder implements ComponentDefinitionRegistryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPropertyPlaceholder.class);

    private ExtendedBlueprintContainer blueprintContainer;
    private String placeholderPrefix = "${";
    private String placeholderSuffix = "}";
    private String nullValue = null;
    private Pattern pattern;

    private LinkedList<String> processingStack = new LinkedList<String>();

    public String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    public String getPlaceholderSuffix() {
        return placeholderSuffix;
    }

    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    public String getNullValue() {
//IC see: https://issues.apache.org/jira/browse/ARIES-1298
        return nullValue;
    }

    public void setNullValue(String nullValue) {
        this.nullValue = nullValue;
    }

    public ExtendedBlueprintContainer getBlueprintContainer() {
//IC see: https://issues.apache.org/jira/browse/ARIES-998
        return blueprintContainer;
    }

    public void setBlueprintContainer(ExtendedBlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public void process(ComponentDefinitionRegistry registry) throws ComponentDefinitionException {
        try {
            for (String name : registry.getComponentDefinitionNames()) {
                processMetadata(registry.getComponentDefinition(name));
            }
        } finally {
            processingStack.clear();
        }
    }

    protected Metadata processMetadata(Metadata metadata) {
        try {
            if (metadata instanceof BeanMetadata) {
                BeanMetadata bmd = (BeanMetadata) metadata;
                processingStack.add("Bean named " + bmd.getId() + "->");
                return processBeanMetadata(bmd);
            } else if (metadata instanceof CollectionMetadata) {
                CollectionMetadata cmd = (CollectionMetadata) metadata;
                processingStack.add("Collection of type " + cmd.getCollectionClass() + "->");
                return processCollectionMetadata(cmd);
            } else if (metadata instanceof MapMetadata) {
                processingStack.add("Map->");
                return processMapMetadata((MapMetadata) metadata);
            } else if (metadata instanceof PropsMetadata) {
                processingStack.add("Properties->");
                return processPropsMetadata((PropsMetadata) metadata);
            } else if (metadata instanceof ValueMetadata) {
                processingStack.add("Value->");
                return processValueMetadata((ValueMetadata) metadata);
            } else {
                processingStack.add("Unknown Metadata " + metadata + "->");
                return metadata;
            }
        } finally {
            processingStack.removeLast();
        }
    }

    protected Metadata processBeanMetadata(BeanMetadata component) {
        for (BeanArgument arg :  component.getArguments()) {

            try {
                processingStack.add(
                        "Argument index " + arg.getIndex() + " and value type " + arg.getValueType() + "->");
                if(arg instanceof MutableBeanArgument) {
                    ((MutableBeanArgument) arg).setValue(processMetadata(arg.getValue()));
                } else {
                    //Say that we can't change this argument, but continue processing
                    //If the value is mutable then we may be ok!
                    printWarning(arg, "Constructor Argument");
                    processMetadata(arg.getValue());
                }
            } finally {
                processingStack.removeLast();
            }
        }
        for (BeanProperty prop : component.getProperties()) {

            try {
                processingStack.add("Property named " + prop.getName() + "->");
                if(prop instanceof MutableBeanProperty) {
                    ((MutableBeanProperty) prop).setValue(processMetadata(prop.getValue()));
                } else {
                    //Say that we can't change this property, but continue processing
                    //If the value is mutable then we may be ok!
                    printWarning(prop, "Injection Property");
                    processMetadata(prop.getValue());
                }
            } finally {
                processingStack.removeLast();
            }
        }

        Target factoryComponent = component.getFactoryComponent();
        if(factoryComponent != null) {

            try {

                if(component instanceof MutableBeanMetadata) {
                    processingStack.add("Factory Component->");
                    ((MutableBeanMetadata) component).setFactoryComponent(
                            (Target) processMetadata(factoryComponent));
                } else {
                    printWarning(component, "Factory Component");
                    processingStack.add("Factory Component->");
                    processMetadata(factoryComponent);
                }
            } finally {
                processingStack.removeLast();
            }
        }

        return component;
    }

    protected Metadata processPropsMetadata(PropsMetadata metadata) {

        List<MapEntry> entries = new ArrayList<MapEntry>(metadata.getEntries());
        if(!!! entries.isEmpty()) {

            try {
                if(metadata instanceof MutablePropsMetadata) {
                    processingStack.add("Properties->");
                    MutablePropsMetadata mpm = (MutablePropsMetadata) metadata;

                    for (MapEntry entry : entries) {
                        mpm.removeEntry(entry);
                    }
                    for (MapEntry entry : processMapEntries(entries)) {
                        mpm.addEntry(entry);
                    }
                } else {
                    printWarning(metadata, "Properties");
                    processingStack.add("Properties->");
                    processMapEntries(entries);
                }
            } finally {
                processingStack.removeLast();
            }
        }
        return metadata;
    }

    protected Metadata processMapMetadata(MapMetadata metadata) {
        List<MapEntry> entries = new ArrayList<MapEntry>(metadata.getEntries());
        if(!!! entries.isEmpty()) {

            try {
                if(metadata instanceof MutableMapMetadata) {
                    processingStack.add("Map->");
                    MutableMapMetadata mmm = (MutableMapMetadata) metadata;

                    for (MapEntry entry : entries) {
                        mmm.removeEntry(entry);
                    }
                    for (MapEntry entry : processMapEntries(entries)) {
                        mmm.addEntry(entry);
                    }
                } else {
                    printWarning(metadata, "Map");
                    processingStack.add("Map->");
                    processMapEntries(entries);
                }
            } finally {
                processingStack.removeLast();
            }
        }
        return metadata;
    }

    protected List<MapEntry> processMapEntries(List<MapEntry> entries) {
        for (MapEntry entry : entries) {
            try {
                processingStack.add("Map Entry Key: " + entry.getKey() + " Value: " + entry.getValue() + "->" );

                if(entry instanceof MutableMapEntry) {
                    ((MutableMapEntry) entry).setKey((NonNullMetadata) processMetadata(entry.getKey()));
                    ((MutableMapEntry) entry).setValue(processMetadata(entry.getValue()));
                } else {
                    printWarning(entry, "Map Entry");
                    processMetadata(entry.getKey());
                    processMetadata(entry.getValue());
                }
            } finally {
                processingStack.removeLast();
            }
        }
        return entries;
    }

    protected Metadata processCollectionMetadata(CollectionMetadata metadata) {

        List<Metadata> values = new ArrayList<Metadata>(metadata.getValues());
        if(!!! values.isEmpty()) {

            try {
                if(metadata instanceof MutableCollectionMetadata) {
                    processingStack.add("Collection type: " + metadata.getValueType() + "->");
                    MutableCollectionMetadata mcm = (MutableCollectionMetadata) metadata;

                    for (Metadata value : values) {
                        mcm.removeValue(value);
                    }
                    for (Metadata value : values) {
                        mcm.addValue(processMetadata(value));
                    }
                } else {
                    printWarning(metadata, "Collection type: " + metadata.getValueType());
                    processingStack.add("Collection type: " + metadata.getValueType() + "->");
                    for (Metadata value : values) {
                        processMetadata(value);
                    }
                }
            } finally {
                processingStack.removeLast();
            }
        }
        return metadata;
    }

    protected Metadata processValueMetadata(ValueMetadata metadata) {

        return new LateBindingValueMetadata(metadata);
    }

    private void printWarning(Object immutable, String processingType) {
        StringBuilder sb = new StringBuilder("The property placeholder processor for ");
        sb.append(placeholderPrefix).append(',').append(" ").append(placeholderSuffix)
                .append(" found an immutable ").append(processingType)
                .append(" at location ");

        for(String s : processingStack) {
            sb.append(s);
        }

        sb.append(". This may prevent properties, beans, or other items referenced by this component from being properly processed.");

        LOGGER.info(sb.toString());
    }

    protected Object retrieveValue(String expression) {
        return getProperty(expression);
    }

    protected Object processString(String str) {
        // TODO: we need to handle escapes on the prefix / suffix
        Matcher matcher = getPattern().matcher(str);
        while (matcher.find()) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1601
            String n = matcher.group(1);
            int idx = n.indexOf(placeholderPrefix);
            if (idx >= 0) {
                matcher.region(matcher.start(1) + idx, str.length());
                continue;
            }
//IC see: https://issues.apache.org/jira/browse/ARIES-1298
            Object rep = retrieveValue(matcher.group(1));
            if (rep != null) {
                if (rep instanceof String || !matcher.group(0).equals(str)) {
                    str = str.replace(matcher.group(0), rep.toString());
                    matcher.reset(str);
                } else {
                    return rep;
                }
            }
        }
        if (nullValue != null && nullValue.equals(str)) {
            return null;
        }
        return str;
    }

    protected Object getProperty(String val) {
        return null;
    }

    protected Pattern getPattern() {
        if (pattern == null) {
            pattern = Pattern.compile("\\Q" + placeholderPrefix + "\\E(.+?)\\Q" + placeholderSuffix + "\\E");
        }
        return pattern;
    }

    public class LateBindingValueMetadata implements ExtendedValueMetadata {

        private final ValueMetadata metadata;
        private boolean retrieved;
        private Object retrievedValue;

        public LateBindingValueMetadata(ValueMetadata metadata) {
            this.metadata = metadata;
        }

        public String getStringValue() {
//IC see: https://issues.apache.org/jira/browse/ARIES-1298
            retrieve();
            return retrievedValue instanceof String ? (String) retrievedValue : null;
        }

        public String getType() {
            return metadata.getType();
        }

        public Object getValue() {
            retrieve();
            return retrievedValue instanceof String ? null : retrievedValue;
        }

        private void retrieve() {
            if (!retrieved) {
                Object o = null;
                if (metadata instanceof ExtendedValueMetadata) {
                    o = ((ExtendedValueMetadata) metadata).getValue();
                }
                if (o == null) {
                    String v = metadata.getStringValue();
                    LOGGER.debug("Before process: {}", v);
                    retrievedValue = processString(v);
                    LOGGER.debug("After process: {}", retrievedValue);
                } else {
                    LOGGER.debug("Skipping non string value: {}", o);
                }
                retrieved = true;
            }
        }
    }
}
