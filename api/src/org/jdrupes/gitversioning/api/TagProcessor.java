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

package org.jdrupes.gitversioning.api;

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * A [TagProcessor] generates a version based on information retrieved from
 * a git repository.
 */
@FunctionalInterface
public interface TagProcessor {

    /**
     * Generate the version.
     *
     * @param repository the git repository
     * @param subDir the sub directory of the repository where the code
     * is located that is relevant for versioning. May be null if the
     * whole repository is relevant.
     * @param commit the latest commit with a version tag (see [TagFilter]).
     * May be null if no such commit exists.
     * @param tagName the tag name the tag's name. May be null if no such
     * tag exists.
     * @param version the version part of the tag. Defaults to "0.0.0" if
     * no tag was found.
     * @return the version
     * @throws IOException is handled by the invoker as a convenience
     * @throws GitAPIException is handled by the onvoker as a convenience
     */
    String version(Repository repository, Path subDir, RevCommit commit,
            String tagName, String version) throws IOException, GitAPIException;
}
