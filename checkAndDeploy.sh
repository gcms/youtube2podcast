#!/bin/bash

set -x



check() {
  GIT_URL="$1"
  PROJECT="$2"
  BUILD_DIR="$3"
  
  git config --global user.email "$USER@localhost"
  git config --global user.name "$USER"
  
  if [ ! "$GIT_URL" ] || [ ! "$PROJECT" ] || [ ! "$BUILD_DIR" ]; then
    echo Usage: $0 GIT_URL PROJECT BUILD_DIR
    exit 1
  fi
  
  
  if [ ! -d "$BUILD_DIR" ]; then
    mkdir -p "$BUILD_DIR"
    cd $BUILD_DIR/..
    rmdir $BUILD_DIR
    git clone $GIT_URL
    cd $BUILD_DIR
  
    echo Updated
    exit 0
  fi
  
  cd $BUILD_DIR
  GIT_PULL=$(git pull | egrep "[A-Za-z0-9]+\.\.[A-Z0-9a-z]+")
  if [ "$GIT_PULL" ] && [ "$?" == "0"  ]; then
    echo Updated: $GIT_PULL
  fi

}


APP_USER=web
PROJECT=youtube2podcast
GIT_URL=http://github.com/gcms/$PROJECT.git

if [ ! "$BUILD_DIR" ]; then
  BUILD_DIR=/usr/web/tmp/$PROJECT
fi

BASEDIR=$(dirname $0)

UPDATED=$(sudo -u $APP_USER check "$GIT_URL" "$PROJECT" "$BUILD_DIR")
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