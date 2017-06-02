/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const _ = require('lodash');
const bluebird = require('bluebird');

const ONE_MINUTE_IN_MILLIS = 1000 * 60;
const ONE_HOUR_IN_MILLIS = ONE_MINUTE_IN_MILLIS * 60;

const STEP_INCREMENT_ACHIEVEMENT = ONE_MINUTE_IN_MILLIS * 10; //each step is 10 minutes

const MIN_SCORE_TO_SUBMIT = ONE_MINUTE_IN_MILLIS * 10; //minutes

let submittingScore = false;

function submitScore(scoreObj) {
    logger.debug(`submittingScore: ${submittingScore} islogged: ${global.googleApis.isLogged()} scoreObj: `, scoreObj);
    if (!submittingScore && scoreObj.localScore && scoreObj.localScore >= MIN_SCORE_TO_SUBMIT && global.googleApis.isLogged()) {
        submittingScore = true;

        let totalTime = 0;
        let achievementsDefinition = [];

        //get the score from GGS
        logger.debug('Submit score : get the high score');
        getMyScorePromise()
            .then(function (result) {
                //update the score
                logger.debug('Submit score : updating the score');
                let highScore = result.items[0] ? parseInt(result.items[0].scoreValue) : 0;

                global.contributionTimeData.setServerContributedTime(highScore);
                totalTime = scoreObj.localScore + highScore;

                let submitProm = bluebird.promisify(global.googleApis.getGames().scores.submit);
                let submitParam = {leaderboardId: global.gameIds.LEAD_LEADERBOARDS, score: totalTime};
                return submitProm(submitParam);
            })
            .then(function (result) {
                //get the achievements definition
                logger.debug('Submit score : getting the achievements definition');
                global.contributionTimeData.setLocalContributedTime(0);
                global.contributionTimeData.setServerContributedTime(totalTime);

                let achievementsProm = bluebird.promisify(global.googleApis.getAchievementsDefinition);
                return achievementsProm();
            })
            .then(function (result) {
                //increment the 'incremental' achievements
                logger.debug('Submit score : incrementing achievements');
                achievementsDefinition = result;
                return unlockIncrementalAchievementsWithPromise(achievementsDefinition, totalTime);
            })
            .then(function (result) {
                //unlocking straight achievements
                logger.debug('Submit score : unlocking straight achievements');
                return unlockStraightAchievementsWithPromise(scoreObj.consecutiveContributedTime);
            })
            .catch(function (error) {
                logger.error('submitScore error: ', error.message);
            })
            .finally(function () {
                global.googleApis.updateCredentials();
                submittingScore = false;
            });
    }
}


function unlockIncrementalAchievementsWithPromise(achievementsDefinition, totalTime) {
    let setStepsProm = bluebird.promisify(global.googleApis.getGames().achievements.setStepsAtLeast);

    let setStepsPromisesArray = [];
    let achievementList = sortAchievements(achievementsDefinition);
    let totalSteps = totalTime / STEP_INCREMENT_ACHIEVEMENT;
    let accumulatedSteps = 0;
    for (let achievement of achievementList) {
        accumulatedSteps += achievement.totalSteps;
        let stepsToSubmit = calculateStepsToSubmit(totalSteps, accumulatedSteps, achievement.totalSteps);
        if (stepsToSubmit <= 0) {
            break;
        } else {
            let stepsObj = {achievementId: achievement.id, steps: stepsToSubmit};
            setStepsPromisesArray.push(setStepsProm(stepsObj));
        }
    }

    return bluebird.each(setStepsPromisesArray, function (result, index, length) {
        let currentAchievement = achievementList[index];
        if (!_.isEmpty(result) && !_.isEmpty(currentAchievement)) {
            showAchievementUnlockedNotification(currentAchievement.id, result.newlyUnlocked);
        }
        return result;
    });
}


function unlockStraightAchievementsWithPromise(consecutiveContributedTime) {
    //unlock straight achievements
    let straightAchievements = [
        {hours: 6, id: global.gameIds.ACH_6_HOURS_STRAIGHT_BREAST_CANCER},
        {hours: 12, id: global.gameIds.ACH_12_HOURS_STRAIGHT_BREAST_CANCER},
        {hours: 18, id: global.gameIds.ACH_18_HOURS_STRAIGHT_BREAST_CANCER},
        {hours: 24, id: global.gameIds.ACH_24_HOURS_STRAIGHT_BREAST_CANCER}];

    let unlockAchivementProm = bluebird.promisify(global.googleApis.getGames().achievements.unlock);
    let unlockAchivementsPromisesArray = [];

    let researchDetails = global.miscData.getResearchDetails();
    //breast cancer id is ee4240c3-0d76-4229-8c1a-b933c6be6921
    if (consecutiveContributedTime && researchDetails && researchDetails.target_id == 'ee4240c3-0d76-4229-8c1a-b933c6be6921') {
        for (let currentAchievement of straightAchievements) {
            if (consecutiveContributedTime >= currentAchievement.hours * ONE_HOUR_IN_MILLIS) {
                unlockAchivementsPromisesArray.push(unlockAchivementProm({achievementId: currentAchievement.id}));
            } else {
                break;
            }
        }
    }

    return bluebird.each(unlockAchivementsPromisesArray, function (result, index, length) {
        let currentAchievement = straightAchievements[index];
        if (!_.isEmpty(result) && !_.isEmpty(currentAchievement)) {
            showAchievementUnlockedNotification(currentAchievement.id, result.newlyUnlocked);
        }
        return result;
    });
}


function showAchievementUnlockedNotification(achievementId, newlyUnlocked) {
    logger.debug(`showAchievementUnlockedNotification ${achievementId} newlyUnlocked: ${newlyUnlocked}`);
    if (newlyUnlocked) {
        global.googleApis.getAchievementsDefinition(function (err, achievementsDefinition) {
            if (err) {
                logger.error('showAchievementUnlockedNotification error: ', err.message);
            } else if (!_.isEmpty(achievementsDefinition)) {
                let currentAchievementDefinition = _.find(achievementsDefinition.items, ['id', achievementId]);
                if (currentAchievementDefinition) {
                    let title = global.i18n.__('achievement_unlocked');
                    let message = currentAchievementDefinition.name;
                    let iconUrl = currentAchievementDefinition.unlockedIconUrl;
                    global.notification.showNotification({title: title, message: message, iconUrl: iconUrl});
                }
            }
        });
    }
}

function unlockAchievement(achievementId) {
    let unlockAchivementProm = bluebird.promisify(global.googleApis.getGames().achievements.unlock);
    unlockAchivementProm({achievementId: achievementId})
        .then(function (result) {
            showAchievementUnlockedNotification(achievementId, result.newlyUnlocked);
        })
        .catch(function (err) {
            logger.error('unlockAchievement error: ', err.message);
        });
}

function getMyScorePromise() {
    let getMyScoreProm = bluebird.promisify(global.googleApis.getGames().scores.get);
    let myScoreAttribute = {leaderboardId: global.gameIds.LEAD_LEADERBOARDS, playerId: 'me', timeSpan: 'ALL_TIME'};
    return getMyScoreProm(myScoreAttribute);
}

function sortAchievements(achievementsDefinition) {
    let achievementList = [];
    achievementList.push(_.find(achievementsDefinition.items, ['id', global.gameIds.ACH_AMINOACID]));
    achievementList.push(_.find(achievementsDefinition.items, ['id', global.gameIds.ACH_PEPTIDE]));
    achievementList.push(_.find(achievementsDefinition.items, ['id', global.gameIds.ACH_POLYPEPTIDE]));
    achievementList.push(_.find(achievementsDefinition.items, ['id', global.gameIds.ACH_PROTEIN]));
    achievementList.push(_.find(achievementsDefinition.items, ['id', global.gameIds.ACH_ENZYME]));
    return achievementList;
}


function calculateStepsToSubmit(totalSteps, accumulatedSteps, steps) {
    let stepsRatio = Math.min(1, totalSteps / accumulatedSteps);
    return Math.floor(stepsRatio * steps);
}

module.exports = {
    submitScore: submitScore,
    getMyScorePromise: getMyScorePromise,
    unlockAchievement: unlockAchievement,
};