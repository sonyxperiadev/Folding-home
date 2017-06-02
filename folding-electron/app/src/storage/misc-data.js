/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const uuidKey = 'uuid';
const researchDetailsKey = 'research_details';
const peopleHelpingOutKey = 'people_helping_out';
const playerProfile = 'player_profile';

function getUUID() {
    let savedUUID = database.get(uuidKey).value();
    if (savedUUID && savedUUID.length > 0) {
        return savedUUID;
    }
    let nodeUUID = require('node-uuid');
    let generatedUUID = nodeUUID.v4();
    database.set(uuidKey, generatedUUID).value();
    return generatedUUID;
}

function getResearchDetails() {
    return database.get(researchDetailsKey).value();
}

function setResearchDetails(details) {
    database.set(researchDetailsKey, details).value();
}

function getPeopleHelpingOut() {
    return database.get(peopleHelpingOutKey).value();
}

function setPeopleHelpingOut(details) {
    database.set(peopleHelpingOutKey, details).value();
}

function getPlayerProfile() {
    return database.get(playerProfile).value();
}

function setPlayerProfile(profile) {
    database.set(playerProfile, profile).value();
}

module.exports = {
    getUUID: getUUID,
    getResearchDetails: getResearchDetails,
    setResearchDetails: setResearchDetails,
    getPlayerProfile: getPlayerProfile,
    setPlayerProfile: setPlayerProfile,
    getPeopleHelpingOut: getPeopleHelpingOut,
    setPeopleHelpingOut: setPeopleHelpingOut,
};
