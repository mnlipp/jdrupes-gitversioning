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

import io.github.azagniotov.matcher.AntPathMatcher;
import java.nio.file.Path;

/**
 * Matches paths against an Ant-style pattern.
 *
 * <p>Supports {@code *}, {@code **}, and {@code ?} wildcards via the
 * ant-style-path-matcher library.
 */
public class AntPatternMatcher implements IncludeMatcher {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final AntPathMatcher pathMatcher
        = new AntPathMatcher.Builder().build();
    private final String pattern;

    /**
     * Creates a new Ant pattern matcher.
     *
     * @param pattern the Ant-style pattern
     */
    public AntPatternMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(Path path) {
        return pathMatcher.isMatch(pattern, path.toString());
    }

}
