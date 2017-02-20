#!/bin/bash

PROJECT=youtube2podcast
GIT_URL=http://github.com/gcms/$PROJECT.git

if [ ! "$BUILD_DIR" ]; then
  export BUILD_DIR=/tmp/$USER/build/$PROJECT
fi


UPDATED=$(./check.sh $GIT_URL $PROJECT)
if [ "$UPDATED" ]; then
  touch "$BUILD_DIR/.build"
fi

if [ -f "$BUILD_DIR/.build" ]; then
  cd "$BUILD_DIR"
  ./deploy.sh && rm -f "$BUILD_DIR/.build"
fi

exit 0
