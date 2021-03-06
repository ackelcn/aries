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
package org.apache.aries.blueprint.itests;

import static org.apache.aries.blueprint.itests.Helper.mvnBundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;

/**
 * this test is based on blueprint container test, but this test starts the
 * blueprint sample before the blueprint bundle is started so going a slightly 
 * different code path
 *
 */
@RunWith(PaxExam.class)
public class BlueprintContainer2Test extends AbstractBlueprintIntegrationTest {

    @Test
    public void test() throws Exception {
        applyCommonConfiguration(context());
//IC see: https://issues.apache.org/jira/browse/ARIES-1218

        Bundle bundle = getSampleBundle();
        bundle.start();
        startBlueprintBundles();
        
        // do the test
        Helper.testBlueprintContainer(context(), bundle);
    }
    
    @org.ops4j.pax.exam.Configuration
    public Option[] configuration() {
//IC see: https://issues.apache.org/jira/browse/ARIES-1184
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(false),
            mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample", false)
        };
    }

}
