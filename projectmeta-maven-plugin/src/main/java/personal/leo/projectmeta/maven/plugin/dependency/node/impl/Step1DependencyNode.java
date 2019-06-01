package personal.leo.projectmeta.maven.plugin.dependency.node.impl;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import personal.leo.projectmeta.maven.plugin.dependency.holder.JarPathHolder;
import personal.leo.projectmeta.maven.plugin.dependency.node.DependencyNode;
import personal.leo.projectmeta.maven.plugin.dependency.visitor.DependencyNodeVisitor;

/**
 * Default implementation of a DependencyNode.
 */
public class Step1DependencyNode implements DependencyNode {

    private final Artifact artifact;

    private final DependencyNode parent;

    private final String versionConstraint;

    private List<DependencyNode> children;

    public Step1DependencyNode(DependencyNode parent, Artifact artifact, String versionConstraint) {
        this.parent = parent;
        this.artifact = artifact;
        this.versionConstraint = versionConstraint;
    }

    @Override
    public boolean accept(DependencyNodeVisitor visitor) {

        if (isJar(artifact)) {
            final File artifactFile = artifact.getFile();

            if (artifactFile == null) {
                JarPathHolder.parseAndAdd(artifact);
            } else {
                try {
                    JarPathHolder.add(artifactFile.getCanonicalPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (visitor.visit(this)) {
            for (DependencyNode child : getChildren()) {
                if (!child.accept(visitor)) {
                    break;
                }
            }
        }

        return visitor.endVisit(this);
    }

    private boolean isJar(Artifact artifact) {
        return "jar".equalsIgnoreCase(artifact.getType());
    }

    /**
     * @return Artifact for this DependencyNode.
     */
    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * @param children List of DependencyNode to set as child nodes.
     */
    public void setChildren(List<DependencyNode> children) {
        this.children = children;
    }

    /**
     * @return List of child nodes for this DependencyNode.
     */
    @Override
    public List<DependencyNode> getChildren() {
        return children;
    }

    /**
     * @return Parent of this DependencyNode.
     */
    @Override
    public DependencyNode getParent() {
        return parent;
    }

    @Override
    public String getPremanagedVersion() {
        return null;
    }

    @Override
    public String getPremanagedScope() {
        return null;
    }

    @Override
    public String getVersionConstraint() {
        return versionConstraint;
    }

    @Override
    public Boolean getOptional() {
        return null;
    }

    @Override
    public String toNodeString() {
        return null;
    }

}
