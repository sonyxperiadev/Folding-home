/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {ipcMain} = electron;
const fs = require('fs');

/*The listener that listen a json file request and send back the json file that is used to draw the contribution time charts*/
ipcMain.on(global.eventMessages.getContributionTime, function (event) {
    fs.readFile(dirs.home + '/db_report.json', 'utf8', (err, data) => {
        if (err instanceof Error && err.code === 'ENOENT') {
            throw "No such file or directory";
        }
        let contributionTimeJson = JSON.parse(data);
        event.sender.send(global.eventMessages.gotContributionTime, contributionTimeJson);
    });
});
