#!/bin/bash

OLDVERSION=$1
VERSION=$2

echo "Stating publish process for JWT Scala from $OLDVERSION to $VERSION ..."

if [ ! -e ../jwt-scala-doc/api/$OLDVERSION ]; then
  echo "Wait a minute... it looks like the old version doesn't exist. You sure about what you're doing?"
  exit 0;
fi

if [ -e ../jwt-scala-doc/api/$VERSION ]; then
  echo "Looks like this version already exists... let's stop here."
  exit 0;
fi

echo "Upgrading version in files"
sed -i "s/$OLDVERSION/$VERSION/g" ./README.md
sed -i "s/buildVersion = \"$OLDVERSION\"/buildVersion = \"$VERSION\"/g" ./project/Build.scala
sed -i "s/version := \"$OLDVERSION\"/version := \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i "s/\"pdi\" %% \"jwt-play\" % \"$OLDVERSION\"/\"pdi\" %% \"jwt-play\" % \"$VERSION\"/g" ./examples/play-angular-standalone/build.sbt
sed -i "s/version: $OLDVERSION/version: $VERSION/g" ../jwt-scala-doc/_config.yml
sed -i "s/versions: [/versions: [$OLDVERSION, /g" ../jwt-scala-doc/_config.yml

echo "Generating ScalaDoc"
sbt doc

echo "Copying ScalaDoc to documentation website"
mkdir ../jwt-scala-doc/api/$VERSION

cp -r ./core/common/target/edge/scala-2.11/api ../jwt-scala-doc/api/$VERSION/jwt-core
cp -r ./play-json/target/edge/scala-2.11/api ../jwt-scala-doc/api/$VERSION/jwt-play-json
cp -r ./play/target/edge/scala-2.11/api ../jwt-scala-doc/api/$VERSION/jwt-play
cp ../jwt-scala-doc/api/$OLDVERSION/index.html ../jwt-scala-doc/api/$VERSION/index.html

sed -i "s/$OLDVERSION/$VERSION/g" ../jwt-scala-doc/api/$VERSION/index.html

echo "Pushing to GitHub"
git add .
git commit -m "Release v$VERSION"
git tag -a v$VERSION -m "Release v$VERSION"
git push origin master
echo "Don't forget to create the release on GitHub <---------------------"

cd ../jwt-scala-doc && git add .
cd ../jwt-scala-doc && git commit -m "Release v$VERSION"
cd ../jwt-scala-doc && git push origin gh-pages

echo "Publishing to Bintray"
sbt publish

# The end
exit 0;
