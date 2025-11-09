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

package org.jdrupes.gitversioning.core;

import java.nio.file.Path;
import java.util.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.jdrupes.gitversioning.api.TagProcessor;

/**
 * A base class for {@link TagProcessor}s.
 */
public abstract class TagProcessorBase implements TagProcessor {

    /** The logger. */
    protected final Logger log = Logger.getLogger(getClass().getName());

    /**
     * Initializes a new tag processor base.
     */
    protected TagProcessorBase() {
        // Make javadoc happy.
    }

    /**
     * Checks if the sub directory contains that have been added, removed
     * or modified since the last commit.
     *
     * @param repository the repository
     * @param subDir the sub dir
     * @return true, if is dirty
     * @throws GitAPIException the git API exception
     */
    protected static boolean isDirty(Repository repository, Path subDir)
            throws GitAPIException {
        try (Git git = Git.wrap(repository)) {
            Status status = git.status().call();
            String start = subDir == null ? "" : subDir.toString();

            return status.getModified().stream()
                .anyMatch(path -> path.startsWith(start))
                || status.getUntracked().stream()
                    .anyMatch(path -> path.startsWith(start))
                || status.getUncommittedChanges().stream()
                    .anyMatch(path -> path.startsWith(start))
                || status.getMissing().stream()
                    .anyMatch(path -> path.startsWith(start))
                || status.getConflicting().stream()
                    .anyMatch(path -> path.startsWith(start))
                || status.getAdded().stream()
                    .anyMatch(path -> path.startsWith(start))
                || status.getRemoved().stream()
                    .anyMatch(path -> path.startsWith(start));
        }
    }
}
