#!/bin/bash

APP_NAME=youtube2podcast
APP_USER=web

TARGET_DIR=/usr/web/youtube2podcast
TARGET_JAR=$TARGET_DIR/build/libs/$APP_NAME.jar

BASEDIR=$(dirname $0)
cp -rvf $BASEDIR/ $TARGET_DIR
sudo chown -R $APP_USER:$APP_USER $TARGET_DIR

cd $TARGET_DIR

sudo -u $APP_USER ./gradlew build

#cp -v ./build/libs/$APP_NAME.jar $TARGET_JAR

rm -f /etc/init.d/$APP_NAME
ln -s $TARGET_JAR /etc/init.d/$APP_NAME

chown $APP_USER:$APP_USER $TARGET_JAR
chmod 500 $TARGET_JAR

systemctl daemon-reload

service $APP_NAME status
service $APP_NAME restart
