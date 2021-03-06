package org.solmix.karaf.plugin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.graph.Dependency;

public class MvnToAetherMapper
{
    private MvnToAetherMapper() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts a Maven model dependency to an Aether dependency.
     *
     * @param dependency The Maven model dependency.
     * @return The Aether dependency.
     */
    public static Dependency toAether(org.apache.maven.model.Dependency dependency) {
        DefaultArtifact artifact = new DefaultArtifact(dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getClassifier(),
                null,
                dependency.getVersion(),
                new DefaultArtifactType(dependency.getType()));
        return new Dependency(artifact, null);
    }

    /**
     * Converts a list of Maven model dependencies to a list of Aether dependencies.
     *
     * @param dependencies The Maven model dependencies.
     * @return The Aether dependencies.
     */
    public static List<Dependency> toAether(List<org.apache.maven.model.Dependency> dependencies) {
        List<Dependency> result = new ArrayList<>();
        for (org.apache.maven.model.Dependency dependency : dependencies) {
            result.add(toAether(dependency));
        }
        return result;
    }
}
