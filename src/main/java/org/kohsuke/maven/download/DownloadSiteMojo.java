package org.kohsuke.maven.download;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.report.projectinfo.AbstractProjectInfoRenderer;
import org.apache.maven.report.projectinfo.AbstractProjectInfoReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.i18n.I18N;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * @author Kohsuke Kawaguchi
 */
@Mojo(name="download")
public class DownloadSiteMojo extends AbstractProjectInfoReport {

    @Component
    protected ArtifactMetadataSource artifactMetadataSource;
    @Component
    protected RepositoryMetadataManager repositoryMetadataManager;
    @Component
    protected ArtifactFactory artifactFactory;
    @Component
    protected ArtifactHandlerManager artifactHandlerManager;
    @Component
    PlexusContainer container;

    @Parameter
    List<DownloadSection> sections = new ArrayList<DownloadSection>();

    /**
     * URL of the repository to resolve downloadable artifacts from.
     * If missing, the first remote repository configured for the project will be used.
     */
    @Parameter
    String url;


    @Override
    protected String getI18Nsection() {
        return "download";
    }

    @Override
    protected String getI18nString(Locale locale, String key) {
        return getI18N( locale ).getString(getClass().getName(), locale, "report." + getI18Nsection() + '.' + key);
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        if (sections.isEmpty())
            sections.add(new DownloadSection());    // default
        for (DownloadSection section : sections) {
            section.postCreate(project,artifactHandlerManager);
        }
        RendererImpl r = new RendererImpl(getSink(),getI18N(locale),locale);
        r.render();
    }

    public String getOutputName() {
        return "download";
    }

    class RendererImpl extends AbstractProjectInfoRenderer {
        private Locale locale;

        RendererImpl(Sink sink, I18N i18n, Locale locale) {
            super(sink, i18n, locale);
            this.locale = locale;
        }

        @Override
        protected String getI18Nsection() {
            return DownloadSiteMojo.this.getI18Nsection();
        }

        @Override
        protected String getI18nString(String section, String key) {
            return DownloadSiteMojo.this.getI18nString(locale,key);
        }

        @Override
        public void renderBody() {
            try {
                startSection(getTitle());

                Artifact artifact = getProject().getArtifact();
                ArtifactRepository repo;
                if (url!=null)
                    repo = new DefaultArtifactRepository("default", url, new DefaultRepositoryLayout());
                else
                    repo = remoteRepositories.get(0);

                RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
                repositoryMetadataManager.resolve(metadata, Collections.singletonList(repo), localRepository);

                for (DownloadSection section : sections) {
                    startTable();

                    sink.tableRow();
                    tableHeaderCell(getI18nString("column.version"));
                    for (Type t : section.types) {
                        tableHeaderCell(t.getTitle());
                    }
                    sink.tableRow_();

                    List<String> versions = new ArrayList<String>(metadata.getMetadata().getVersioning().getVersions());
                    Collections.reverse(versions);  // newer versions first
                    for (String v : versions) {
                        sink.tableRow();
                        tableCell(v);
                        for (Type t : section.types) {
                            Artifact a = t.createArtifact(project,factory,section,v);
                            String url = getURL(a,repo);
                            sink.tableCell();
                            link(url, url.substring(url.lastIndexOf('/') + 1));
                            sink.tableCell_();
                        }

                        sink.tableRow_();
                    }

                    endTable();
                }

                endSection();
            } catch (RepositoryMetadataResolutionException e) {
                throw new RuntimeException("Failed to generate downloads.html",e);
            }
        }

        @Override
        public String getTitle() {
            return "Downloads";
        }
    }

    private String getURL(Artifact a, ArtifactRepository r) {
        String url = r.getUrl();
        if (!url.endsWith("/")) url+='/';
        return url+r.pathOf(a);
    }
}
