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
package org.apache.aries.transaction.test.impl;

import javax.transaction.Transactional;
import java.sql.SQLException;

public class MandatoryTestBeanImpl extends TestBeanImpl {

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public void insertRow(String name, int value, Exception e) throws SQLException {
        super.insertRow(name, value, e);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public void delegateInsertRow(String name, int value) throws SQLException {
//IC see: https://issues.apache.org/jira/browse/ARIES-628
        super.delegateInsertRow(name, value);
    }

}
