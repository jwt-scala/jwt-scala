#!/bin/bash

OLDVERSION=$1
VERSION=$2

echo "Stating publish process for JWT Scala from $OLDVERSION to $VERSION ..."

echo "Upgrading version in files"
sed -i.tmp "s/$OLDVERSION/$VERSION/g" ./README.md
sed -i.tmp "s/$OLDVERSION/$VERSION/g" ./docs/src/main/mdoc/_install.md

rm ./README.md.tmp
rm ./docs/src/main/mdoc/_install.md.tmp

# The end
exit 0;
