/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const isOnline = require('is-online');

const CHECK_TIMER_INTERVAL = 1000 * 60;

let checkTimer;

let lastStatus = false;

function checkConnectivity() {
    isOnline(function (err, online) {
        try {
            lastStatus = online;
            logger.info(`is online: ${online}`);
            sendMessage(eventMessages.gotOnlineStatus, online);
        } catch (error) {
            logger.error(error);
        }
    });
}

function startChecker() {
    if (!checkTimer) {
        checkConnectivity();
        checkTimer = setInterval(checkConnectivity, CHECK_TIMER_INTERVAL);
    }

}

function stopChecker() {
    if (checkTimer) {
        clearInterval(checkTimer);
        checkTimer = null;
    }
}

function getCurrentStatus() {
    return lastStatus;
}

module.exports = {
    startChecker: startChecker,
    stopChecker: stopChecker,
    getCurrentStatus: getCurrentStatus,
};