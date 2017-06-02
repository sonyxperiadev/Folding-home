/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {ipcMain} = electron;

//process event
ipcMain.on(global.eventMessages.getAchievements, function (event) {
    global.googleApis.getAchievements(function (err, response) {
        event.sender.send(global.eventMessages.gotAchievements, err, response);
    });
});
ipcMain.on(global.eventMessages.getAchievementsDefinition, function (event) {
    global.googleApis.getAchievementsDefinition(function (err, response) {
        event.sender.send(global.eventMessages.gotAchievementsDefinition, err, response);
    });
});