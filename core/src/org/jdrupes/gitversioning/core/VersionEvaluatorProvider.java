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

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jdrupes.gitversioning.api.TagFilter;
import org.jdrupes.gitversioning.api.TagProcessor;
import org.jdrupes.gitversioning.api.VersionEvaluator;

/**
 * A simple version evaluator provider. It looks up the tags in the
 * current branch, uses the tag filter to identify the version tags
 * and selects the latest one.
 * 
 * It then invokes the tag processor to produce a version string.
 */
public class VersionEvaluatorProvider
        implements org.jdrupes.gitversioning.api.VersionEvaluatorProvider {

    /** The logger. */
    protected final Logger log = Logger.getLogger(getClass().getName());
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Map<ObjectId, Set<ObjectId>> reachableByHead
        = new ConcurrentHashMap<>();
    private Repository repository;
    private Path directory;
    private TagFilter tagFilter = new DefaultTagFilter();
    private TagProcessor tagProcessor = new MavenStyleTagProcessor();

    /**
     * Initializes a new version evaluator provider.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public VersionEvaluatorProvider() {
        // Make javadoc happy.
    }

    @Override
    public VersionEvaluatorProvider repository(Repository repository) {
        this.repository = repository;
        return this;
    }

    @Override
    public VersionEvaluator subDirectory(Path subDirectory) {
        if (subDirectory.isAbsolute()) {
            subDirectory = subDirectory.relativize(
                repository.getWorkTree().toPath());
        }
        directory = subDirectory;
        return this;
    }

    @Override
    public VersionEvaluator tagFilter(TagFilter tagFilter) {
        this.tagFilter = tagFilter;
        return this;
    }

    @Override
    public VersionEvaluator tagProcessor(TagProcessor tagProcessor) {
        this.tagProcessor = tagProcessor;
        return this;
    }

    @Override
    public String version() {
        try {
            var latest = getLatestVersionTagged();
            return tagProcessor.version(repository, directory,
                latest.commit(), latest.tag(), latest.version().toString());
        } catch (IOException | GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    private record VersionedTag(Ref ref, String tag, Semver version) {
    }

    private record VersionedCommit(RevCommit commit, String tag,
            Semver version) {
    }

    private VersionedCommit getLatestVersionTagged()
            throws GitAPIException, IOException {
        try (var git = Git.wrap(repository);
                var revWalk = new RevWalk(repository)) {
            var reachable = reachableCommits();
            return git.tagList().call().stream()
                .mapMulti((Ref ref, Consumer<
                        VersionedTag> consumer) -> addVersionInfo(ref)
                            .ifPresent(consumer))
                .sorted(new Comparator<VersionedTag>() {
                    @Override
                    public int compare(VersionedTag obj1, VersionedTag obj2) {
                        return obj2.version().compareTo(obj1.version());
                    }
                }).mapMulti((VersionedTag vt,
                        Consumer<VersionedCommit> consumer) -> findCommit(
                            revWalk, vt.ref()).ifPresent(
                                c -> consumer.accept(new VersionedCommit(
                                    c, vt.tag(), vt.version()))))
                .filter(vc -> reachable.contains(vc.commit().getId()))
                .findFirst().orElseGet(
                    () -> new VersionedCommit(null, null, new Semver("0.0.0")));
        }
    }

    private Optional<RevCommit> findCommit(RevWalk revWalk, Ref ref) {
        try {
            RevObject refd = revWalk.parseAny(ref.getObjectId());
            revWalk.reset();
            return switch (refd) {
            case RevTag revtag -> Optional
                .of(revWalk.parseCommit(revtag.getObject()));
            case RevCommit revcommit -> Optional.of(revcommit);
            default -> Optional.empty();
            };
        } catch (IOException e) {
            return Optional.empty();
        } finally {
            revWalk.reset();
        }
    }

    private Set<ObjectId> reachableCommits() throws IOException {
        ObjectId headId = repository.resolve("HEAD");
        if (headId == null) {
            // No commits yet
            return Collections.emptySet();
        }
        return reachableByHead.computeIfAbsent(
            headId, k -> {
                try (var revWalk = new RevWalk(repository)) {
                    var reachable = new HashSet<ObjectId>();
                    revWalk.markStart(revWalk.parseCommit(headId));
                    for (RevCommit commit : revWalk) {
                        reachable.add(commit.getId());
                    }
                    return reachable;
                } catch (IOException e) {
                    return Collections.emptySet();
                }
            });
    }

    private Optional<VersionedTag> addVersionInfo(Ref ref) {
        var tag = ref.getName().substring("refs/tags/".length());
        return tagFilter.version(tag).map(v -> {
            try {
                var version = new Semver(v, Semver.SemverType.LOOSE);
                return new VersionedTag(ref, tag, version);
            } catch (SemverException e) {
                throw new IllegalArgumentException(
                    "Failed to parse version: " + v, e);
            }
        });
    }

}
