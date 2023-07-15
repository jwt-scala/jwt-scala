#!/bin/bash

VERSION=$1

echo "Pushing to GitHub"
git add .
git commit -m "Release v$VERSION"
git tag -a v$VERSION -m "Release v$VERSION"
git push origin main
echo "Don't forget to create the release on GitHub <-----------------------------------------"

exit 0;
