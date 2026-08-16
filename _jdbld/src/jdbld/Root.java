/*
 * JDrupes GitVersioning
 * Copyright (C) 2025 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jdbld;

import static jdbld.ExtProps.GitApi;
import static org.jdrupes.builder.api.Intent.*;
import static org.jdrupes.builder.api.ResourceType.ProjectVersionType;
import static org.jdrupes.builder.api.ResourceType.TestResultType;

import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.core.VersionReporter;

import static org.jdrupes.builder.api.CoreProperties.*;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.mvnrepo.JavadocJarBuilder;
import org.jdrupes.builder.mvnrepo.MvnDeployDestination;
import org.jdrupes.builder.mvnrepo.MvnPublisher;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.MvnVersionType;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarBuilder;
import org.jdrupes.gitversioning.api.VersionEvaluator;
import org.jdrupes.gitversioning.core.DefaultTagFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import static org.jdrupes.builder.mvnrepo.MvnProperties.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.eclipse.jgit.api.Git;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import org.jdrupes.builder.java.Javadoc;
import org.jdrupes.builder.java.LibraryBuilder;
import org.jdrupes.builder.junit.JUnitTestRunner;

public class Root extends AbstractRootProject {

    @Override
    public void prepareProject(Project project) throws Exception {
        project.set(ArtifactId, project.name());
        setupVersion(project);
        setupCommonGenerators(project);
        setupEclipseConfigurator(project);
    }

    public Root() {
        super(name("JDrupes-GitVersioning"));
        set(GroupId, "org.jdrupes.gitversioning");

        dependency(Expose, project(Api.class));
        dependency(Expose, project(Core.class));

        // Supply overall javadoc
        generator(Javadoc::new).projects(Stream.of(this, project(Api.class),
            project(Core.class)))
            .destination(rootProject().directory().resolve("webpages/javadoc"))
            .tagletpath(new MvnRepoLookup()
                .resolve("org.jdrupes.taglets:plantuml-taglet:3.1.0",
                    "net.sourceforge.plantuml:plantuml:1.2023.11")
                .resources(of(ClasspathElementType).using(Supply, Expose)))
            .taglets(Stream.of("org.jdrupes.taglets.plantUml.PlantUml",
                "org.jdrupes.taglets.plantUml.StartUml",
                "org.jdrupes.taglets.plantUml.EndUml"))
            .options("-overview",
                directory().resolve("overview.html").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/javadoc-overwrites.css").toString())
            .options("--add-script",
                directory().resolve("misc/highlight.min.js").toString())
            .options("--add-script",
                directory().resolve("misc/highlight-all.js").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/highlight-default.css").toString())
            .options("-bottom",
                readString(directory().resolve("misc/javadoc.bottom.txt")))
            .options("--allow-script-in-comments")
            .options("-linksource")
            .options("-link",
                "https://docs.oracle.com/en/java/javase/25/docs/api/")
            .options("-quiet");

        // Commands
        commandAlias("version").projects("**")
            .resources(of(ProjectVersionType).using(Supply));
        commandAlias("build").projects("**")
            .resources(of(LibraryJarFileType).using(Supply));
        commandAlias("test").description("Run all tests").projects("**")
            .resources(of(TestResultType).using(Supply));
        commandAlias("javadoc").resources(of(JavadocDirectoryType));
        commandAlias("eclipse").resources(of(
            new ResourceType<EclipseConfiguration>() {}));
        commandAlias("pomFile").resources(of(PomFileType));
        commandAlias("mavenPublication").resources(of(MvnPublicationType));
    }

    private static void setupVersion(Project project) {
        try {
            if (project instanceof RootProject) {
                project.set(GitApi, Git.open(project.directory().toFile()));
                return;
            }
        } catch (IOException e) {
            throw new BuildException().cause(e);
        }

        var evaluator = VersionEvaluator
            .forRepository(project.<Git> get(GitApi).getRepository())
            .subDirectory(project.directory())
            .tagFilter(new DefaultTagFilter().prepend(project.name() + "-"));
        project.set(Version, evaluator.version());
        project.generator(VersionReporter::new);
    }

    private static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            if (!(project instanceof MergedTestProject)) {
                project.generator(JavaCompiler::new)
                    .addSources(Path.of("src"), "**/*.java")
                    .options("--release", "21");
                project.generator(JavaResourceCollector::new)
                    .add(Path.of("resources"), "**/*");
                setupArtifactGeneration(project);
            } else {
                project.generator(JavaCompiler::new).addSources(Path.of("test"),
                    "**/*.java").options("--release", "25");
                project.generator(JavaResourceCollector::new).add(Path.of(
                    "test-resources"), "**/*");
                project.dependency(Consume, new MvnRepoLookup()
                    .resolve("junit:junit:4.13.2")
                    .bom("org.junit:junit-bom:5.14.2")
                    .resolve("org.junit.jupiter:junit-jupiter-api")
                    .resolve("org.junit.jupiter:junit-jupiter-params")
                    .resolve("org.junit.jupiter:junit-jupiter-engine",
                        "org.junit.vintage:junit-vintage-engine",
                        "net.jodah:concurrentunit:0.4.2"));
                project.dependency(Supply, JUnitTestRunner::new);
            }
        }
    }

    private static void setupArtifactGeneration(Project project) {
        // Provide POM
        project.dependency(Supply, PomFileGenerator::new)
            .adaptPom(model -> {
                model.setDescription("See URL.");
                model.setUrl("https://jdrupes.org/");
                var scm = new Scm();
                scm.setUrl(
                    "https://github.com/jdrupes/jdrupes-gitversioning");
                scm.setConnection(
                    "scm:git://github.com/jdrupes/jdrupes-gitversioning.git");
                scm.setDeveloperConnection(
                    "scm:git://github.com/jdrupes/jdrupes-gitversioning.git");
                model.setScm(scm);
                var license = new License();
                license.setName("AGPL 3.0");
                license.setUrl(
                    "https://www.gnu.org/licenses/agpl-3.0.en.html");
                license.setDistribution("repo");
                model.setLicenses(List.of(license));
                var developer = new Developer();
                developer.setId("mnlipp");
                developer.setName("Michael N. Lipp");
                model.setDevelopers(List.of(developer));
            });

        // Provide library jar
        project.dependency(Supply, new LibraryBuilder(project)
            .addFrom(project.providers().select(Supply))
            .addEntries(project.resources(
                project.of(PomFileType).using(Supply))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                    .resolve((String) project.get(GroupId))
                    .resolve(project.name())
                    .resolve("pom.xml"), pomFile))));

        // Supply sources jar
        project.generator(SourcesJarBuilder::new).addTrees(
            project.resources(project.of(
                JavaSourceTreeType).using(Supply, Expose)));

        // Supply javadoc
        project.generator(Javadoc::new)
            .options("-overview", project.rootProject().directory()
                .resolve("overview.html").toString())
            .options("--add-stylesheet",
                project.rootProject().directory()
                    .resolve("misc/javadoc-overwrites.css").toString())
            .options("--add-script", project.rootProject().directory()
                .resolve("misc/highlight.min.js").toString())
            .options("--add-script", project.rootProject().directory()
                .resolve("misc/highlight-all.js").toString())
            .options("--add-stylesheet",
                project.rootProject().directory()
                    .resolve("misc/highlight-default.css").toString())
            .options("-bottom", project.rootProject().readString(
                Path.of("misc/javadoc.bottom.txt")))
            .options("--allow-script-in-comments")
            .options("-linksource")
            .options("-link",
                "https://docs.oracle.com/en/java/javase/21/docs/api/")
            .options("-quiet");

        // Supply javadoc jar
        project.generator(JavadocJarBuilder::new);

        // Publish (deploy). Credentials and signing information is
        // obtained through properties and/or settings.xml.
        project.generator(MvnPublisher::new).destinations(
            new MvnDeployDestination(MvnVersionType.SNAPSHOT,
                MvnVersionType.RELEASE).repositoryUri(
                    URI.create(
                        "https://codeberg.org/api/packages/JDrupes/maven"))
                    .id("codeberg"));
    }

    private static void setupEclipseConfigurator(Project project) {
        project.generator(new EclipseConfigurator(project)
            .eclipseAlias(project instanceof RootProject ? project.name()
                : project.get(GroupId) + "." + project.name())
            .adaptProjectConfiguration((Document doc,
                    Node buildSpec, Node natures) -> {
                if (project instanceof JavaProject) {
                    var cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleNature"));
                    cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDNature"));
                }
            }).adaptConfiguration(() -> {
                if (!(project instanceof JavaProject)) {
                    return;
                }
                try {
                    Files.copy(
                        Root.class.getResourceAsStream("net.sf.jautodoc.prefs"),
                        project.directory()
                            .resolve(".settings/net.sf.jautodoc.prefs"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("checkstyle"),
                        project.directory().resolve(".checkstyle"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("eclipse-pmd"),
                        project.directory().resolve(".eclipse-pmd"),
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BuildException().cause(e);
                }
            }));
    }
}
