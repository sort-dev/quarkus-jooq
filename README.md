# Quarkus JOOQ Extension

An extension to Quarkus exposing JOOQ and connecting it to Quarkus data sources.

This code is based off of the plugin: https://github.com/quarkiverse/quarkus-jooq
Which has flaws that are not yet being addressed by the contributors (see [issue list](https://github.com/quarkiverse/quarkus-jooq/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc))

Releases are available from JitPack: https://jitpack.io/#sort-dev/quarkus-jooq

Add maven repo to your build:
```
"https://jitpack.io"
```

Latest release dependency:
```
com.github.sort-dev:quarkus-jooq:0.1.5
```
This works with JOOQ 3.15.5 and Quarkus 2.9.0-FINAL, those before the `javax.*` to `jakarta.*` changes.

!! Work in Progress !!