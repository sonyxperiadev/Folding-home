#
# Licensed under the LICENSE.
# Copyright 2017, Sony Mobile Communications Inc.
#
echo 'Removing JobServer Service from StartUp'

service jobserver stop > /dev/null

update-rc.d -f jobserver remove

rm /etc/init.d/jobserver

CRON_ENTRIES=$(crontab -l  2> /dev/null)
EXIT_CODE=$(echo "$?")
JOBSERVER_ENTRY=$(echo "$CRON_ENTRIES" | grep -v runcloudwatchbackup.sh)

if [ $EXIT_CODE -eq '0' ]; then
    if [ "$JOBSERVER_ENTRY" ]; then
        echo "$JOBSERVER_ENTRY" | crontab -
    else
        crontab -r
    fi
fi
