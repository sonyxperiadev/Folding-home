/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {ipcMain} = electron;

//process event
ipcMain.on(global.eventMessages.getLeaderBoard, function (event) {
    global.googleApis.getLeaderboards(function (err, response) {
        event.sender.send(global.eventMessages.gotLeaderBoard, err, response);
    });
});

ipcMain.on(global.eventMessages.getPlayer, function (event, playerId) {
    global.googleApis.getPlayer(playerId, function (err, response) {
        event.sender.send(global.eventMessages.gotPlayer, err, response, playerId);
    });
});