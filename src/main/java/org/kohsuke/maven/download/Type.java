package org.kohsuke.maven.download;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @author Kohsuke Kawaguchi
 */
public class Type {
    /**
     * Extension of the artifact, aka "artifact type".
     *
     * Defaults to "jar"
     */
    @Parameter
    String type = "jar";

    /**
     * Classifier of the artifact.
     */
    @Parameter
    String classifier;

    @Parameter
    String title;

    public Artifact createArtifact(MavenProject currentProject, ArtifactFactory factory, DownloadSection section, String version) {
        return factory.createArtifactWithClassifier(
                section.groupId==null ? currentProject.getGroupId() : section.groupId,
                section.artifactId==null ? currentProject.getArtifactId() : section.artifactId,
                version, type, classifier);
    }

    public String getTitle() {
        if (title != null) return title;
        if (classifier!=null)   return classifier+':'+type;
        return type;
    }
}
