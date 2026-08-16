package org.jdrupes.gitversioning.core;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import java.util.Map;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.jdrupes.gitversioning.api.VersionEvaluator;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenStyleTagProcessorTests {

    @TempDir
    Path tempDir;
    private Git git;
    private Repository repository;

    @AfterEach
    void tearDown() throws Exception {
        if (git != null) {
            git.close();
        }
        clearReachableCache();
    }

    private void clearReachableCache() {
        try {
            Field field = VersionEvaluatorProvider.class
                .getDeclaredField("reachableByHead");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ObjectId, ?> cache = (Map<ObjectId, ?>) field.get(null);
            cache.clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private void initRepo() throws Exception {
        git = Git.init().setInitialBranch("main")
            .setDirectory(tempDir.toFile()).call();
        repository = git.getRepository();
        repository.getConfig().setString("user", null, "name", "Test");
        repository.getConfig().setString("user", null, "email",
            "test@test.com");
        repository.getConfig().save();
    }

    private void writeFile(String relativePath, String content)
            throws java.io.IOException {
        var path = tempDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, CREATE, TRUNCATE_EXISTING);
    }

    private void commitAll(String message) throws Exception {
        git.add().addFilepattern(".").call();
        git.commit().setMessage(message).call();
    }

    private void tag(String name) throws Exception {
        git.tag().setName(name).call();
    }

    private void checkoutBranch(String name) throws Exception {
        git.checkout().setCreateBranch(true).setName(name).call();
    }

    private VersionEvaluator createEvaluator() {
        return new VersionEvaluatorProvider()
            .repository(repository);
    }

    // --- SNAPSHOT version passthrough ---

    @Test
    void snapshotVersionReturnedUnchanged() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0-SNAPSHOT");

        var processor = new MavenStyleTagProcessor();
        var result = processor.version(createEvaluator(), "1.0.0-SNAPSHOT",
            "1.0.0-SNAPSHOT");

        assertEquals("1.0.0-SNAPSHOT", result);
    }

    @Test
    void snapshotVersionUnchangedEvenWithDirtyFiles() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("2.0.0-SNAPSHOT");
        writeFile("src/Main.java", "changed");
        git.add().addFilepattern(".").call();

        var processor = new MavenStyleTagProcessor();
        var result = processor.version(createEvaluator(), "2.0.0-SNAPSHOT",
            "2.0.0-SNAPSHOT");

        assertEquals("2.0.0-SNAPSHOT", result);
    }

    @Test
    void snapshotVersionUnchangedOnFeatureBranch() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("feature/x");
        tag("1.0.0-SNAPSHOT");

        var processor = new MavenStyleTagProcessor();
        var result = processor.version(createEvaluator(), "1.0.0-SNAPSHOT",
            "1.0.0-SNAPSHOT");

        assertEquals("1.0.0-SNAPSHOT", result);
    }

    // --- Clean version passthrough (no dirty, no modified) ---

    @Test
    void cleanVersionOnMainBranch() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0");

        var processor = new MavenStyleTagProcessor();
        var result = processor.version(createEvaluator(), "1.0.0", "1.0.0");

        assertEquals("1.0.0", result);
    }

    @Test
    void cleanVersionOnMasterBranch() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("master");
        tag("1.0.0");

        var processor = new MavenStyleTagProcessor();
        var result = processor.version(createEvaluator(), "1.0.0", "1.0.0");

        assertEquals("1.0.0", result);
    }

    @Test
    void cleanVersionOnFeatureBranch() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("feature/my-feature");
        tag("1.0.0");

        var processor = new MavenStyleTagProcessor();
        var result = processor.version(createEvaluator(), "1.0.0", "1.0.0");

        assertEquals("1.0.0", result);
    }

    // --- Dirty files trigger version bump ---

    @Test
    void dirtyFilesBumpPatchOnMain() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
    }

    @Test
    void dirtyFilesBumpPatchOnFeatureBranch() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("feature/my-feature");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-feature_my_feature-SNAPSHOT", result);
    }

    @Test
    void dirtyFilesBumpPatchOnComplexBranchName() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("feature/my-feature!branch@2025");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-feature_my_feature_branch_2025-SNAPSHOT", result);
    }

    // --- Modified files trigger version bump ---

    @Test
    void modifiedFilesBumpPatchOnMain() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0");
        writeFile("src/Main.java", "changed");
        commitAll("modified");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
    }

    @Test
    void modifiedFilesBumpPatchOnFeatureBranch() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("develop");
        tag("1.0.0");
        writeFile("src/Main.java", "changed");
        commitAll("modified");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-develop-SNAPSHOT", result);
    }

    // --- Version increment correctness ---

    @Test
    void versionBumpFrom100To101() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
    }

    @Test
    void versionBumpFrom234To235() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("2.3.4");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "2.3.4", "2.3.4");

        assertEquals("2.3.5-SNAPSHOT", result);
    }

    // --- Ignored branches ---

    @Test
    void defaultIgnoredBranchMain() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
        assertFalse(result.contains("main"));
    }

    @Test
    void defaultIgnoredBranchMaster() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("master");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
        assertFalse(result.contains("master"));
    }

    @Test
    void ignoredBranchesReplacesDefaults() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor()
            .ignoredBranches("develop");

        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-main-SNAPSHOT", result);
    }

    @Test
    void ignoredBranchesReplacesDefaultsDevelopBranch() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("develop");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor()
            .ignoredBranches("develop");

        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
    }

    @Test
    void ignoredBranchesWithRegex() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("release-1.0");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor()
            .ignoredBranches("release-.*");

        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
        assertFalse(result.contains("release"));
    }

    @Test
    void ignoreBranchesAddsToDefaults() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor()
            .ignoreBranches("develop");

        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
        assertFalse(result.contains("main"));
    }

    @Test
    void ignoreBranchesDoesNotRemoveDefaults() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("master");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor()
            .ignoreBranches("develop");

        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
        assertFalse(result.contains("master"));
    }

    // --- Branch name normalization ---

    @Test
    void branchNameNormalizationSlash() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("feature/my-feature");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-feature_my_feature-SNAPSHOT", result);
    }

    @Test
    void branchNameNormalizationSpecialChars() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("hotfix/v1.0.0-fix");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-hotfix_v1_0_0_fix-SNAPSHOT", result);
    }

    @Test
    void branchNameNormalizationPlus() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("feature+my+branch");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-feature_my_branch-SNAPSHOT", result);
    }

    @Test
    void branchNameKeepsUnderscores() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("feature_my_branch");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-feature_my_branch-SNAPSHOT", result);
    }

    @Test
    void branchNameKeepsAlphanumeric() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("abc123");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-abc123-SNAPSHOT", result);
    }

    // --- Builder pattern ---

    @Test
    void ignoredBranchesReturnsThis() {
        var processor = new MavenStyleTagProcessor();
        assertSame(processor, processor.ignoredBranches("main"));
    }

    @Test
    void ignoreBranchesReturnsThis() {
        var processor = new MavenStyleTagProcessor();
        assertSame(processor, processor.ignoreBranches("develop"));
    }

    @Test
    void ignoredBranchesMultiplePatterns() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        checkoutBranch("develop");
        tag("1.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor()
            .ignoredBranches("develop", "integration");

        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
        assertFalse(result.contains("develop"));
    }

    // --- Edge cases ---

    @Test
    void zeroVersionWithChanges() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("0.0.0");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "0.0.0", "0.0.0");

        assertEquals("0.0.1-SNAPSHOT", result);
    }

    @Test
    void largeVersionNumber() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("10.20.30");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "10.20.30", "10.20.30");

        assertEquals("10.20.31-SNAPSHOT", result);
    }

    @Test
    void tagNameWithPrefix() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "v1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
    }

    @Test
    void modifiedAndDirtyBothPresent() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0");
        writeFile("src/Main.java", "changed");
        commitAll("modified");
        writeFile("src/NewFile.java", "new");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator().matchingGlob("**");
        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.1-SNAPSHOT", result);
    }

    @Test
    void matcherFiltersDirtyAndModified() throws Exception {
        initRepo();
        writeFile("src/Main.java", "x");
        writeFile("test/Test.java", "x");
        commitAll("initial");
        tag("1.0.0");
        writeFile("test/Test.java", "new content");

        var processor = new MavenStyleTagProcessor();
        var evaluator = createEvaluator()
            .matchingGlob("src/**");

        var result = processor.version(evaluator, "1.0.0", "1.0.0");

        assertEquals("1.0.0", result);
    }
}
