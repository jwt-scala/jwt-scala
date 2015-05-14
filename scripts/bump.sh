#!/bin/bash

OLDVERSION=$1
VERSION=$2

echo "Stating publish process for JWT Scala from $OLDVERSION to $VERSION ..."

echo "Upgrading version in files"
sed -i "s/$OLDVERSION/$VERSION/g" ./README.md
sed -i "s/version := \"$OLDVERSION\"/version := \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i "s/\"pdi\" %% \"jwt-play\" % \"$OLDVERSION\"/\"pdi\" %% \"jwt-play\" % \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i "s/version: $OLDVERSION/version: $VERSION/g" ./docs/src/site/_config.yml
sed -i "s/versions: \[/versions: \[$OLDVERSION, /g" ./docs/src/site/_config.yml

# The end
exit 0;
