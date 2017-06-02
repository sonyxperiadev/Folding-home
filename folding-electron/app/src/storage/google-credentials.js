/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const googleCredentialsKey = 'google_credentials';
const accessTokenKey = 'access_token';
const tokenTypeKey = 'token_type';
const expiryDateKey = 'expiry_date';
const idTokenKey = 'id_token';
const refreshTokenKey = 'refresh_token';

function getAttribute(key) {
    return database.get(googleCredentialsKey + '.' + key).value();
}

function setAttribute(key, value) {
    //console.log(`setAttribute key: ${key} value: ${value}`);
    if (key && value) {
        database.set(googleCredentialsKey + '.' + key, value).value();
    }
}

function deleteAttribute(key) {
    return database.get(googleCredentialsKey + '.' + key).remove({}).value();
}

function getAll() {
    return database.get(googleCredentialsKey).value();
}

function updateAll(credentials) {
    setAttribute(accessTokenKey, credentials.access_token);
    setAttribute(tokenTypeKey, credentials.token_type);
    setAttribute(expiryDateKey, credentials.expiry_date);
    setAttribute(idTokenKey, credentials.id_token);
    setAttribute(refreshTokenKey, credentials.refresh_token);
}

function deleteAll() {
    database.set(googleCredentialsKey, {}).value();
}

module.exports = {
    accessTokenKey:accessTokenKey,
    tokenTypeKey:tokenTypeKey,
    expiryDateKey:expiryDateKey,
    idTokenKey:idTokenKey,
    refreshTokenKey:refreshTokenKey,

    setAttribute: setAttribute,
    getAttribute: getAttribute,
    getAll: getAll,
    deleteAll: deleteAll,
    updateAll: updateAll,
};