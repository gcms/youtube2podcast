#!/bin/bash

BASEDIR=$(dirname $0)

SOURCE_JAR=$BASEDIR/build/libs/$APP_NAME.jar
if [ ! -f "$SOURCE_JAR" ]; then
  echo "File '$SOURCE_JAR' not found! Needs to build before deploying"
  exit 1;
fi

TARGET_DIR="$1"
APP_USER="$2"

if [ ! "$TARGET_DIR" ] || [ ! "$APP_USER" ]; then
  echo "Usage: $0 target_dir user_name"
  exit 1
fi


cd $BASEDIR
BASEDIR=$PWD

APP_NAME=$(basename $BASEDIR)

if [ ! "$(echo $TARGET_DIR | grep $APP_NAME)" ]; then
  TARGET_DIR=$TARGET_DIR/$APP_NAME
fi

TARGET_JAR=$TARGET_DIR/build/libs/$APP_NAME.jar

cp -rvf $BASEDIR/ $TARGET_DIR
chown -R $APP_USER:$APP_USER $TARGET_DIR

rm -f /etc/init.d/$APP_NAME
ln -s $TARGET_JAR /etc/init.d/$APP_NAME

chown $APP_USER:$APP_USER $TARGET_JAR
chmod 500 $TARGET_JAR

systemctl daemon-reload

service $APP_NAME status
service $APP_NAME restart
service $APP_NAME status
