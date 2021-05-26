let GithubActions =
      https://raw.githubusercontent.com/regadas/github-actions-dhall/master/package.dhall

let List/map =
      https://prelude.dhall-lang.org/v11.1.0/List/map sha256:dd845ffb4568d40327f2a817eb42d1c6138b929ca758d50bc33112ef3c885680

let allProjects =
      [ "core"
      , "playJson"
      , "playFramework"
      , "circe"
      , "upickle"
      , "json4sNative"
      , "json4sJackson"
      , "sprayJson"
      , "argonaut"
      ]

let noScala3 =
      [ "playJson"
      , "json4sCommon"
      , "json4sJackson"
      , "json4sNative"
      , "sprayJson"
      , "playFramework"
      ]

let scala212 = ../versions/scala212 as Text

let scala213 = ../versions/scala213 as Text

let scala3 = ../versions/scala3 as Text

let scalaVersions = [ scala212, scala213, scala3 ]

let excludesScala3 =
      List/map
        Text
        { project : Text, scala : Text }
        (\(p : Text) -> { project = p, scala = scala3 })
        noScala3

let setup =
      [ GithubActions.steps.actions/checkout
      , GithubActions.Step::{
        , name = Some "Setup Java and Scala"
        , uses = Some "olafurpg/setup-scala@v10"
        , `with` = Some (toMap { java-version = "adopt@1.8" })
        }
      , GithubActions.Step::{
        , name = Some "Cache sbt"
        , uses = Some "actions/cache@v2"
        , `with` = Some
            ( toMap
                { path =
                    ''
                    ~/.sbt
                    ~/.ivy2/cache
                    ~/.coursier/cache/v1
                    ~/.cache/coursier/v1
                    ~/AppData/Local/Coursier/Cache/v1
                    ~/Library/Caches/Coursier/v1
                    ''
                , key =
                    "\${{ runner.os }}-sbt-cache-v2-\${{ hashFiles('**/*.sbt') }}-\${{ hashFiles('project/build.properties') }}"
                }
            )
        }
      ]

let lintingJob =
      { runs-on = GithubActions.types.RunsOn.ubuntu-latest
      , steps =
            setup
          # [ GithubActions.Step::{
              , name = Some "Checking code formatting"
              , run = Some "sbt formatCheck"
              }
            ]
      }

let mdocJob =
      { runs-on = GithubActions.types.RunsOn.ubuntu-latest
      , steps =
            setup
          # [ GithubActions.Step::{
              , name = Some "Check the mdoc documentation"
              , run = Some "sbt docs/mdoc"
              }
            ]
      }

let mimaJob =
      { runs-on = GithubActions.types.RunsOn.ubuntu-latest
      , strategy = Some GithubActions.Strategy::{
        , matrix = toMap { project = allProjects }
        }
      , steps =
            setup
          # [ GithubActions.Step::{
              , name = Some "Report binary issues"
              , run = Some "sbt \${{ matrix.project }}/mimaReportBinaryIssues"
              }
            ]
      }

let testsJob =
      { runs-on = GithubActions.types.RunsOn.ubuntu-latest
      , strategy.matrix
        =
        { project = allProjects
        , scala = scalaVersions
        , exclude = excludesScala3
        }
      , steps =
            setup
          # [ GithubActions.Step::{
              , name = Some "Test"
              , `if` = Some "!startsWith(matrix.scala, '2.13')"
              , run = Some
                  "sbt ++\${{ matrix.scala }} \${{ matrix.project }}/test"
              }
            , GithubActions.Step::{
              , name = Some "Test with coverage"
              , `if` = Some "startsWith(matrix.scala, '2.13')"
              , run = Some
                  "sbt ++\${{ matrix.scala }} coverage \${{ matrix.project }}/test coverageReport"
              }
            , GithubActions.Step::{
              , name = Some "Publish coverage"
              , `if` = Some "startsWith(matrix.scala, '2.13')"
              , uses = Some "codecov/codecov-action@v1"
              }
            ]
      }

in  { name = "CI"
    , on = GithubActions.On::{
      , push = Some GithubActions.Push::{ branches = Some [ "master" ] }
      , pull_request = Some GithubActions.PullRequest::{
        , branches = Some [ "master" ]
        }
      }
    , jobs =
      { linting = lintingJob, mima = mimaJob, mdoc = mdocJob, tests = testsJob }
    }
