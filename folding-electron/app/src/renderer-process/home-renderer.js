/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const _ = require('lodash');
const {ipcRenderer} = electron;

const PROCESS_STATUS = {running: 'running', notRunning: 'notRunning'};
const RESEARCH_BAR_STATUS = {closed: 'closed', opened: 'opened'};

//widgets
const startStopButton = document.getElementById('button-start-stop');
const processingSpam = document.getElementById('processing-spam');
const readMore = document.getElementById('read-more');
const rowResearchTitle = document.getElementById('row-research-title');
const rowFullDesc = document.getElementById('row-full-desc');
const rowMainResearchTitle = document.getElementById('home-main-row-research-title');
const rowMainPeopleHelpingOut = document.getElementById('home-main-row-people-helping-out');
const fbShareButton = document.getElementById('fb-share');

//set initial state
rowFullDesc.style.display = 'none';
rowResearchTitle.status = RESEARCH_BAR_STATUS.closed;
startStopButton.status = PROCESS_STATUS.notRunning;

let onlineStatus = false;


function setContributionStatus() {
    let buttonSrc = '';
    let text = '';
    if (startStopButton.status == PROCESS_STATUS.notRunning) {
        buttonSrc = 'ic_power_off.png';
        text = 'disabled';
    } else if (!onlineStatus) {
        buttonSrc = 'ic_stand_by.png';
        text = 'no_internet_connection';
    } else {
        buttonSrc = 'ic_power_on.png';
        text = 'helping_out';
    }

    startStopButton.src = dirs.assets + '/img/' + buttonSrc;
    processingSpam.innerText = i18n.__(text);
}


//button listeners
rowResearchTitle.addEventListener('click', function () {
    if (rowResearchTitle.status == RESEARCH_BAR_STATUS.closed) {
        rowResearchTitle.status = RESEARCH_BAR_STATUS.opened;
        rowFullDesc.style.display = 'block';
    } else {
        rowResearchTitle.status = RESEARCH_BAR_STATUS.closed;
        rowFullDesc.style.display = 'none';
    }
});

startStopButton.addEventListener('click', function () {
    startStopButton.status = startStopButton.status == PROCESS_STATUS.running ? PROCESS_STATUS.notRunning : PROCESS_STATUS.running;
    ipcRenderer.send(startStopButton.status == PROCESS_STATUS.running ? global.eventMessages.startProcess : global.eventMessages.stopProcess);
    setContributionStatus();
});

readMore.addEventListener('click', function () {
    ipcRenderer.send(global.eventMessages.showReadMore);
});

fbShareButton.addEventListener('click', function () {
    ipcRenderer.send(global.eventMessages.shareOnFacebook);
});


//IPC messages
ipcRenderer.on(global.eventMessages.gotResearchDetails, function (event, researchDetails) {
    let hasResearchDetails = !_.isEmpty(researchDetails);

    rowMainResearchTitle.style.display = hasResearchDetails ? 'block' : 'none';
    rowFullDesc.style.display = 'none';
    rowMainPeopleHelpingOut.style.display = hasResearchDetails ? 'block' : 'none';

    if (hasResearchDetails) {
        document.getElementById('desc-title').textContent = researchDetails.title;
        document.getElementById('full-desc-title').textContent = researchDetails.description;
    }
});

ipcRenderer.on(global.eventMessages.gotPeopleHelpingOut, function (event, value) {
    document.getElementById('people-helping').textContent = value;
});

ipcRenderer.on(global.eventMessages.gotPlayerScore, function (event, value) {
    document.getElementById('contributed-time').textContent = value;
});

ipcRenderer.on(global.eventMessages.gotOnlineStatus, function (event, online) {
    onlineStatus = online;
    //only changes the button status if it is running
    setContributionStatus();
});

ipcRenderer.send(global.eventMessages.getResearchDetails);
ipcRenderer.send(global.eventMessages.getPeopleHelpingOut);
ipcRenderer.send(global.eventMessages.getPlayerScore);
ipcRenderer.send(global.eventMessages.getOnlineStatus);