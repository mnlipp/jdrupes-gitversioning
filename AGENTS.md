# JDrupes Git Versioning — Agent Guide

## Build System

- **No Maven/Gradle.** This project uses [JDrupes Builder](https://github.com/mnlipp/jdrupes-builder), invoked via `./jdbld`.
- All build commands run from the repo root:
  - `./jdbld build` — compile both modules, produce JARs in `api/build/libs/` and `core/build/libs/`
  - `./jdbld javadoc` — generate Javadoc (output in `core/build/doc/`)
  - `./jdbld deploy` — deploy artifacts to the Codeberg Maven registry
- The builder requires Java 25. The `javaHome` in `.jdbld.properties` may need updating on a new machine. Alternatively set `JAVA_HOME` before invoking `./jdbld`.
- The `jdbld` script downloads the builder JAR to `~/.jdbld/versions/` on first run.

## Module Structure

| Directory | Artifact | Role |
|-----------|----------|------|
| `api/` | `org.jdrupes.gitversioning:api` | Public API — SPI interfaces (`VersionEvaluator`, `VersionEvaluatorProvider`, `TagFilter`, `TagProcessor`). Sources in `api/src/`. |
| `core/` | `org.jdrupes.gitversioning:core` | Implementation. Depends on `api`. Sources in `core/src/`, resources (SPI descriptor) in `core/resources/`. |

Build order is `api` → `core` (core depends on the api JAR).

## Dependencies

- **JGit** (org.eclipse.jgit) — Git repository access
- **semver4j** — Semantic version parsing
- **commons-codec**, **JavaEWAH** — pulled transitively by JGit
- **slf4j-api** — logging facade

Maven coordinates are in the `.classpath` files (not a pom.xml). The builder resolves them from `~/.m2/repository`.

## Code Quality Tools

Configured via Eclipse plugin settings (not CLI):
- **Checkstyle**: `core/.checkstyle`, `api/.checkstyle` — rules from JDrupes Builder's `checkstyle.xml`
- **PMD**: `core/.eclipse-pmd`, `api/.eclipse-pmd` — rules from `ruleset.xml`
- **jautodoc**: `_jdbld/.settings/net.sf.jautodoc.prefs` — auto-generates Javadoc for public API methods using getter/setter name conventions

These are primarily Eclipse IDE integrations; no standalone CLI equivalent exists.

## Formatting

Per `.editorconfig`: 4-space indent for Java, UTF-8, final newline, trimmed trailing whitespace.

## Generated / Ignored Artifacts

- `**/build/`, `**/bin/` — compiled classes and JARs
- `_jdbld/` — builder workspace (most contents ignored except `.project`, `.classpath`, `.settings/`)
- `jdbld.log` — last build log

## SPI Registration

The `core` module registers `VersionEvaluatorProvider` via Java Services:
`core/resources/META-INF/services/org.jdrupes.gitversioning.api.VersionEvaluatorProvider`

## CI / Deployment

- **GitHub Pages**: `.github/workflows/jekyll.yml` runs on push to `main`. Steps: checkout, build javadoc via `./jdbld javadoc`, build Jekyll site in `webpages/`, deploy.
- CI requires `graphviz` and `xmlstarlet` system packages.
- Maven artifacts are published to `https://codeberg.org/api/packages/JDrupes/maven/` (not Maven Central).
