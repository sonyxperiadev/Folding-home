/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {ipcRenderer} = electron;

const legalContent = document.getElementById('legal-content');

ipcRenderer.on(global.eventMessages.gotLegalText, function (event, text) {
    legalContent.innerHTML = text;
});

ipcRenderer.send(global.eventMessages.getLegalText);