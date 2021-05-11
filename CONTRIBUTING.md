## Contributor Guide

For any bug, new feature, or documentation improvement,
the best way to start a conversation is by creating a new issue on github.

You're welcome to submit PRs right away without creating a ticket (issue) first, but be aware that there
is no guarantee your PR is going to be merged so your work might be for nothing.

## Tests

Continuous integration will run tests on your PR, needless to say it has to be green to be merged :).

To run the tests locally:
- Run all tests with `sbt testAll` (if `java.lang.LinkageError`, just re-run the command)
- Run a single project test, for example `sbt circeProject/test`

If you need to change the Github Action workflow, don't edit the file directly as it's generated from
a `dhall` file. Edit `dhall/tests.dhall` instead, install [Dhall](https://dhall-lang.org/) on your computer
and run `scripts/dhall.sh` to update the workflow file.

## Formatting

The project using [Scalafmt](https://scalameta.org/scalafmt/) for formatting.

Before submitting your PR you can format the code by running `sbt format`, but the best way is to configure your IDE/Editor
to pick up the Scalafmt config from the repo and format it automatically. It is supported at least by IntelliJ and VSCode.

## Documentation

To have a locally running doc website and test your documentation changes:
  - `sbt ~docs/makeMicrosite`
  - `cd docs/target/site`
  - `jekyll serve -b /jwt-scala`
  - Go to [http://localhost:4000/jwt-scala/](http://localhost:4000/jwt-scala/)

## Publishing

This is only for maintainers with write access (just one for now!) but a new version can be published to Sonatype:
- Update version numbers in `build.sbt`
- `sbt releaseAll`

Documentation (microsite + scaladoc) can be published with:
- `sbt publish-doc`
