#!/bin/bash

OLDVERSION=$1
VERSION=$2

echo "Stating publish process for JWT Scala from $OLDVERSION to $VERSION ..."

echo "Upgrading version in files"
sed -i.tmp "s/$OLDVERSION/$VERSION/g" ./README.md
sed -i.tmp "s/version := \"$OLDVERSION\"/version := \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i.tmp "s/\"com.pauldijou\" %% \"jwt-play\" % \"$OLDVERSION\"/\"com.pauldijou\" %% \"jwt-play\" % \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i.tmp "s/version: $OLDVERSION/version: $VERSION/g" ./docs/src/site/_config.yml
sed -i.tmp "s/versions: \[/versions: \[$OLDVERSION, /g" ./docs/src/site/_config.yml

# The end
exit 0;
