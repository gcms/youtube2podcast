#!/bin/bash

APP_NAME=youtube2podcast
APP_USER=web


BASEDIR=$(dirname $0)
./gradlew build

rm -f /etc/init.d/$APP_NAME
ln -s ./etc/init.d/$APP_NAME ./build/libs/$APP_NAME.jar

sudo chown $APP_USER:$APP_USER ./build/libs/$APP_NAME.jar
sudo chmod 500 ./build/libs/$APP_NAME.jar

systemctl daemon-reload

service status $APP_NAME

