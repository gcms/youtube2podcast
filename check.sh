#!/bin/bash

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
