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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.apache.aries.subsystem.core.internal.Utils;
import org.osgi.framework.Version;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class DeployedContentHeader
//IC see: https://issues.apache.org/jira/browse/ARIES-1387
        extends AbstractClauseBasedHeader<DeployedContentHeader.Clause>
        implements RequirementHeader<DeployedContentHeader.Clause> {

    public static class Clause extends AbstractClause {
        public static final String ATTRIBUTE_DEPLOYEDVERSION = DeployedVersionAttribute.NAME;
        public static final String ATTRIBUTE_RESOURCEID = "resourceId";
        public static final String ATTRIBUTE_TYPE = TypeAttribute.NAME;
        public static final String DIRECTIVE_REFERENCE = ReferenceDirective.NAME;
        public static final String DIRECTIVE_STARTORDER = StartOrderDirective.NAME;

        public Clause(String clause) {
            super( 
            		parsePath(clause, Patterns.SYMBOLIC_NAME, false), 
            		parseParameters(clause, false), 
            		generateDefaultParameters(
            				TypeAttribute.DEFAULT,
            				ReferenceDirective.TRUE));
        }

        public Clause(Resource resource) {
//IC see: https://issues.apache.org/jira/browse/ARIES-907
            this(resource, true);
        }

        public Clause(Resource resource, boolean referenced) {
            this(appendResource(resource, new StringBuilder(), referenced)
                    .toString());
        }

        public boolean contains(Resource resource) {
            return getSymbolicName()
                    .equals(ResourceHelper.getSymbolicNameAttribute(resource))
                    && getDeployedVersion().equals(
                            ResourceHelper.getVersionAttribute(resource))
                    && getType()
                            .equals(ResourceHelper.getTypeAttribute(resource));
        }

        public Version getDeployedVersion() {
            return ((DeployedVersionAttribute) getAttribute(
                    ATTRIBUTE_DEPLOYEDVERSION)).getVersion();
        }

        public String getSymbolicName() {
            return path;
        }

        public int getStartOrder() {
            return ((StartOrderDirective) getAttribute(DIRECTIVE_STARTORDER))
                    .getStartOrder();
        }

        public String getType() {
            return ((TypeAttribute) getAttribute(ATTRIBUTE_TYPE)).getType();
        }

        public boolean isReferenced() {
            return ((ReferenceDirective) getDirective(DIRECTIVE_REFERENCE))
                    .isReferenced();
        }

        public DeployedContentRequirement toRequirement(Resource resource) {
//IC see: https://issues.apache.org/jira/browse/ARIES-825
            return new DeployedContentRequirement(this, resource);
        }
    }

    public static final String NAME = SubsystemConstants.DEPLOYED_CONTENT;

    public static DeployedContentHeader newInstance(
            Collection<Resource> resources) {
        StringBuilder builder = new StringBuilder();
        for (Resource resource : resources) {
//IC see: https://issues.apache.org/jira/browse/ARIES-907
            appendResource(resource, builder, true);
            builder.append(',');
        }
        // Remove the trailing comma.
        // TODO Intentionally letting the exception propagate since there must
        // be at least one resource.
        builder.deleteCharAt(builder.length() - 1);
        return new DeployedContentHeader(builder.toString());
    }

    private static StringBuilder appendResource(Resource resource,
            StringBuilder builder, boolean referenced) {
        String symbolicName = ResourceHelper.getSymbolicNameAttribute(resource);
        Version version = ResourceHelper.getVersionAttribute(resource);
        String type = ResourceHelper.getTypeAttribute(resource);
        builder.append(symbolicName).append(';')
                .append(Clause.ATTRIBUTE_DEPLOYEDVERSION).append('=')
                .append(version.toString()).append(';')
                .append(Clause.ATTRIBUTE_TYPE).append('=').append(type)
                .append(';').append(Clause.ATTRIBUTE_RESOURCEID).append('=')
                .append(Utils.getId(resource)).append(';')
                .append(Clause.DIRECTIVE_REFERENCE).append(":=")
                .append(referenced);
        return builder;
    }

    public DeployedContentHeader(Collection<Clause> clauses) {
        super(clauses);
    }

    public DeployedContentHeader(String value) {
    	super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
    }

    public boolean contains(Resource resource) {
        for (Clause clause : getClauses())
            if (clause.contains(resource))
                return true;
        return false;
    }

    public Clause getClause(Resource resource) {
        String symbolicName = ResourceHelper.getSymbolicNameAttribute(resource);
        Version version = ResourceHelper.getVersionAttribute(resource);
        String type = ResourceHelper.getTypeAttribute(resource);
        for (Clause clause : clauses) {
            if (symbolicName.equals(clause.getPath())
                    && clause.getDeployedVersion().equals(version)
                    && type.equals(clause.getType()))
                return clause;
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getValue() {
        return toString();
    }

    public boolean isReferenced(Resource resource) {
//IC see: https://issues.apache.org/jira/browse/ARIES-907
        DeployedContentHeader.Clause clause = getClause(resource);
        if (clause == null)
            return false;
        return clause.isReferenced();
    }

    @Override
    public List<Requirement> toRequirements(Resource resource) {
        List<Requirement> requirements = new ArrayList<Requirement>(
                clauses.size());
        for (Clause clause : clauses)
            requirements.add(clause.toRequirement(resource));
        return requirements;
    }
}
