#!/bin/bash

set -x


checkAndDeploy() {
GIT_URL="$1"
APP_USER="$2"
DESTINATION="$3"

PROJECT=$(echo $GIT_URL | sed -e 's/.*\/\([^\/]*\)\.git/\1/g')
if [ ! "$BUILD_DIR" ]; then
  BUILD_DIR=$DESTINATION/tmp/$PROJECT
fi

BASEDIR=$(dirname $0)

UPDATED=$(sudo -u $APP_USER $BASEDIR/checkout.sh "$GIT_URL" "$BUILD_DIR")
if [ "$UPDATED" ]; then
  touch "$BUILD_DIR/.build"
fi

if [ -f "$BUILD_DIR/.build" ]; then
  cd "$BUILD_DIR"
  sudo -u "$APP_USER" ./gradlew build &&
  ./deploy.sh $DESTINATION $APP_USER &&
  rm -f "$BUILD_DIR/.build"
fi

}


checkAndDeploy http://github.com/gcms/youtube2podcast.git web /usr/web

exit 0