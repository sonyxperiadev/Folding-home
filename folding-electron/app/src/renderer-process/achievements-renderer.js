/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const _ = require('lodash');
const {ipcRenderer} = electron;

const achievementErrorContent = document.getElementById('achievement-error-content');

const achievementsMenuItem = document.getElementById('achievements-menu-item');
const achievementsContainer = document.getElementById('achievements-container');

let achievementsDefinition;
let achievementsList;

function getElementIdByAchievementId(playerId) {
    return 'achievement-id' + playerId;
}

function checkErrorAndShowMessage(err){
    let showError = !_.isEmpty(err);

    achievementsContainer.style.display = showError ? 'none' : 'block';
    achievementErrorContent.style.display = showError ? 'block' : 'none';
    if (showError) {
        setError(err);
    }
    return showError;
}

function clearContainers() {
    while (achievementsContainer.hasChildNodes()) {
        achievementsContainer.removeChild(achievementsContainer.lastChild);
    }
    while (achievementErrorContent.hasChildNodes()) {
        achievementErrorContent.removeChild(achievementErrorContent.lastChild);
    }
}

function requestAchievements() {
    if (_.isEmpty(achievementsDefinition)) {
        ipcRenderer.send(global.eventMessages.getAchievementsDefinition);
    } else {
        ipcRenderer.send(global.eventMessages.getAchievements);
    }
}

achievementsMenuItem.addEventListener('click', function () {
    requestAchievements();
});

//ipc events
ipcRenderer.on(global.eventMessages.gotLoginInfo, function (event, err, profile) {
    loggedIn = !err && !_.isEmpty(profile);
    if (loggedIn) {
        requestAchievements();
    } else {
        achievementsDefinition = undefined;
        achievementsList = undefined;
        clearContainers();
        checkErrorAndShowMessage(err);
    }
});

ipcRenderer.on(global.eventMessages.gotAchievementsDefinition, function (event, err, achievements) {
    if (!checkErrorAndShowMessage(err) && !_.isEmpty(achievements)) {
        achievementsDefinition = achievements;
        ipcRenderer.send(global.eventMessages.getAchievements);
    }
});

ipcRenderer.on(global.eventMessages.gotAchievements, function (event, err, achievements) {
    if (!checkErrorAndShowMessage(err) && !_.isEmpty(achievements) && !_.isEqual(achievementsList, achievements)) {
        achievementsList = achievements;
        setAchievements(achievements);
    }
});

function setAchievements(achievements) {
    clearContainers();
    global.inflateHtml({file: 'achievement-item.html'}, function (html) {
        if (html) {
            for (let i = 0; i < achievements.items.length; i++) {
                setAchievementItem(html, i, achievements.items[i]);
            }
        }
    });
}

function setAchievementItem(elementData, position, playerAchievement) {
    let achievementDefinition = _.find(achievementsDefinition.items, ['id', playerAchievement.id]);
    if (!_.isEmpty(achievementDefinition)) {
        let liElement = document.createElement('li');
        liElement.innerHTML = elementData;
        let achievementIconElem = liElement.getElementsByTagName('img')[0];
        let achievementProgressElem = liElement.getElementsByTagName('div')[0];
        let achievementNameElem = liElement.getElementsByTagName('h2')[0];
        let achievementDescriptionElem = liElement.getElementsByTagName('p')[0];

        if (playerAchievement.achievementState == 'HIDDEN') {
            liElement.removeChild(achievementProgressElem);

            achievementIconElem.src = dirs.images + '/ic_lock_grey.png';
            achievementNameElem.textContent = global.i18n.__('secret');
            achievementDescriptionElem.textContent = global.i18n.__('keep_playing_to_learn_more');
        } else if (playerAchievement.achievementState == 'REVEALED') {
            if (achievementDefinition.achievementType == 'INCREMENTAL') {
                liElement.removeChild(achievementIconElem);

                let currentSteps = playerAchievement.currentSteps ? playerAchievement.currentSteps : 0;
                let progress = Math.round((currentSteps / achievementDefinition.totalSteps) * 100);

                let spanElem = achievementProgressElem.getElementsByTagName('span')[0];

                achievementProgressElem.className = `c100 green small p${progress}`;
                spanElem.textContent = progress + '%';
            } else {
                liElement.removeChild(achievementProgressElem);

                achievementIconElem.src = achievementDefinition.revealedIconUrl;
            }
            achievementNameElem.textContent = achievementDefinition.name;
            achievementDescriptionElem.textContent = achievementDefinition.description;
        } else {
            liElement.removeChild(achievementProgressElem);

            achievementIconElem.src = achievementDefinition.unlockedIconUrl;
            achievementNameElem.textContent = achievementDefinition.name;
            achievementDescriptionElem.textContent = achievementDefinition.description;
        }

        liElement.id = getElementIdByAchievementId(achievementDefinition.id);

        achievementsContainer.appendChild(liElement);
    }
}

function setError(err) {
    global.inflateHtml({file: 'error.html', parent: achievementErrorContent}, function () {
        let messageKey = '';
        let errorMessageElement = achievementErrorContent.getElementsByClassName('error-text')[0];

        if (err.customErrorCode == global.errorCodes.notLoggedIn) {
            messageKey = 'login_to_view_achievements';
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
