/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.archive;

import org.osgi.framework.Constants;

public class BundleManifest extends Manifest {
	public BundleManifest(java.util.jar.Manifest manifest) {
//IC see: https://issues.apache.org/jira/browse/ARIES-737
		super(manifest);
//IC see: https://issues.apache.org/jira/browse/ARIES-825
		fillInDefaults();
	}
	
	private void fillInDefaults() {
		Header<?> header = headers.get(Constants.BUNDLE_VERSION);
		if (header == null)
			headers.put(Constants.BUNDLE_VERSION, BundleVersionHeader.DEFAULT);
	}
}
