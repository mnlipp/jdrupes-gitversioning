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
import static org.jdrupes.builder.api.Intend.*;
import static org.jdrupes.builder.api.Project.Properties.Version;
import static org.jdrupes.builder.mvnrepo.MvnProperties.ArtifactId;
import static org.jdrupes.builder.mvnrepo.MvnProperties.GroupId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.eclipse.jgit.api.Git;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import org.jdrupes.builder.java.JavaSourceFile;
import org.jdrupes.builder.java.LibraryGenerator;
import org.jdrupes.builder.mvnrepo.MvnPublisher;
import org.jdrupes.builder.mvnrepo.PomFile;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarGenerator;
import org.jdrupes.gitversioning.api.VersionEvaluator;
import org.jdrupes.gitversioning.core.DefaultTagFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/// The Class ProjectPreparation.
///
public class ProjectPreparation {

    public static void setupVersion(Project project) {
        try {
            if (project instanceof RootProject) {
                project.set(GitApi, Git.open(project.directory().toFile()));
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }

        var evaluator = VersionEvaluator
            .forRepository(project.<Git> get(GitApi).getRepository())
            .subDirectory(project.directory())
            .tagFilter(new DefaultTagFilter().prepend("v"));
        project.set(Version, evaluator.version());
    }

    public static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            project.generator(JavaCompiler::new)
                .addSources(Path.of("src"), "**/*.java")
                .options("--release", "21");
            project.generator(JavaResourceCollector::new)
                .add(Path.of("resources"), "**/*");

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
            project.dependency(Supply, new LibraryGenerator(project)
                .from(project.providers(Supply))
                .addEntries(project.from(Supply).get(
                    new ResourceRequest<PomFile>(new ResourceType<>() {}))
                    .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                        .resolve((String) project.get(GroupId))
                        .resolve((String) project.get(ArtifactId))
                        .resolve("pom.xml"), pomFile))));

            // Supply sources jar
            project.generator(SourcesJarGenerator::new)
                .addTrees(project.get(
                    new ResourceRequest<FileTree<JavaSourceFile>>(
                        new ResourceType<>() {})));

            // Publish (deploy). Credentials and signing information is
            // obtained through properties.
            project.generator(MvnPublisher::new);
        }
    }

    public static void setupEclipseConfigurator(Project project) {
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
                    throw new BuildException(e);
                }
            }));
    }

}
