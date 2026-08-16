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
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Generates a version string from Git repository information.
 *
 * <p>Receives the evaluated tag name and parsed version, then produces the
 * final version string. Implementations typically incorporate information
 * about dirty files, commit count, or branch name.
 */
@FunctionalInterface
public interface TagProcessor {

    /**
     * Generates the version string.
     *
     * @param evaluator the version evaluator, providing access to the
     * repository state (dirty files, modified files, etc.)
     * @param tagName the tag name, or {@code null} if no matching tag exists
     * @param version the parsed version from the tag, defaults to {@code 0.0.0}
     * if no tag matches
     * @return the version string
     * @throws IOException if an I/O error occurs accessing the repository
     * @throws GitAPIException if a Git operation fails
     */
    String version(VersionEvaluator evaluator,
            String tagName, String version) throws IOException, GitAPIException;
}
