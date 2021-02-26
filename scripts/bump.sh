#!/bin/bash

OLDVERSION=$1
VERSION=$2

echo "Stating publish process for JWT Scala from $OLDVERSION to $VERSION ..."

echo "Upgrading version in files"
sed -i.tmp "s/$OLDVERSION/$VERSION/g" ./README.md
sed -i.tmp "s/version := \"$OLDVERSION\"/version := \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i.tmp "s/\"com.github.jwt-scala\" %% \"jwt-play\" % \"$OLDVERSION\"/\"com.github.jwt-scala\" %% \"jwt-play\" % \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i.tmp "s/version: $OLDVERSION/version: $VERSION/g" ./docs/src/site/_config.yml
sed -i.tmp "s/versions: \[/versions: \[$OLDVERSION, /g" ./docs/src/site/_config.yml

rm ./README.md.tmp
rm ./examples/play-angular-standalone/build.sbt.tmp
rm ./docs/src/site/_config.yml.tmp

# The end
exit 0;
