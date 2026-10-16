## Contributor Guide

For any bug, new feature, or documentation improvement,
the best way to start a conversation is by creating a new issue on github.

You're welcome to submit PRs right away without creating a ticket (issue) first, but be aware that there
is no guarantee your PR is going to be merged so your work might be for nothing.

## Prerequisites

Use JDK 17 or newer and an sbt runner. The build pins sbt 2.0.10 in `project/build.properties`.
Published JVM artifacts require Java 17 or newer.

## Tests

Continuous integration will run tests on your PR, needless to say it has to be green to be merged :).

To run the tests locally:

- Run all tests with `sbt testAll`.
- Test both Scala versions with `sbt '++2.13.18; testAll; ++3.3.7; testAll'`.
- Run all tests in one project with `sbt circeJVM/Test/testFull`.
- Run only new, changed, or previously failing tests with `sbt circeJVM/test`.

`testAll` and CI use `testFull` so no tests are skipped by sbt 2's incremental test selection.
Pass multiple sbt commands in one quoted argument, separated by semicolons.

## Formatting

The project using [Scalafmt](https://scalameta.org/scalafmt/) for formatting.

Before submitting your PR you can format the code by running `sbt format`, but the best way is to configure your IDE/Editor
to pick up the Scalafmt config from the repo and format it automatically. It is supported at least by IntelliJ and VSCode.

## Documentation

To have a locally running doc website and test your documentation changes:

- Run `sbt 'docs/makeSite; docs/previewSite'`.
- Open the URL printed by the preview server.

The generated site, including unified Scaladoc, is written to `target/site`.

## Publishing

Create a release with a new tag on GitHub, and a new version with automatically be published to Sonatype by Github Actions with the corresponding version number.

Documentation (site + Scaladoc) can be published with:

- `sbt publish-doc`
