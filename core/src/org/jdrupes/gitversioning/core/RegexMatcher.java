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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Matches paths against a Java regular expression.
 *
 * <p>Delegates to {@link PathMatcher} with the {@code regex:} scheme.
 */
public class RegexMatcher implements IncludeMatcher {

    private final PathMatcher matcher;

    /**
     * Creates a new regex matcher.
     *
     * @param pattern the regular expression pattern
     */
    public RegexMatcher(String pattern) {
        matcher = FileSystems.getDefault().getPathMatcher("regex:" + pattern);
    }

    @Override
    public boolean matches(Path path) {
        return matcher.matches(path);
    }

}
