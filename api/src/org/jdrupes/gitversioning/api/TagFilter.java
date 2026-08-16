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

import java.util.Optional;

/**
 * Determines whether a tag name represents a version tag and extracts
 * the version string from it.
 *
 * <p>Used by the evaluator to identify which tags are version-relevant.
 */
@FunctionalInterface
public interface TagFilter {

    /**
     * Returns the version part of the tag if the tag matches.
     *
     * @param tagName the full tag name (without {@code refs/tags/} prefix)
     * @return the version string if the tag matches, empty otherwise
     */
    Optional<String> version(String tagName);
}
