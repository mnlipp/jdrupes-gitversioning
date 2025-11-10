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

import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.mvnrepo.MvnPublication;
import org.jdrupes.builder.mvnrepo.PomFile;
import static org.jdrupes.builder.mvnrepo.MvnProperties.*;
import org.jdrupes.builder.java.JavadocDirectory;
import org.jdrupes.builder.java.LibraryJarFile;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) {
        project.set(ArtifactId, project.name());
        ProjectPreparation.setupVersion(project);
        ProjectPreparation.setupCommonGenerators(project);
        ProjectPreparation.setupEclipseConfigurator(project);
    }

    public Root() {
        super(name("JDrupes-GitVersioning"));
        set(GroupId, "org.jdrupes.gitversioning");

        dependency(Expose, project(Api.class));
        dependency(Expose, project(Core.class));

        // Commands
        commandAlias("build", requestFor(LibraryJarFile.class),
            requestFor(JavadocDirectory.class));
        commandAlias("javadoc", requestFor(JavadocDirectory.class));
        commandAlias("eclipse", requestFor(EclipseConfiguration.class));
        commandAlias("pomFile", requestFor(PomFile.class));
        commandAlias("mavenPublication", requestFor(MvnPublication.class));
    }
}
