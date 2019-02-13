#!/bin/bash

VERSION=$1

# Remove current scaladoc
if [ -e ./docs/src/site/api/latest ]; then
  rm -r ./docs/src/site/api/latest
fi

if [ -e ./docs/src/site/api/$VERSION ]; then
  rm -r ./docs/src/site/api/$VERSION
fi

# Recreate dirs
mkdir ./docs/src/site/api/latest
mkdir ./docs/src/site/api/$VERSION

# Copy scaladoc
cp -r ./core/target/scala-2.12/api ./docs/src/site/api/latest/jwt-core
cp -r ./json/play-json/target/scala-2.12/api ./docs/src/site/api/latest/jwt-play-json
cp -r ./json/json4s-native/target/scala-2.12/api ./docs/src/site/api/latest/jwt-json4s
cp -r ./json/spray-json/target/scala-2.12/api ./docs/src/site/api/latest/jwt-spray-json
cp -r ./json/circe/target/scala-2.12/api ./docs/src/site/api/latest/jwt-circe
cp -r ./json/upickle/target/scala-2.12/api ./docs/src/site/api/latest/jwt-upickle
cp -r ./json/argonaut/target/scala-2.12/api ./docs/src/site/api/latest/jwt-argonaut
cp -r ./play/target/scala-2.12/api ./docs/src/site/api/latest/jwt-play
cp ./docs/src/site/_templates/index_api.html ./docs/src/site/api/latest/index.html
sed -i.tmp "s/placeholderVersion/$VERSION/g" ./docs/src/site/api/latest/index.html

cp -r ./core/target/scala-2.12/api ./docs/src/site/api/$VERSION/jwt-core
cp -r ./json/play-json/target/scala-2.12/api ./docs/src/site/api/$VERSION/jwt-play-json
cp -r ./json/json4s-native/target/scala-2.12/api ./docs/src/site/api/$VERSION/jwt-json4s
cp -r ./json/spray-json/target/scala-2.12/api ./docs/src/site/api/$VERSION/jwt-spray-json
cp -r ./json/circe/target/scala-2.12/api ./docs/src/site/api/$VERSION/jwt-circe
cp -r ./json/upickle/target/scala-2.12/api ./docs/src/site/api/$VERSION/jwt-upickle
cp -r ./json/argonaut/target/scala-2.12/api ./docs/src/site/api/$VERSION/jwt-argonaut
cp -r ./play/target/scala-2.12/api ./docs/src/site/api/$VERSION/jwt-play
cp ./docs/src/site/_templates/index_api.html ./docs/src/site/api/$VERSION/index.html
sed -i.tmp "s/placeholderVersion/$VERSION/g" ./docs/src/site/api/$VERSION/index.html
