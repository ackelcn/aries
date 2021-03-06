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
package org.apache.aries.blueprint.reflect;

import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;

/**
 * A metadata for environment managers.
 *
 * @version $Rev$, $Date$
 */
public class PassThroughMetadataImpl extends ComponentMetadataImpl implements MutablePassThroughMetadata {

    private Object object;

    public PassThroughMetadataImpl() {
    }

    public PassThroughMetadataImpl(PassThroughMetadata source) {
//IC see: https://issues.apache.org/jira/browse/ARIES-4
        super(source);
        this.object = source.getObject();
    }

    public PassThroughMetadataImpl(String id, Object object) {
        this.id = id;
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
//IC see: https://issues.apache.org/jira/browse/ARIES-4
        this.object = object;
    }
}
