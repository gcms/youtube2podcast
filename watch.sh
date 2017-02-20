#!/bin/bash

INTERVAL=60
BASEDIR=$(dirname $0)
cd $BASEDIR

while :; do
  check=$(./checkAndDeploy.sh)
  if [ "$check" ]; then
    ./deploy.sh
  fi
  
sleep $INTERVAL
done
