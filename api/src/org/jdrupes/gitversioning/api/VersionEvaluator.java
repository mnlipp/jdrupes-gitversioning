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

import java.nio.file.Path;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.Repository;

/**
 * Defines a configurable version evaluator.
 * 
 * By default, all files in the work tree are included when evaluating
 * the version. This can be changed using the "{@code matching}..." methods
 * (and {@link #subDirectory(Path)}). The file selection by these methods
 * is accumulative, i.e. is a logical OR of the matching rules.
 */
public interface VersionEvaluator {

    /**
     * Creates a version evaluator for the given repository. The
     * implementation is looked up using the [ServiceLoader] mechanism
     * with the given class loader.
     *
     * @param repository the repository
     * @param classLoader the class loader
     * @return the version evaluator
     */
    static VersionEvaluator forRepository(Repository repository,
            ClassLoader classLoader) {
        ServiceLoader<VersionEvaluatorProvider> loader
            = ServiceLoader.load(VersionEvaluatorProvider.class, classLoader);
        return loader.findFirst().orElseThrow().repository(repository);
    }

    /**
     * Creates a version evaluator for the given repository. The
     * implementation is looked up using the [ServiceLoader] mechanism,
     * using the class loaders of the current thread and the
     * [VersionEvaluator] class.
     *
     * @param repository the repository
     * @return the version evaluator
     */
    static VersionEvaluator forRepository(Repository repository) {
        return Stream.of(Thread.currentThread().getContextClassLoader(),
            VersionEvaluator.class.getClassLoader()).filter(Objects::nonNull)
            .map(cl -> ServiceLoader.load(VersionEvaluatorProvider.class, cl)
                .stream())
            .flatMap(s -> s).findFirst().map(Provider::get).orElseThrow()
            .repository(repository);
    }

    /**
     * Returns the evalutor's repository.
     *
     * @return the repository
     */
    Repository repository();

    /**
     * Sets the tag filter to use.
     *
     * @param tagFilter the tag filter
     * @return the version evaluator
     */
    VersionEvaluator tagFilter(TagFilter tagFilter);

    /**
     * Sets the tag processor to use.
     *
     * @param tagProcessor the tag processor
     * @return the version evaluator
     */
    VersionEvaluator tagProcessor(TagProcessor tagProcessor);

    /**
     * Include all files matching the given glob when evaluating the version.
     *
     * @param glob the glob expression
     * @return the version evaluator
     */
    VersionEvaluator matchingGlob(String glob);

    /**
     * Include all files matching the given regex when evaluating the version.
     *
     * @param regex the regex
     * @return the version evaluator
     */
    VersionEvaluator matchingRegex(String regex);

    /**
     * Include all files matching the given Ant pattern when evaluating
     * the version.
     *
     * @param pattern the pattern
     * @return the version evaluator
     */
    VersionEvaluator matchingAntPattern(String pattern);

    /**
     * Include all files in the given sub directory when evaluating the version.
     *
     * @param subDirectory the sub directory
     * @return the version evaluator
     */
    VersionEvaluator subDirectory(Path subDirectory);

    /**
     * Returns the list of "dirty" (uncommitted or untracked) files
     * in the work tree that match the configured selection.
     *
     * @return the list
     */
    Stream<Path> dirtyFiles();

    /**
     * Returns the list of files in the work tree that have been modified
     * since the last commit and match the configured selection.
     *
     * @return the stream
     */
    Stream<Path> modifiedFiles();

    /**
     * Returns the evaluated version.
     *
     * @return the string
     */
    String version();
}
