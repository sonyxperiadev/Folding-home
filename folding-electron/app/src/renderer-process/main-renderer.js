/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {ipcRenderer} = electron;
const _ = require('lodash');

//widgets
const loginButton = document.getElementById('button-login-google');
const loginButtonText = document.getElementById('button-login-google-name');

loginButton.addEventListener('click', function () {
    ipcRenderer.send(global.eventMessages.doLogin);
});

//ipc events
ipcRenderer.on(global.eventMessages.gotLoginInfo, function (event, err, profile) {
    //TODO change the button layout
    loginButtonText.textContent = err || _.isEmpty(profile) ? global.i18n.__('login') : profile.displayName;
});

//initial state events
ipcRenderer.send(global.eventMessages.getLoginInfo);