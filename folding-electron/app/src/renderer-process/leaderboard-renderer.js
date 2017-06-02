/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const _ = require('lodash');
const {ipcRenderer} = electron;

const leaderboardErrorContent = document.getElementById('leaderboard-error-content');

const leaderboardSection = document.getElementById('leaderboard-section');
const leaderboardMenuItem = document.getElementById('leaderboards-menu-item');

const leaderboardListPodium = document.getElementById('leaderboard-list-podium');
const leaderboardListNormal = document.getElementById('leaderboard-list-normal');

let leaderboardList;

function getElementIdByPlayerId(playerId) {
    return 'leaderboard-player-id' + playerId;
}

function setPlayerAvatar(player, playerId) {
    if (!_.isEmpty(player)) {
        let element = document.getElementById(getElementIdByPlayerId(playerId));
        if (element) {
            let imageSource = player.avatarImageUrl ? player.avatarImageUrl : player.bannerUrlLandscape;
            element.getElementsByTagName('img')[0].src = imageSource;
        }
    }
}

function checkErrorAndShowMessage(err){
    let showError = !_.isEmpty(err);

    leaderboardSection.style.display = showError ? 'none' : 'block';
    leaderboardErrorContent.style.display = showError ? 'block' : 'none';

    if (showError) {
        setError(err);
    }
    return showError;
}

function clearContainers() {
    while (leaderboardListPodium.hasChildNodes()) {
        leaderboardListPodium.removeChild(leaderboardListPodium.lastChild);
    }
    while (leaderboardListNormal.hasChildNodes()) {
        leaderboardListNormal.removeChild(leaderboardListNormal.lastChild);
    }
    while (leaderboardErrorContent.hasChildNodes()) {
        leaderboardErrorContent.removeChild(leaderboardErrorContent.lastChild);
    }
}


function requestLeaderboards() {
    ipcRenderer.send(global.eventMessages.getLeaderBoard);
}

leaderboardMenuItem.addEventListener('click', function () {
    requestLeaderboards();
});

//ipc events
ipcRenderer.on(global.eventMessages.gotLoginInfo, function (event, err, profile) {
    loggedIn = !err && !_.isEmpty(profile);
    if (loggedIn) {
        requestLeaderboards();
    } else {
        leaderboardList = undefined;
        clearContainers();
        checkErrorAndShowMessage(err);
    }
});

ipcRenderer.on(global.eventMessages.gotLeaderBoard, function (event, err, leaderboard) {
    if (!checkErrorAndShowMessage(err) && !_.isEmpty(leaderboard) && !_.isEqual(leaderboardList, leaderboard)) {
        leaderboardList = leaderboard;
        setLeaderBoards(leaderboard);
    }
});

ipcRenderer.on(global.eventMessages.gotPlayer, function (event, err, player, playerId) {
    if (!_.isEmpty(player)) {
        setPlayerAvatar(player, playerId);
    }
});

function setLeaderBoards(leaderboard) {
    clearContainers();
    global.inflateHtml({file: 'leaderboard-item.html'}, function (html) {
        if (html) {
            for (let i = 0; i < leaderboard.items.length; i++) {
                setPlayerScore(html, i, leaderboard.items[i]);
            }
        }
    });
}

function setPlayerScore(elementData, position, score) {
    //console.log(`position: ${position} score: ${score}`);
    let liElement = document.createElement('li');
    liElement.innerHTML = elementData;
    liElement.getElementsByTagName('h2')[0].textContent = score.player.displayName;
    liElement.getElementsByTagName('h3')[0].textContent = score.player.experienceInfo.currentLevel.level;
    liElement.getElementsByTagName('h3')[1].textContent = score.formattedScore;

    liElement.id = getElementIdByPlayerId(score.player.playerId);

    if (position === 0) {
        liElement.class = 'first';
    } else if (position == 1) {
        liElement.class = 'second';
    } else if (position == 2) {
        liElement.class = 'third';
    }

    //the podium order is 2 _ 1 _ 3
    if (position == 1) {
        leaderboardListPodium.insertBefore(liElement, leaderboardListPodium.childNodes[0]);
    } else if (position <= 2) {
        leaderboardListPodium.appendChild(liElement);
    } else {
        leaderboardListNormal.appendChild(liElement);
    }

    //switch the comment from the 2 lines below if you want to fetch the player's data again
    setPlayerAvatar(score.player, score.player.playerId);
    //ipcRenderer.send(global.eventMessages.getPlayer, score.player.playerId);
}

function setError(err) {
    global.inflateHtml({file: 'error.html', parent: leaderboardErrorContent}, function () {
        let messageKey = '';
        let errorMessageElement = leaderboardErrorContent.getElementsByClassName('error-text')[0];

        if (err.customErrorCode == global.errorCodes.notLoggedIn) {
            messageKey = 'login_to_view_leaderboards';
        } else if (err.customErrorCode == global.errorCodes.noInternet) {
            messageKey = 'no_internet';
        } else {
            messageKey = 'there_was_an_error';
        }

        if (!_.isEmpty(messageKey)) {
            errorMessageElement.textContent = global.i18n.__(messageKey);
        }
    });
}