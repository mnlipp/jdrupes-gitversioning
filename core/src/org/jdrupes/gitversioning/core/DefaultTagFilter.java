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

import java.util.Optional;
import java.util.regex.Pattern;
import org.jdrupes.gitversioning.api.TagFilter;

/**
 * A default implementation of {@link TagFilter}.
 */
public class DefaultTagFilter implements TagFilter {

    /**
     * The default version pattern.
     * {@code ([0-9]+(?:\.[0-9]+){0,2}(?:-[a-zA-Z0-9\+\-_]+)?)}
     */
    public static final String VERSION_PATTERN
        = "([0-9]+(?:\\.[0-9]+){0,2}(?:-[a-zA-Z0-9\\+\\-_]+)?)";

    private String pattern = VERSION_PATTERN;
    private Pattern compiledPattern;

    /**
     * Initializes a new tag filter that uses the {@link #VERSION_PATTERN}.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public DefaultTagFilter() {
        // Make javadoc happy
    }

    /**
     * Sets the pattern. The pattern must have a single capture group
     * that matches the version.
     *
     * @param pattern the pattern
     * @return the default tag filer
     */
    public DefaultTagFilter pattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    /**
     * Prepend the prefix to the existing pattern. This is the most
     * common use case.
     *
     * @param prefix the prefix
     * @return the default tag filer
     */
    public DefaultTagFilter prepend(String prefix) {
        pattern = prefix + pattern;
        return this;
    }

    @Override
    public Optional<String> version(String tagName) {
        if (compiledPattern == null) {
            compiledPattern = Pattern.compile(pattern);
        }
        var matcher = compiledPattern.matcher(tagName);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

}
