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
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.jdrupes.gitversioning.api.TagFilter;
import org.jdrupes.gitversioning.api.TagProcessor;
import org.jdrupes.gitversioning.api.VersionEvaluator;

/**
 * Reference implementation of
 * {@link org.jdrupes.gitversioning.api.VersionEvaluatorProvider}.
 *
 * <p>Finds the latest semantically versioned tag reachable from HEAD using the
 * configured {@link TagFilter}, then delegates to the configured
 * {@link TagProcessor} to produce the final version string.
 *
 * <p>Uses a {@link ConcurrentHashMap} to cache the set of
 * commits reachable from HEAD, avoiding redundant graph walks.
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class VersionEvaluatorProvider
        implements org.jdrupes.gitversioning.api.VersionEvaluatorProvider {

    /** Logger for this instance. */
    protected final Logger log = Logger.getLogger(getClass().getName());
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Map<ObjectId, Set<ObjectId>> reachableByHead
        = new ConcurrentHashMap<>();
    private Repository repository;
    private final List<IncludeMatcher> matchers = new ArrayList<>();
    private TagFilter tagFilter = new DefaultTagFilter();
    private TagProcessor tagProcessor = new MavenStyleTagProcessor();

    /**
     * Creates a new evaluator provider with default tag filter and processor.
     */
    public VersionEvaluatorProvider() {
        // Make javadoc happy.
    }

    @Override
    public VersionEvaluatorProvider repository(Repository repository) {
        this.repository = Objects.requireNonNull(repository);
        return this;
    }

    @Override
    public Repository repository() {
        return repository;
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
    public VersionEvaluator matchingGlob(String glob) {
        matchers.add(new GlobMatcher(glob));
        return this;
    }

    @Override
    public VersionEvaluator matchingRegex(String regex) {
        matchers.add(new RegexMatcher(regex));
        return this;
    }

    @Override
    public VersionEvaluator matchingAntPattern(String pattern) {
        matchers.add(new AntPatternMatcher(pattern));
        return this;
    }

    @Override
    public VersionEvaluator subDirectory(Path subDirectory) {
        var subDir = relativizeDirectory(repository, subDirectory).toString();
        if (subDir.isEmpty()) {
            return this;
        }
        if (!subDir.endsWith("/")) {
            subDir = subDir + "/";
        }
        return matchingAntPattern(subDir + "**");
    }

    /**
     * If sub directory is absolute, return it as a path relative
     * to the repository's work tree.
     *
     * @param repository the repository
     * @param subDirectory the sub directory
     * @return the path
     */
    /* default */ static Path relativizeDirectory(Repository repository,
            Path subDirectory) {
        if (!subDirectory.isAbsolute()) {
            return subDirectory;
        }
        if (!subDirectory.startsWith(
            repository.getWorkTree().toPath().toAbsolutePath())) {
            throw new IllegalArgumentException(subDirectory
                + " is not a directory within the working tree");
        }
        return repository.getWorkTree().toPath().relativize(subDirectory);
    }

    private boolean matches(Path path) {
        return matchers.isEmpty()
            || matchers.stream().filter(m -> m.matches(path)).findAny()
                .isPresent();
    }

    @Override
    public Stream<Path> dirtyFiles() {
        try (Git git = Git.wrap(repository)) {
            Status status = git.status().call();

            // Uncommitted combines added, changed, removed, missing,
            // modified and conflicting
            return Stream.concat(status.getUncommittedChanges().stream(),
                status.getUntracked().stream()).map(Path::of)
                .filter(this::matches);
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Stream<Path> modifiedFiles() {
        try {
            var latest = getLatestVersionTagged();
            return modifiedFiles(latest.commit());
        } catch (IOException | GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.CognitiveComplexity", "PMD.NcssCount" })
    private Stream<Path> modifiedFiles(RevCommit taggedCommit)
            throws IOException, GitAPIException {
        var headId = repository.resolve("HEAD");
        if (headId == null || taggedCommit == null
            || taggedCommit.getId().equals(headId)) {
            return Stream.empty();
        }

        @SuppressWarnings("PMD.CloseResource")
        var revWalk = new RevWalk(repository);
        @SuppressWarnings("PMD.CloseResource")
        var git = new Git(repository);
        @SuppressWarnings("PMD.CloseResource")
        var reader = repository.newObjectReader();
        try {
            revWalk.markStart(revWalk.parseCommit(headId));
            var commits = revWalk.iterator();
            var taggedId = taggedCommit.getId();
            var spliterator = new AbstractSpliterator<Path>(
                Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
                private Iterator<DiffEntry> diffs = Collections.emptyIterator();
                private boolean finished;

                @Override
                public boolean tryAdvance(Consumer<? super Path> action) {
                    while (!finished) {
                        // Lazily consume the current commit's diffs.
                        while (diffs.hasNext()) {
                            var diff = diffs.next();
                            var newPath = Path.of(diff.getNewPath());
                            if (matches(newPath)) {
                                action.accept(newPath);
                                return true;
                            }
                            var oldPath = Path.of(diff.getOldPath());
                            if (matches(oldPath)) {
                                action.accept(oldPath);
                                return true;
                            }
                        }

                        // Lazily advance to the next commit.
                        if (!commits.hasNext()) {
                            finished = true;
                            return false;
                        }
                        var commit = commits.next();
                        if (commit.getId().equals(taggedId)) {
                            finished = true;
                            return false;
                        }

                        // Next commit, new diffs
                        diffs = nextDiffs(git, reader, commit);
                    }
                    return false;
                }

                private Iterator<DiffEntry> nextDiffs(Git git,
                        ObjectReader reader,
                        RevCommit commit) {
                    try {
                        var oldTreeParser = new CanonicalTreeParser();
                        oldTreeParser.reset(reader,
                            commit.getParent(0).getTree().getId());
                        var newTreeParser = new CanonicalTreeParser();
                        newTreeParser.reset(reader,
                            commit.getTree().getId());
                        return git.diff().setNewTree(newTreeParser)
                            .setOldTree(oldTreeParser).call().iterator();
                    } catch (GitAPIException e) {
                        throw new UncheckedIOException(new IOException(
                            "Unable to calculate Git diff", e));
                    } catch (IOException e) {
                        throw new UncheckedIOException(
                            "Unable to calculate Git diff", e);
                    }
                }
            };

            return StreamSupport.stream(spliterator, false)
                .onClose(() -> {
                    reader.close();
                    git.close();
                    revWalk.close();
                });

        } catch (RuntimeException | Error e) {
            reader.close();
            git.close();
            revWalk.close();
            throw e;
        }
    }

    @Override
    public String version() {
        try {
            var latest = getLatestVersionTagged();
            return tagProcessor.version(this, latest.tag(),
                latest.version().toString());
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
