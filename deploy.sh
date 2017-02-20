#!/bin/bash

APP_NAME=youtube2podcast
APP_USER=web

TARGET_JAR=/usr/web/$APP_NAME.jar

BASEDIR=$(dirname $0)
cd $BASEDIR
sudo -u pi ./gradlew build

cp -v ./build/libs/$APP_NAME.jar $TARGET_JAR

rm -f /etc/init.d/$APP_NAME
ln -s $TARGET_JAR /etc/init.d/$APP_NAME

chown $APP_USER:$APP_USER $TARGET_JAR
chmod 500 $TARGET_JAR

systemctl daemon-reload

service $APP_NAME status

