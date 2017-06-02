/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const fs = require('fs');
const {ipcMain} = electron;

ipcMain.on(global.eventMessages.getLegalText, function (event) {
    fs.readFile(dirs.raw + '/Legal.txt', 'utf8', (err, data) => {
        if (!err) {
            let htmlText = data.replace(/\r?\n/g, '<br/>');
            event.sender.send(global.eventMessages.gotLegalText, htmlText);
        }
    });
});