#
# Licensed under the LICENSE.
# Copyright 2017, Sony Mobile Communications Inc.
#
echo 'Copying init script.'

FOREVER_PATH=$(which forever)
JOBSERVER_DIR=$(pwd)

sed -i -e"s,JOBSERVER_DIRECTORY=.*,JOBSERVER_DIRECTORY=$JOBSERVER_DIR,g" scripts/runcloudwatchbackup.sh

CRON_ENTRIES=$(crontab -l  2> /dev/null)
EXIT_CODE=$(echo "$?")
JOBSERVER_ENTRY=$(echo "$CRON_ENTRIES" | grep runcloudwatchbackup.sh)

if [ $EXIT_CODE -eq '0' ]; then
    if [ "$JOBSERVER_ENTRY" ]; then
        echo 'Cronjob already set'
    else
        crontab -l | sed "$ a\0 1 * * Sun $JOBSERVER_DIR/scripts/runcloudwatchbackup.sh >> /var/log/backupcloud.log 2>&1" | crontab -
        echo "Cronjob saved"
    fi
elif [ $EXIT_CODE -eq '1' ]; then
    echo "0 1 * * Sun $JOBSERVER_DIR/scripts/runcloudwatchbackup.sh >> /var/log/backupcloud.log 2>&1" | crontab -
    echo "Cronjob saved"
else
        echo 'Cron command failed'
        echo 'BACKUP WONT BE SAVED'
fi

cp scripts/logrotatejobserver /etc/logrotate.d/jobserver

if [ $FOREVER_PATH ]; then
    set -e

    sed -i -e"s,APPLICATION_DIRECTORY=.*,APPLICATION_DIRECTORY=$JOBSERVER_DIR,g" scripts/jobserver

    cp scripts/jobserver /etc/init.d/jobserver

    update-rc.d jobserver defaults

    service jobserver start > /dev/null

    echo "Job Server installed and running"
    set +e
else
    echo "\n\nForever not installed globally. You need to run the server manually\n\n"
fi
