#!/bin/bash

set -x

APP_USER=web
PROJECT=youtube2podcast
GIT_URL=http://github.com/gcms/$PROJECT.git

if [ ! "$BUILD_DIR" ]; then
  BUILD_DIR=/usr/web/tmp/$PROJECT
fi

BASEDIR=$(dirname $0)

UPDATED=$(sudo -u $APP_USER $BASEDIR/check.sh "$GIT_URL" "$PROJECT" "$BUILD_DIR")
if [ "$UPDATED" ]; then
  touch "$BUILD_DIR/.build"
fi

if [ -f "$BUILD_DIR/.build" ]; then
  cd "$BUILD_DIR"
  sudo -u "$APP_USER" ./gradlew build &&
  ./deploy.sh /usr/$APP_USER $APP_USER &&
  rm -f "$BUILD_DIR/.build"
fi

exit 0
