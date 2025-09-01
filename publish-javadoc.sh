#!/usr/bin/env bash
set -e
set -x
test -z "$(git status . --porcelain)"
mvn --batch-mode org.apache.maven.plugins:maven-javadoc-plugin:3.11.3:javadoc
GIT_HASH=$( git rev-parse HEAD )
WORKING_DIR=$(pwd)
if [ "x$GH_REMOTE" = "x" ]; then
	GH_REMOTE="git@github.com:liblouis/liblouis-java.git"
fi
GH_BRANCH=gh-pages
TMP_DIR=$( mktemp -t "$(basename "$0").XXXXXX" )
rm $TMP_DIR
git clone --branch $GH_BRANCH --depth 1 $GH_REMOTE $TMP_DIR
cd $TMP_DIR
git rm -r *
mkdir -p api
cp -r $WORKING_DIR/target/reports/apidocs/* api/
git add .
if [ "x$GH_USER_NAME" != "x" ]; then
	git config user.name "$GH_USER_NAME"
fi
if [ "x$GH_USER_EMAIL" != "x" ]; then
	git config user.email "$GH_USER_EMAIL"
fi
git commit -m "publish javadoc [ commit ${GIT_HASH} ]"
git push $GH_REMOTE $GH_BRANCH:$GH_BRANCH
cd $WORKING_DIR
rm -rf $TMP_DIR
