#
# Licensed under the LICENSE.
# Copyright 2017, Sony Mobile Communications Inc.
#
echo 'Checking dependencies'

redis-server --version > /dev/null 2>&1

if [ $? -eq '0' ];
    then
        echo 'Redis server ok.';
    else
        echo 'Redis server not installed.';
        exit 1;
fi
