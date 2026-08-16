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
 * Configurable version evaluator for a Git repository.
 *
 * <p>By default, all files in the work tree are considered when evaluating
 * the version. Restrict the scope using the {@code matching*} methods or
 * {@link #subDirectory(java.nio.file.Path)}. Multiple matchers are combined
 * with a logical OR.
 *
 * <p>Obtain an instance via {@link #forRepository(Repository)}
 * or {@link #forRepository(Repository, ClassLoader)}.
 */
public interface VersionEvaluator {

    /**
     * Creates a version evaluator for the given repository. The
     * implementation is looked up using the {@link ServiceLoader} mechanism
     * with the given class loader.
     *
     * @param repository the repository
     * @param classLoader the class loader to use for service loading
     * @return the version evaluator
     * @throws java.util.NoSuchElementException if no
     * {@link VersionEvaluatorProvider} is available
     */
    static VersionEvaluator forRepository(Repository repository,
            ClassLoader classLoader) {
        ServiceLoader<VersionEvaluatorProvider> loader
            = ServiceLoader.load(VersionEvaluatorProvider.class, classLoader);
        return loader.findFirst().orElseThrow().repository(repository);
    }

    /**
     * Creates a version evaluator for the given repository. The
     * implementation is looked up using the {@link ServiceLoader} mechanism,
     * trying the context class loader of the current thread and the class
     * loader of this class.
     *
     * @param repository the repository
     * @return the version evaluator
     * @throws java.util.NoSuchElementException if no
     * {@link VersionEvaluatorProvider} is available
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
     * Returns the evaluator's repository.
     *
     * @return the repository
     */
    Repository repository();

    /**
     * Sets the tag filter to use. The filter determines which tags are
     * recognized as version tags and extracts the version string from them.
     *
     * @param tagFilter the tag filter
     * @return this evaluator for chaining
     */
    VersionEvaluator tagFilter(TagFilter tagFilter);

    /**
     * Sets the tag processor to use. The processor generates the final
     * version string from the tag name and parsed version.
     *
     * @param tagProcessor the tag processor
     * @return this evaluator for chaining
     */
    VersionEvaluator tagProcessor(TagProcessor tagProcessor);

    /**
     * Include all files matching the given glob expression when evaluating
     * the version.
     *
     * @param glob the glob expression
     * @return this evaluator for chaining
     */
    VersionEvaluator matchingGlob(String glob);

    /**
     * Include all files matching the given regular expression when evaluating
     * the version.
     *
     * @param regex the regular expression
     * @return this evaluator for chaining
     */
    VersionEvaluator matchingRegex(String regex);

    /**
     * Include all files matching the given Ant pattern when evaluating
     * the version.
     *
     * @param pattern the Ant pattern
     * @return this evaluator for chaining
     */
    VersionEvaluator matchingAntPattern(String pattern);

    /**
     * Include all files under the given sub-directory when evaluating
     * the version.
     *
     * @param subDirectory the sub-directory, relative to the repository work
     * tree or absolute
     * @return this evaluator for chaining
     */
    VersionEvaluator subDirectory(Path subDirectory);

    /**
     * Returns a stream of "dirty" (uncommitted or untracked) files
     * in the work tree that match the configured file selection.
     *
     * @return a stream of paths
     */
    Stream<Path> dirtyFiles();

    /**
     * Returns a stream of files modified since the latest version tag
     * that match the configured file selection.
     *
     * @return a stream of paths
     */
    Stream<Path> modifiedFiles();

    /**
     * Evaluates and returns the version string for the current repository
     * state.
     *
     * @return the version string
     */
    String version();
}
