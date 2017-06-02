/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
function getMessage(action, content) {
    return {
        action: action, content: content
    };
}
function getKey() {
    let os = require('os');
    return getMessage('key', {
        key: 'clientkey',
        uuid: miscData.getUUID(),
        app_version: require('electron').app.getVersion().replace(/\./g, ''),
        platform: os.platform(),
        os_version: os.release(),
        arch: process.arch
    });
}

function getKill(immediately) {
    return getMessage('kill', immediately ? 'SIGKILL' : 'SIGTERM');
}

function getResume() {
    return getMessage('continue', {});
}

module.exports = {
    getKey: getKey,
    getKill: getKill,
    getResume: getResume
};