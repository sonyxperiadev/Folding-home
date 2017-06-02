#!/bin/bash
#
# Licensed under the LICENSE.
# Copyright 2017, Sony Mobile Communications Inc.
#

JOBSERVER_DIRECTORY=/path/to/jobserver

NODE_BIN_DIR=/usr/bin

NODE_PATH=/usr/local/lib/node_modules

PATH=$NODE_BIN_DIR:$PATH

export NODE_PATH=$NODE_PATH

cd $JOBSERVER_DIRECTORY/scripts

node backupcloudwatch.js
