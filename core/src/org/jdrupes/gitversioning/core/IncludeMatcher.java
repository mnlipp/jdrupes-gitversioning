/*
 * JDrupes GitVersioning
 * Copyright (C) 2026 Michael N. Lipp
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

/**
 * Matches a file path against a pattern.
 *
 * <p>Internal interface used by {@link VersionEvaluatorProvider} to
 * determine which files are relevant for version evaluation.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface IncludeMatcher {

    /**
     * Checks whether the path matches the pattern.
     *
     * @param path the file path (relative to the work tree)
     * @return {@code true} if the path matches
     */
    boolean matches(Path path);
}
