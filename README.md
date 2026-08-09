# JDrupes Git Versioning

[![Maven Repository Version](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcodeberg.org%2Fapi%2Fpackages%2FJDrupes%2Fmaven%2Forg%2Fjdrupes.gitversioning%2Fapi%2Fmaven-metadata.xml&strategy=releaseProperty)](https://codeberg.org/JDrupes/-/packages/maven/org.jdrupes.gitversioning:api/versions)
[![Maven Repository Version](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcodeberg.org%2Fapi%2Fpackages%2FJDrupes%2Fmaven%2Forg%2Fjdrupes.gitversioning%2Fcore%2Fmaven-metadata.xml&strategy=releaseProperty)](https://codeberg.org/JDrupes/-/packages/maven/org.jdrupes.gitversioning:core/versions)

A small library for deriving a project's version from tags in its 
Git repository.

Starting with version 0.2.0, this is no longer distributed via Maven Central.
Rather, it is available from the
[JDrupes Maven registry on Codeberg](https://codeberg.org/api/packages/JDrupes/maven/).

Using gradle, add the registry with:

```groovy
repositories {
    maven {
        url 'https://codeberg.org/api/packages/JDrupes/maven'
    }
}
```

Documentation is available 
[here](https://mnlipp.github.io/JDrupes-GitVersioning/).
