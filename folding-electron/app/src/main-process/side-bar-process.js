/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {ipcMain} = electron;
const _ = require('lodash');

function showLoggedInNotification(profile){
    if (!_.isEmpty(profile)){
        let title = global.i18n.__('notification_logged_in_title');
        let message = global.i18n.__('notification_logged_in_text', profile.displayName);
        let iconUrl = profile.avatarImageUrl;
        global.notification.showNotification({title: title, message: message, iconUrl: iconUrl});
    }
}

function showSharedNotification(err) {
    if (_.isEmpty(err)){
        let title = global.i18n.__('fb_share_title');
        let message = global.i18n.__('fb_share_message');
        global.notification.showNotification({title: title, message: message});
    }
}

//login events
ipcMain.on(global.eventMessages.doLogin, function (event) {
    global.googleApis.doLogin(function (err, profile) {
        event.sender.send(global.eventMessages.gotLoginInfo, err, profile);

        showLoggedInNotification(profile);

        let contributedTime = global.contributionTimeData.getTotalContributedTime(true);
        global.sendMessage(global.eventMessages.gotPlayerScore, contributedTime);
    }, electron.BrowserWindow);
});

ipcMain.on(global.eventMessages.getLoginInfo, function (event) {
    let profile = global.miscData.getPlayerProfile();
    event.sender.send(global.eventMessages.gotLoginInfo, null, profile);

    showLoggedInNotification(profile);

    if (global.googleApis.isLogged()) {
        global.googleApis.getMe(function (err, profile) {
            let usageLimit = err && err.domain != 'usageLimits';
            if (!usageLimit) {
                event.sender.send(global.eventMessages.gotLoginInfo, err, profile);
            }
        });
    }
});


//share event
ipcMain.on(global.eventMessages.shareOnFacebook, function (event) {
    logger.debug('Share on FB requested');
    global.fbShare.checkPublishPermission(electron.BrowserWindow, function(err){
        logger.debug('Share on FB finished with error? ', (!_.isEmpty(err)));
        if (_.isEmpty(err)) {
            showSharedNotification(err);
            global.scores.unlockAchievement(global.gameIds.ACH_SHARE_ON_FACEBOOK);
        }
    });
});