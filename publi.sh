#!/bin/bash

OLDVERSION=$1
VERSION=$2

echo "Stating publish process for JWT Scala from $OLDVERSION to $VERSION ..."

if [ ! -e ./docs/src/site/api/$OLDVERSION ]; then
  echo "Wait a minute... it looks like the old version doesn't exist. You sure about what you're doing?"
  exit 1;
fi

if [ -e ./docs/src/site/api/$VERSION ]; then
  echo "Looks like this version already exists... let's stop here."
  exit 1;
fi

echo "Upgrading version in files"
sed -i "s/$OLDVERSION/$VERSION/g" ./README.md
sed -i "s/version := \"$OLDVERSION\"/version := \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i "s/\"pdi\" %% \"jwt-play\" % \"$OLDVERSION\"/\"pdi\" %% \"jwt-play\" % \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i "s/version: $OLDVERSION/version: $VERSION/g" ./docs/src/site/_config.yml
sed -i "s/versions: [/versions: [$OLDVERSION, /g" ./docs/src/site/_config.yml

echo "Pushing to GitHub"
git add .
git commit -m "Release v$VERSION"
git tag -a v$VERSION -m "Release v$VERSION"
git push origin master
echo "Don't forget to create the release on GitHub <---------------------"

# The end
exit 0;
