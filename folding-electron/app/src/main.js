/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

global.isAsar = function () {
    return process.mainModule.filename.indexOf('app.asar') !== -1;
};

//node modules used only in this file
const electron = require('electron');
const glob = require('glob');
const path = require('path');
const os = require('os');
const app = electron.app;
const {BrowserWindow} = electron;

require('./utils/global-config.js');

global.requireLocalPackage = function (packageName) {
    let fullPath = path.join(global.dirs.source, packageName);
    return require(fullPath);
};

app.setPath("appData", path.join(global.dirs.home, 'usrdata/appData'));
app.setPath("userData", path.join(global.dirs.home, 'usrdata/userData'));

//app modules
//set global to modules 'statically' used across the app
//set const to modules used locally only

//storage modules
global.database = requireLocalPackage('/storage/database.js');
global.miscData = requireLocalPackage('/storage/misc-data.js');
global.googleCredentialsData = requireLocalPackage('/storage/google-credentials.js');
global.contributionTimeData = requireLocalPackage('/storage/contribution-time.js');
global.facebookCredentialsData = requireLocalPackage('/storage/facebook-credentials.js');

//util modules
global.logger = requireLocalPackage('/utils/logger.js');
global.i18n = requireLocalPackage('/utils/i18n.js');
global.formatter = requireLocalPackage('/utils/formatter.js');

//GGS modules
global.googleApis = requireLocalPackage('/gamification/google-apis.js');
global.scores = requireLocalPackage('/gamification/scores.js');

global.fbShare = requireLocalPackage('/social/facebook-share.js');

global.processManager = requireLocalPackage('/environment/process-manager.js');
global.notification = requireLocalPackage('/utils/notification.js');
global.fileDownloader = requireLocalPackage('/utils/file-downloader.js');
global.connectivityChecker = requireLocalPackage('/utils/connectivity-checker.js');


global.sendMessage = function (eventMessage, object) {
    mainWindow.webContents.send(eventMessage, object);
};

global.mainWindow = null;

logger.info('>>>>>>> Starting new session');
logger.info('System info platform: ',  os.platform(), ' type: ', os.type(), ' arch: ', os.arch(), ' release: ', os.release(), ' cpus: ', os.cpus());
logger.info('Dirs: ', dirs);
logger.info('<<<<<<<');

function initialize() {
    var shouldQuit = makeSingleInstance();
    if (shouldQuit) return app.quit();

    function createWindow() {
        // Create the browser window.

        connectivityChecker.startChecker();
        let session = require('electron').session.fromPartition('', {cache: false});
        mainWindow = new BrowserWindow({
            width: 900,
            height: 750,
            resizable: false,
            webPreferences: {session: session},
            fullscreenable: false
        });

        // and load the index.html of the app.
        mainWindow.loadURL(`file://${global.dirs.views}/index.html`);
        //mainWindow.setMenu(null);

        // Emitted when the window is closed.
        mainWindow.on('closed', function () {
            connectivityChecker.stopChecker();
            // Dereference the window object, usually you would store windows
            // in an array if your app supports multi windows, this is the time
            // when you should delete the corresponding element.
            mainWindow = null;
        });
    }

    // This method will be called when Electron has finished
    // initialization and is ready to create browser windows.
    // Some APIs can only be used after this event occurs.
    app.on('ready', createWindow);

    // Quit when all windows are closed.
    app.on('window-all-closed', function () {
        // On OS X it is common for applications and their menu bar
        // to stay active until the user quits explicitly with Cmd + Q
        // if (process.platform !== 'darwin') {
        app.quit();
        // }
    });

    app.on('activate', function () {
        // On OS X it's common to re-create a window in the app when the
        // dock icon is clicked and there are no other windows open.
        if (mainWindow === null) {
            createWindow();
        }
    });
}

function makeSingleInstance() {
    if (process.mas) return false;

    return app.makeSingleInstance(function () {
        if (mainWindow) {
            if (mainWindow.isMinimized()) mainWindow.restore();
            mainWindow.focus();
        }
    });
}


// Require each JS file in the main-process dir
function loadMainProcesses() {
    var files = glob.sync(path.join(global.dirs.source, 'main-process/**/*.js'));
    files.forEach(function (file) {
        require(file);
    });
}

initialize();
loadMainProcesses();

// In this file you can include the rest of your app's specific main-process process
// code. You can also put them in separate files and require them here.
