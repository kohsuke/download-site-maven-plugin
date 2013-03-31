package org.kohsuke.maven.download;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures what to be listed in the downloads section.
 *
 * @author Kohsuke Kawaguchi
 */
public class DownloadSection {
    /**
     * Group ID (default to the groupId of the current project)
     */
    @Parameter
    String groupId;

    /**
     * Artifact ID (default to the groupId of the current project)
     */
    @Parameter
    String artifactId;

    /**
     * The artifacts to list.
     */
    @Parameter
    List<Type> types = new ArrayList<Type>();

    /**
     * Fill in the proper default value.
     */
    public void postCreate(MavenProject project, ArtifactHandlerManager artifactHandlerManager) {
        if (types.isEmpty()) {
            Type t = new Type();
            ArtifactHandler h = artifactHandlerManager.getArtifactHandler(project.getPackaging());
            if (h!=null)
                t.type = h.getExtension();
            else
                t.type = h.getPackaging();
            types.add(t);
        }
    }
}
