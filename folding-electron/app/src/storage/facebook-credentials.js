/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const facebookCredentialsKey = 'facebook_credentials';
const accessTokenKey = 'access_token';

function getAttribute(key) {
    return database.get(facebookCredentialsKey + '.' + key).value();
}

function setAttribute(key, value) {
    //console.log(`setAttribute key: ${key} value: ${value}`);
    if (key && value) {
        database.set(facebookCredentialsKey + '.' + key, value).value();
    }
}

function deleteAttribute(key) {
    return database.get(facebookCredentialsKey + '.' + key).remove({}).value();
}

function getAll() {
    return database.get(facebookCredentialsKey).value();
}

function updateAll(credentials) {
    console.log('credentials::', credentials);
    setAttribute(accessTokenKey, credentials.access_token);
}

function deleteAll() {
    database.set(facebookCredentialsKey, {}).value();
}

module.exports = {
    accessTokenKey:accessTokenKey,

    setAttribute: setAttribute,
    getAttribute: getAttribute,
    getAll: getAll,
    deleteAll: deleteAll,
    updateAll: updateAll,
};