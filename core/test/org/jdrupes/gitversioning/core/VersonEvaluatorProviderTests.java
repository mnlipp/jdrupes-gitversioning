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
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VersonEvaluatorProviderTests {

    @TempDir
    Path tempDir;
    private Git git;
    private Repository repository;

    @AfterEach
    void tearDown() {
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
            // Ignore - cache field may not exist in future versions
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

    // --- matchingGlob tests ---

    @Test
    void matchingGlobBasic() throws Exception {
        initRepo();

        writeFile("Main.java", "class Main {}");
        writeFile("Util.java", "class Util {}");
        writeFile("README.md", "# Readme");
        commitAll("initial");

        writeFile("Main.java", "class Main { updated }");
        writeFile("Util.java", "class Util { updated }");
        writeFile("README.md", "# Readme updated");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("*.java");

        var dirty = provider.dirtyFiles().sorted().toList();
        assertEquals(2, dirty.size());
        assertTrue(dirty.contains(Path.of("Main.java")));
        assertTrue(dirty.contains(Path.of("Util.java")));
    }

    @Test
    void matchingGlobRecursive() throws Exception {
        initRepo();

        writeFile("src/a/Test1.java", "x");
        writeFile("src/b/Test2.java", "x");
        writeFile("src/b/deep/Test3.java", "x");
        writeFile("src/readme.md", "x");
        commitAll("initial");

        writeFile("src/a/Test1.java", "changed");
        writeFile("src/b/Test2.java", "changed");
        writeFile("src/b/deep/Test3.java", "changed");
        writeFile("src/readme.md", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**/*.java");

        var dirty = provider.dirtyFiles().sorted().toList();
        assertEquals(3, dirty.size());
    }

    @Test
    void matchingGlobNoMatch() throws Exception {
        initRepo();

        writeFile("src/Main.java", "class Main {}");
        commitAll("initial");

        writeFile("src/Main.java", "class Main { updated }");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("*.txt");

        assertTrue(provider.dirtyFiles().toList().isEmpty());
    }

    @Test
    void matchingGlobDirectoryPattern() throws Exception {
        initRepo();

        writeFile("src/Main.java", "x");
        writeFile("test/Test.java", "x");
        commitAll("initial");

        writeFile("src/Main.java", "changed");
        writeFile("test/Test.java", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("src/**");

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
        assertEquals(Path.of("src", "Main.java"), dirty.get(0));
    }

    // --- matchingRegex tests ---

    @Test
    void matchingRegexBasic() throws Exception {
        initRepo();

        writeFile("src/Main.java", "x");
        writeFile("src/Test.java", "x");
        writeFile("README.md", "x");
        commitAll("initial");

        writeFile("src/Main.java", "changed");
        writeFile("src/Test.java", "changed");
        writeFile("README.md", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingRegex(".*Test.*");

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
        assertEquals(Path.of("src", "Test.java"), dirty.get(0));
    }

    @Test
    void matchingRegexExtension() throws Exception {
        initRepo();

        writeFile("a.java", "x");
        writeFile("b.txt", "x");
        writeFile("c.java", "x");
        writeFile("d.md", "x");
        commitAll("initial");

        writeFile("a.java", "changed");
        writeFile("b.txt", "changed");
        writeFile("c.java", "changed");
        writeFile("d.md", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingRegex(".*\\.java");

        var dirty = provider.dirtyFiles().sorted().toList();
        assertEquals(2, dirty.size());
    }

    // --- matchingAntPattern tests ---

    @Test
    void matchingAntPatternBasic() throws Exception {
        initRepo();

        writeFile("src/Main.java", "x");
        writeFile("test/Test.java", "x");
        writeFile("README.md", "x");
        commitAll("initial");

        writeFile("src/Main.java", "changed");
        writeFile("test/Test.java", "changed");
        writeFile("README.md", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingAntPattern("src/**");

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
        assertEquals(Path.of("src", "Main.java"), dirty.get(0));
    }

    @Test
    void matchingAntPatternWildcard() throws Exception {
        initRepo();

        writeFile("config/app.properties", "x");
        writeFile("config/db.properties", "x");
        writeFile("config/readme.md", "x");
        commitAll("initial");

        writeFile("config/app.properties", "changed");
        writeFile("config/db.properties", "changed");
        writeFile("config/readme.md", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingAntPattern("config/*.properties");

        var dirty = provider.dirtyFiles().sorted().toList();
        assertEquals(2, dirty.size());
    }

    @Test
    void matchingAntPatternRecursiveStar() throws Exception {
        initRepo();

        writeFile("src/main/a/Core.java", "x");
        writeFile("src/main/b/Util.java", "x");
        writeFile("src/test/CoreTest.java", "x");
        commitAll("initial");

        writeFile("src/main/a/Core.java", "changed");
        writeFile("src/main/b/Util.java", "changed");
        writeFile("src/test/CoreTest.java", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingAntPattern("src/main/**");

        var dirty = provider.dirtyFiles().sorted().toList();
        assertEquals(2, dirty.size());
    }

    // --- Multiple matchers (OR logic) ---

    @Test
    void matchingMultipleMatchersOr() throws Exception {
        initRepo();

        writeFile("src/Main.java", "x");
        writeFile("docs/guide.md", "x");
        writeFile("config/app.yml", "x");
        commitAll("initial");

        writeFile("src/Main.java", "changed");
        writeFile("docs/guide.md", "changed");
        writeFile("config/app.yml", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**/*.java")
            .matchingGlob("**/*.md");

        var dirty = provider.dirtyFiles().sorted().toList();
        assertEquals(2, dirty.size());
    }

    // --- subDirectory tests ---

    @Test
    void subDirectoryRelative() throws Exception {
        initRepo();

        writeFile("src/main/Main.java", "x");
        writeFile("src/test/Test.java", "x");
        writeFile("docs/readme.md", "x");
        commitAll("initial");

        writeFile("src/main/Main.java", "changed");
        writeFile("src/test/Test.java", "changed");
        writeFile("docs/readme.md", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .subDirectory(Path.of("src", "main"));

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
        assertEquals(Path.of("src", "main", "Main.java"), dirty.get(0));
    }

    @Test
    void subDirectoryAbsolute() throws Exception {
        initRepo();

        writeFile("lib/core/Util.java", "x");
        writeFile("lib/ext/Ext.java", "x");
        writeFile("src/Main.java", "x");
        commitAll("initial");

        writeFile("lib/core/Util.java", "changed");
        writeFile("lib/ext/Ext.java", "changed");
        writeFile("src/Main.java", "changed");
        git.add().addFilepattern(".").call();

        var absolutePath = tempDir.resolve("lib").resolve("core");
        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .subDirectory(absolutePath);

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
    }

    @Test
    void subDirectoryOutsideWorkTree() throws Exception {
        initRepo();

        var outsidePath = Path.of("/tmp/outside");
        var provider = new VersionEvaluatorProvider()
            .repository(repository);

        assertThrows(IllegalArgumentException.class,
            () -> provider.subDirectory(outsidePath));
    }

    // --- dirtyFiles tests ---

    @Test
    void dirtyFilesModified() throws Exception {
        initRepo();

        writeFile("src/Main.java", "class Main {}");
        writeFile("src/Util.java", "class Util {}");
        commitAll("initial");

        writeFile("src/Main.java", "class Main { modified }");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**");

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
        assertEquals(Path.of("src", "Main.java"), dirty.get(0));
    }

    @Test
    void dirtyFilesUntracked() throws Exception {
        initRepo();

        writeFile("src/Main.java", "class Main {}");
        commitAll("initial");

        writeFile("src/NewFile.java", "class NewFile {}");

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**");

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
        assertEquals(Path.of("src", "NewFile.java"), dirty.get(0));
    }

    @Test
    void dirtyFilesMixed() throws Exception {
        initRepo();

        writeFile("a.txt", "x");
        writeFile("b.txt", "x");
        commitAll("initial");

        writeFile("a.txt", "changed");
        git.add().addFilepattern(".").call();
        writeFile("c.txt", "new");

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**");

        var dirty = provider.dirtyFiles().sorted().toList();
        assertEquals(2, dirty.size());
    }

    @Test
    void dirtyFilesWithMatcher() throws Exception {
        initRepo();

        writeFile("src/Main.java", "x");
        writeFile("docs/guide.md", "x");
        commitAll("initial");

        writeFile("src/Main.java", "changed");
        writeFile("docs/guide.md", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**/*.java");

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
        assertEquals(Path.of("src", "Main.java"), dirty.get(0));
    }

    @Test
    void dirtyFilesFilteredByRegex() throws Exception {
        initRepo();

        writeFile("src/Main.java", "x");
        writeFile("src/MainTest.java", "x");
        writeFile("docs/guide.md", "x");
        commitAll("initial");

        writeFile("src/Main.java", "changed");
        writeFile("src/MainTest.java", "changed");
        writeFile("docs/guide.md", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingRegex(".*Test.*");

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
        assertEquals(Path.of("src", "MainTest.java"), dirty.get(0));
    }

    @Test
    void dirtyFilesFilteredBySubDirectory() throws Exception {
        initRepo();

        writeFile("src/main/Main.java", "x");
        writeFile("src/test/Test.java", "x");
        writeFile("docs/readme.md", "x");
        commitAll("initial");

        writeFile("src/main/Main.java", "changed");
        writeFile("src/test/Test.java", "changed");
        writeFile("docs/readme.md", "changed");
        git.add().addFilepattern(".").call();

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .subDirectory(Path.of("src", "test"));

        var dirty = provider.dirtyFiles().toList();
        assertEquals(1, dirty.size());
        assertEquals(Path.of("src", "test", "Test.java"), dirty.get(0));
    }

    // --- modifiedFiles tests ---

    @Test
    void modifiedFilesBasic() throws Exception {
        initRepo();

        writeFile("src/Main.java", "v1");
        writeFile("src/Util.java", "v1");
        commitAll("v1");
        tag("1.0.0");

        writeFile("src/Main.java", "v2");
        commitAll("v2");

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**");

        var modified = provider.modifiedFiles().toList();
        assertEquals(1, modified.size());
        assertEquals(Path.of("src", "Main.java"), modified.get(0));
    }

    @Test
    void modifiedFilesMultiple() throws Exception {
        initRepo();

        writeFile("a.java", "v1");
        writeFile("b.java", "v1");
        writeFile("c.java", "v1");
        commitAll("v1");
        tag("1.0.0");

        writeFile("a.java", "v2");
        commitAll("commit a");

        writeFile("b.java", "v2");
        writeFile("c.java", "v2");
        commitAll("commit b c");

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**");

        var modified = provider.modifiedFiles().sorted().toList();
        assertEquals(3, modified.size());
    }

    @Test
    void modifiedFilesNone() throws Exception {
        initRepo();

        writeFile("src/Main.java", "x");
        commitAll("initial");
        tag("1.0.0");

        var provider = new VersionEvaluatorProvider()
            .repository(repository);

        assertTrue(provider.modifiedFiles().toList().isEmpty());
    }

    @Test
    void modifiedFilesWithMatcher() throws Exception {
        initRepo();

        writeFile("src/Main.java", "v1");
        writeFile("src/Config.yml", "v1");
        commitAll("v1");
        tag("1.0.0");

        writeFile("src/Main.java", "v2");
        writeFile("src/Config.yml", "v2");
        commitAll("v2");

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**/*.java");

        var modified = provider.modifiedFiles().toList();
        assertEquals(1, modified.size());
        assertEquals(Path.of("src", "Main.java"), modified.get(0));
    }

    @Test
    void modifiedFilesAddedRemoved() throws Exception {
        initRepo();

        writeFile("old.java", "x");
        commitAll("initial");
        tag("1.0.0");

        Files.delete(tempDir.resolve("old.java"));
        writeFile("new.java", "x");
        commitAll("swap");

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**");

        var modified = provider.modifiedFiles().sorted().toList();
        assertEquals(2, modified.size());
    }

    @Test
    void modifiedFilesRename() throws Exception {
        initRepo();

        writeFile("original.txt", "content");
        commitAll("initial");
        tag("1.0.0");

        Files.delete(tempDir.resolve("original.txt"));
        writeFile("renamed.txt", "content");
        commitAll("renamed");

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**");

        var modified = provider.modifiedFiles().sorted().toList();
        assertEquals(2, modified.size());
    }

    @Test
    void modifiedFilesHighestTag() throws Exception {
        initRepo();

        writeFile("file.txt", "v1");
        commitAll("v1");
        tag("1.0.0");

        writeFile("file.txt", "v2");
        writeFile("a.txt", "new");
        commitAll("v2");
        tag("2.0.0");

        writeFile("file.txt", "v3");
        writeFile("b.txt", "new");
        commitAll("v3");

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .matchingGlob("**");

        var modified = provider.modifiedFiles().sorted().toList();
        assertEquals(2, modified.size());
        assertFalse(modified.contains(Path.of("a.txt")));
    }

    @Test
    void modifiedFilesSubDirectory() throws Exception {
        initRepo();

        writeFile("src/main/Main.java", "v1");
        writeFile("src/test/Test.java", "v1");
        commitAll("v1");
        tag("1.0.0");

        writeFile("src/main/Main.java", "v2");
        writeFile("src/test/Test.java", "v2");
        commitAll("v2");

        var provider = new VersionEvaluatorProvider()
            .repository(repository)
            .subDirectory(Path.of("src", "test"));

        var modified = provider.modifiedFiles().toList();
        assertEquals(1, modified.size());
        assertEquals(Path.of("src", "test", "Test.java"), modified.get(0));
    }

    // --- Builder pattern & edge cases ---

    @Test
    void builderPatternReturnsThis() throws Exception {
        initRepo();

        var provider = new VersionEvaluatorProvider();

        assertSame(provider, provider.repository(repository));
        assertSame(provider, provider.matchingGlob("*.java"));
        assertSame(provider, provider.matchingRegex(".*"));
        assertSame(provider, provider.matchingAntPattern("src/**"));
        assertSame(provider, provider.subDirectory(Path.of("src")));
    }

    @Test
    void repositoryNull() {
        var provider = new VersionEvaluatorProvider();
        assertThrows(NullPointerException.class,
            () -> provider.repository(null));
    }

    @Test
    void repositoryGetter() throws Exception {
        initRepo();

        var provider = new VersionEvaluatorProvider()
            .repository(repository);

        assertSame(repository, provider.repository());
    }
}
