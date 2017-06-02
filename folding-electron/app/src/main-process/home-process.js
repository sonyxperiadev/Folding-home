/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {BrowserWindow} = electron;
const {ipcMain} = electron;

//process event
ipcMain.on(global.eventMessages.startProcess, function (event) {
    processManager.startProcess();
});
ipcMain.on(global.eventMessages.stopProcess, function (event) {
    processManager.stopProcess(false);
});
ipcMain.on(global.eventMessages.getResearchDetails, function (event) {
    let researchDetails = global.miscData.getResearchDetails();
    event.sender.send(global.eventMessages.gotResearchDetails, researchDetails);
});
ipcMain.on(global.eventMessages.getPeopleHelpingOut, function (event) {
    let peopleHelpingOut = global.miscData.getPeopleHelpingOut();
    let number = Math.max(parseInt(peopleHelpingOut ? peopleHelpingOut : 0), 1);
    event.sender.send(global.eventMessages.gotPeopleHelpingOut, number);
});
ipcMain.on(global.eventMessages.getPlayerScore, function (event) {
    let totalTime = global.contributionTimeData.getTotalContributedTime(true);
    event.sender.send(global.eventMessages.gotPlayerScore, totalTime);
});
ipcMain.on(global.eventMessages.getOnlineStatus, function (event) {
    let online = global.connectivityChecker.getCurrentStatus();
    event.sender.send(global.eventMessages.gotOnlineStatus, online);
});

//read more event
ipcMain.on(global.eventMessages.showReadMore, function () {
    // Create the read more window.
    let readMoreWin = new BrowserWindow({
        width: 600,
        height: 600,
        parent: global.mainWindow,
        modal: true,
        show: false,
        resizable: false,
        fullscreenable: false
    });
    readMoreWin.on('closed', () => {
        readMoreWin = null;
    });
    readMoreWin.on('blur', () => {
        readMoreWin.close();
    });
    readMoreWin.loadURL(global.miscData.getResearchDetails().url);
    readMoreWin.setMenu(null);
    readMoreWin.show();
});
