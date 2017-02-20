#!/bin/bash

APP_USER=web
GIT_URL=http://github.com/gcms/$PROJECT.git
PROJECT=youtube2podcast

if [ ! "$BUILD_DIR" ]; then
  export BUILD_DIR=/usr/web/tmp/$PROJECT
fi


UPDATED=$(sudo -u $APP_USER./check.sh "$GIT_URL" "$PROJECT")
if [ "$UPDATED" ]; then
  touch "$BUILD_DIR/.build"
fi

if [ -f "$BUILD_DIR/.build" ]; then
  cd "$BUILD_DIR"
  sudo -u $APP_USER ./gradlew build &&
  ./deploy.sh /usr/$APP_USER $APP_USER &&
  rm -f "$BUILD_DIR/.build"
fi

exit 0
