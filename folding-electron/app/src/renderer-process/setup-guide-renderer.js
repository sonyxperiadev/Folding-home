/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {ipcRenderer} = electron;
const _ = require('lodash');

const prevButton = document.getElementById('bt-prev');
const nextButton = document.getElementById('bt-next');

const signInButton = document.getElementById('setup-guide-signin');

const indicators = document.getElementById('setup-indicators');

const page1 = document.getElementsByClassName('wizard-1')[0];
const page2 = document.getElementsByClassName('wizard-2')[0];
const page3 = document.getElementsByClassName('wizard-3')[0];

const setupReadMore = document.getElementById('setup-read-more');

const MIN_PAGE = 1;
const MAX_PAGE = 3;

let currentPage = 0;

let loggedIn = false;

//button events
signInButton.addEventListener('click', function () {
    if (!loggedIn) {
        ipcRenderer.send(global.eventMessages.doLogin);
    }
});

//button events
setupReadMore.addEventListener('click', function () {
  ipcRenderer.send(global.eventMessages.showSetupReadMore);
});

nextButton.addEventListener('click', function () {
    changePage(Math.min(currentPage + 1, MAX_PAGE));
});

prevButton.addEventListener('click', function () {
    changePage(Math.max(currentPage - 1, MIN_PAGE));
});

function getPageByNumber(number) {
    switch (number) {
        case 1:
            return page1;
        case 2:
            return page2;
        case 3:
            return page3;
        default:
            return page1;
    }
}

function setIndicators(page) {
    for (let i = 0; i < indicators.children.length; i++) {
        indicators.children[i].classList.remove('nav-active');
        if (i + 1 == page) {
            indicators.children[i].classList.add('nav-active');
        }
    }
}

function changePage(newPage) {
    if (newPage != currentPage) {
        setIndicators(newPage);

        let oldPageView = getPageByNumber(currentPage);
        let newPageView = getPageByNumber(newPage);

        oldPageView.style.display = 'none';
        newPageView.style.display = 'block';

        prevButton.style.visibility = newPage == MIN_PAGE ? 'hidden' : 'visible';
        nextButton.style.visibility = newPage == MAX_PAGE ? 'hidden' : 'visible';

        currentPage = newPage;
    }
}

//ipc events
ipcRenderer.on(global.eventMessages.gotLoginInfo, function (event, err, profile) {
    loggedIn = !err && !_.isEmpty(profile);
    signInButton.textContent = loggedIn ? profile.displayName : global.i18n.__('sign_in_ggs');
});

//initial events
changePage(1);
