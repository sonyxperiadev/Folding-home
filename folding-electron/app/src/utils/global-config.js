/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const _ = require('lodash');
const path = require('path');
const fs = require('fs');
const {app} = require('electron');

function initGlobalVars() {
    // global vars used across the app
    // these vars are set in: main-process.js and read from the render process in index.html
    // they can be set again in app.js in order to make they available to be used in angular directives/templates
    global.buildConfig = {};
    global.dirs = {};
    global.eventMessages = {};
    global.gameIds = {};
    global.errorCodes = {};

    initBuildConfig();
    initProjectDirs();
    initEventMessages();
    initGameIds();
    initErrorCodes();
}

function initErrorCodes(){
    errorCodes.notLoggedIn = 'notLoggedIn';
    errorCodes.noInternet = 'noInternet';
    errorCodes.unknown = 'unknown';
}

function initGameIds(){
    gameIds.CLIENT_ID = '<PLACE_YOUR_GOOGLE_GAME_CLIENT_ID_HERE>';
    gameIds.ACH_SHARE_ON_FACEBOOK = '<PLACE_YOUR_SHARE_ON_FACEBOOK_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_START_FOLDING = '<PLACE_YOUR_START_FOLDING_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_AMINOACID = '<PLACE_YOUR_AMINOACID_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_PEPTIDE = '<PLACE_YOUR_PEPTIDE_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_POLYPEPTIDE = '<PLACE_YOUR_POLYPEPTIDE_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_PROTEIN = '<PLACE_YOUR_PROTEIN_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_ENZYME = '<PLACE_YOUR_ENZYME_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_OPEN_THE_BREAST_CANCER_DETAILS = '<PLACE_YOUR_OPEN_BREAST_CANCER_DETAILS_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_6_HOURS_STRAIGHT_BREAST_CANCER = '<PLACE_YOUR_6_HOURS_STRAIGHT_BREAST_CANCER_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_12_HOURS_STRAIGHT_BREAST_CANCER = '<PLACE_YOUR_12_HOURS_STRAIGHT_BREAST_CANCER_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_18_HOURS_STRAIGHT_BREAST_CANCER = '<PLACE_YOUR_18_HOURS_STRAIGHT_BREAST_CANCER_ACHIEVEMENT_ID_HERE>';
    gameIds.ACH_24_HOURS_STRAIGHT_BREAST_CANCER = '<PLACE_YOUR_24_HOURS_STRAIGHT_BREAST_CANCER_ACHIEVEMENT_ID_HERE>';
    gameIds.LEAD_LEADERBOARDS = '<PLACE_YOUR_LEADERBOARD_ID_HERE>';
}

function initBuildConfig() {
    buildConfig.write_logs = _.indexOf(process.argv, 'write_logs') != -1;
    buildConfig.print_logs = _.indexOf(process.argv, 'print_logs') != -1;
    buildConfig.report_crashes = _.indexOf(process.argv, 'report_crashes') != -1;
    buildConfig.report_analytics = _.indexOf(process.argv, 'report_analytics') != -1;
}

function initProjectDirs() {
    let rootDir = path.join(__dirname, '../../');
    let binRootDir = global.isAsar() ? path.join(rootDir, '../../app') : rootDir;

    dirs.home = path.join(app.getPath('home'), '/.FoldingAtHome');
    dirs.root = rootDir;
    dirs.source = path.join(rootDir, 'src');
    dirs.views = path.join(rootDir, 'res/views');
    dirs.assets = path.join(rootDir, 'res/assets');
    dirs.css = path.join(rootDir, 'res/assets/css');
    dirs.fonts = path.join(rootDir, 'res/assets/fonts');
    dirs.images = path.join(rootDir, 'res/assets/img');
    dirs.modules = path.join(rootDir, 'node_modules');
    dirs.l10n = path.join(rootDir, 'res/strings');

    dirs.raw = path.join(binRootDir, 'raw');
    dirs.native = path.join(binRootDir, 'libs', process.platform, process.arch);

    if (!fs.existsSync(dirs.home)) {
        fs.mkdirSync(dirs.home);
    }
}

function initEventMessages() {
    eventMessages.startProcess = 'start-process';
    eventMessages.stopProcess = 'stop-process';

    eventMessages.getResearchDetails = 'get_research_details';
    eventMessages.gotResearchDetails = 'got_research_details';

    eventMessages.doLogin = 'do_login';

    eventMessages.getLoginInfo = 'get_login_info';
    eventMessages.gotLoginInfo = 'got_login_info';

    eventMessages.showReadMore = 'show_read_more';
    eventMessages.showSetupReadMore = 'setup_guide_show_read_more';

    eventMessages.getPeopleHelpingOut = 'get_people_helping';
    eventMessages.gotPeopleHelpingOut = 'got_people_helping';

    eventMessages.getLegalText = 'get_legal_text';
    eventMessages.gotLegalText = 'got_legal_text';

    eventMessages.getLeaderBoard = 'get_leaderboard';
    eventMessages.gotLeaderBoard = 'got_leaderboard';

    eventMessages.getContributionTime = 'get_contribution_time';
    eventMessages.gotContributionTime = 'got_contribution_time';

    eventMessages.getAchievements = 'get_achievements';
    eventMessages.gotAchievements = 'got_achievements';

    eventMessages.getAchievementsDefinition = 'get_achievements_definition';
    eventMessages.gotAchievementsDefinition = 'got_achievements_definition';

    eventMessages.getPlayer = 'get_player';
    eventMessages.gotPlayer = 'got_player';

    eventMessages.getPlayerScore = 'get_player_score';
    eventMessages.gotPlayerScore = 'got_player_score';

    eventMessages.getOnlineStatus = 'get_online_status';
    eventMessages.gotOnlineStatus = 'got_online_status';

    eventMessages.shareOnFacebook = 'share_on_facebook';
}
initGlobalVars();
