/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

//set the agent to the one Android uses, this is necessary because the response is not complete
//if it uses a different agent, for example: Player.avatarImageUrl
require('googleapis/lib/transporters.js').prototype.USER_AGENT = 'com.google.android.play.games/<PLACE_YOUR_GOOGLE_GAME_ID_HERE>';

const _ = require('lodash');
const google = require('googleapis');
const OAuth2 = google.auth.OAuth2;
const plus = google.plus('v1');
const games = google.games('v1');

const REDIRECT_URL = 'http://foldingathome-project-redirect.com/oauth2redirect';
const CLIENT_SECRET = '<PLACE_YOUR_GOOGLE_CLIENT_SECRET_HERE>';

const oauth2Client = new OAuth2(global.gameIds.CLIENT_ID, CLIENT_SECRET, REDIRECT_URL);

let credentials = global.googleCredentialsData.getAll();

let achievementsDefinition = {};

if (!_.isEmpty(credentials)) {
    oauth2Client.setCredentials(credentials);
}

google.options({auth: oauth2Client}); // set auth as a global default

// generate a url that asks permissions for Google+ and Google Calendar scopes
const scopes = [
    'https://www.googleapis.com/auth/plus.login',
    'https://www.googleapis.com/auth/games',
    'https://www.googleapis.com/auth/userinfo.profile'
];

const authUrl = oauth2Client.generateAuthUrl({
    access_type: 'offline', // 'online' (default) or 'offline' (gets refresh_token)
    scope: scopes, // If you only need one scope you can pass it as string
    approval_prompt: 'force'
});

function updateCredentials() {
    if (!_.eq(credentials, oauth2Client.credentials)) {
        credentials = oauth2Client.credentials;
        global.googleCredentialsData.updateAll(oauth2Client.credentials);
    }
}

function callBackInterceptor(callback, err, response) {
    updateCredentials();
    if (err) {
        logger.error('callBackInterceptor error: ', err.message);
        if (!err.customErrorCode) {
            let errorCode = '';
            if (!isLogged()) {
                errorCode = global.errorCodes.notLoggedIn;
                //TODO add new statement to verify the internet connection
            } else {
                errorCode = global.errorCodes.unknown;
            }
            err.customErrorCode = errorCode;
        }
    }
    callback(err, response);
}

function getParamsFromRedirect(urlStr) {
    let url = require('url').parse(urlStr);
    let querystring = require('querystring');
    return querystring.parse(url.query);
}

function exchangeAccessToken(callback, code) {
    oauth2Client.getToken(code, function (err, tokens) {
        if (!err) {
            oauth2Client.setCredentials(tokens);
            global.googleCredentialsData.updateAll(tokens);
            getMe(callback);
        } else {
            callBackInterceptor(callback, err, null);
        }
    });
}


function doLogout(callback) {
    global.miscData.setPlayerProfile({});
    global.googleCredentialsData.deleteAll();
    global.contributionTimeData.setServerContributedTime(0);
    //revokeCredentials causes all the credentials to be revoked, not only the one used in this client
    //oauth2Client.revokeCredentials();
    oauth2Client.setCredentials({});
    credentials = {};
    achievementsDefinition = {};
    //callBackInterceptor(callback, null, null);
    callback({customErrorCode: global.errorCodes.notLoggedIn});
}

function doLogin(callback, BrowserWindow) {
    if (isLogged()) {
        doLogout(callback);
    } else {
        let browserWindow = new BrowserWindow({
            show: true,
            parent: global.mainWindow,
            modal: true,
        });

        browserWindow.on('closed', () => {
            browserWindow = null;
            //TODO restore login button state here
        });

        browserWindow.webContents.on('did-get-redirect-request', function (event, oldUrl, newUrl) {
            let params = getParamsFromRedirect(newUrl);
            if (params.code) {
                browserWindow.close();
                exchangeAccessToken(callback, params.code);
            } else if (params.error) {
                browserWindow.close();
            }
        });

        browserWindow.on('blur', () => {
            browserWindow.close();
        });

        browserWindow.loadURL(authUrl, {"extraHeaders": "pragma: no-cache\n"});
    }
}

function getMe(callback) {
    global.scores.getMyScorePromise()
        .then(function (response) {
            let highScore = response.items[0] ? parseInt(response.items[0].scoreValue) : 0;

            global.contributionTimeData.setServerContributedTime(highScore);
            global.miscData.setPlayerProfile(response.player);

            callBackInterceptor(callback, null, response.player);
        })
        .catch(function (err) {
            callBackInterceptor(callback, err, null);
        });
}

function getLeaderboards(callback) {
    getGames().scores.list(
        {leaderboardId: global.gameIds.LEAD_LEADERBOARDS, collection: 'PUBLIC', timeSpan: 'ALL_TIME'},
        function (err, response) {
            callBackInterceptor(callback, err, response);
        }
    );
}

function getAchievements(callback) {
    getGames().achievements.list(
        {playerId: 'me'},
        function (err, response) {
            callBackInterceptor(callback, err, response);
        }
    );
}

function getAchievementsDefinition(callback) {
    if (_.isEmpty(achievementsDefinition)) {
        getGames().achievementDefinitions.list(
            {},
            function (err, response) {
                if (!err) {
                    achievementsDefinition = response;
                }
                callBackInterceptor(callback, err, response);
            }
        );
    } else {
        logger.debug('getAchievementsDefinition serving cached definition');
        callBackInterceptor(callback, null, achievementsDefinition);
    }
}

function getPlayer(playerId, callback) {
    getGames().players.get(
        {playerId: playerId},
        function (err, response) {
            callBackInterceptor(callback, err, response);
        }
    );
}

function isLogged() {
    let tokens = global.googleCredentialsData.getAll();
    return !_.isEmpty(tokens);
}

function getGames() {
    return games;
}
module.exports = {
    updateCredentials: updateCredentials,
    getGames: getGames,
    doLogout: doLogout,
    doLogin: doLogin,
    isLogged: isLogged,
    getMe: getMe,
    getLeaderboards: getLeaderboards,
    getPlayer: getPlayer,
    getAchievements: getAchievements,
    getAchievementsDefinition: getAchievementsDefinition,
};
