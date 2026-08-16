/*
 * JDrupes GitVersioning
 * Copyright (C) 2025, 2026 Michael N. Lipp
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

import com.vdurmont.semver4j.Semver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jdrupes.gitversioning.api.TagProcessor;
import org.jdrupes.gitversioning.api.VersionEvaluator;

/**
 * Maven-style {@link TagProcessor} implementation.
 *
 * <p>If the tagged version ends with {@code -SNAPSHOT}, it is returned
 * unchanged.
 *
 * <p>If there are no dirty files and no files modified since the tag, the
 * tagged version is returned unchanged.
 *
 * <p>Otherwise, the patch number is incremented, the normalized branch name
 * is appended (unless the branch matches an ignored pattern), and
 * {@code -SNAPSHOT} is appended. By default, {@code main} and {@code master}
 * are ignored.
 */
public class MavenStyleTagProcessor extends TagProcessorBase {

    private static final List<Pattern> DEFAULT_PATTERNS
        = List.of(Pattern.compile("main"), Pattern.compile("master"));
    private List<Pattern> ignoredBranches = new ArrayList<>();

    /**
     * Creates a processor with default ignored branches ({@code main},
     * {@code master}).
     */
    public MavenStyleTagProcessor() {
        ignoredBranches.addAll(DEFAULT_PATTERNS);
    }

    /**
     * Replaces the ignored branch patterns. Matching branches are omitted
     * from the generated version string.
     *
     * @param patterns the regular expression patterns
     * @return this processor for chaining
     */
    public MavenStyleTagProcessor ignoredBranches(String... patterns) {
        ignoredBranches = new ArrayList<>(
            Arrays.stream(patterns).map(Pattern::compile).toList());
        return this;
    }

    /**
     * Adds patterns to the list of ignored branches.
     *
     * @param patterns the regular expression patterns
     * @return this processor for chaining
     */
    public MavenStyleTagProcessor ignoreBranches(String... patterns) {
        ignoredBranches
            .addAll(Arrays.stream(patterns).map(Pattern::compile).toList());
        return this;
    }

    @Override
    public String version(VersionEvaluator evaluator, String tagName,
            String version) throws IOException, GitAPIException {
        if (version.endsWith("-SNAPSHOT")) {
            return version;
        }
        if (evaluator.dirtyFiles().findAny().isEmpty()
            && evaluator.modifiedFiles().findAny().isEmpty()) {
            return version;
        }

        // Need new version
        Semver semver
            = new Semver(version, Semver.SemverType.LOOSE).nextPatch();
        StringBuilder newVersion = new StringBuilder(semver.toString());
        var branch = evaluator.repository().getBranch();
        if (!ignoredBranches.stream().map(p -> p.matcher(branch).matches())
            .filter(b -> b).findAny().isPresent()) {
            newVersion.append('-')
                .append(branch.replaceAll("[^_A-Za-z0-9]", "_"));
        }
        newVersion.append("-SNAPSHOT");
        return newVersion.toString();
    }
}
