#!/bin/bash

INTERVAL=60
BASEDIR=$(dirname $0)
cd $BASEDIR

while :; do
  check=$(./check.sh)
  if [ "$check" ]; then
    ./deploy.sh
  fi
  
sleep $INTERVAL
done
