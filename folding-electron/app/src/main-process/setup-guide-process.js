/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {BrowserWindow} = electron;
const {ipcMain} = electron;
const {shell} = require('electron');



//read more event
ipcMain.on(eventMessages.showSetupReadMore, function () {

    // Create the read more window.
    let readMoreWin = new BrowserWindow({
        width: 500,
        height: 500,
        useContentSize: true,
        parent: global.mainWindow,
        modal: true,
        show: false,
        resizable: false,
        fullscreenable: false
    });
    readMoreWin.on('closed', () => {
        readMoreWin = null;
    });

    var handleRedirect = (e, url) => {
      if(url != readMoreWin.webContents.getURL()) {
        e.preventDefault();
        require('electron').shell.openExternal(url);
      }
    };

    readMoreWin.webContents.on('new-window', handleRedirect);

    readMoreWin.on('blur', () => {
        readMoreWin.close();
    });
    readMoreWin.loadURL(`file://${global.dirs.views}/setup-read-more.html`);
    readMoreWin.setMenu(null);
    readMoreWin.show();
});
