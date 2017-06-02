/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const fs = require('fs');
const path = require('path');
const childProcess = require('child_process');

const messenger = global.requireLocalPackage('/environment/messenger.js');

const CONTRIBUTION_UPDATE_INTERVAL = 30000; //30 seconds

let contributionUpdateTimer;
let lastTimeChecked = 0; //used to increment the contributed time
let contributionStartTime = 0; //used to count how many consecutive hours the user has been contributing
let currentProcess;

const PLUGINS_PATH = dirs.native + path.delimiter + path.join(dirs.native, '/plugins');

const options = {
    cwd: dirs.raw,
    stdio: ['pipe', 'pipe', 'pipe', 'ipc'],
    env: {
        path: PLUGINS_PATH,//windows
        DYLD_LIBRARY_PATH: PLUGINS_PATH,//mac ox
        LD_LIBRARY_PATH: PLUGINS_PATH//linux
    }
};

function getGcompExecutablePath(){
    let executableFile = process.platform == 'win32' ? 'gcompnode.exe' : 'gcompnode';
    return path.join(dirs.native, executableFile);
}

function splitLines(data) {
    const lines = data.toString().split('\n');
    for (let i = 0; i < lines.length; i++) {
        try {
            let currentLine = lines[i];
            if (currentLine && currentLine.length > 0) {
                logger.info('Read from client > ', currentLine);
                const messageJSON = JSON.parse(currentLine);
                handleMessage(messageJSON);
            }
        } catch (e) {
            //do nothing, this means the line isn't a valid json and we do not have to process it
        }
    }
}

function handleMessage(message) {
    if (message.action == 'get_key') {
        writeToProcess(messenger.getKey());
    } else if (message.action == 'research_details') {
        miscData.setResearchDetails(message.content);
        sendMessage(eventMessages.gotResearchDetails, message.content);
    } else if (message.action == 'number_of_users') {
        let peopleHelpingOut = message.content.number_of_users;
        miscData.setPeopleHelpingOut(peopleHelpingOut);
        sendMessage(eventMessages.gotPeopleHelpingOut, peopleHelpingOut);
    }
}

function writeToProcess(message) {
    try {
        if (currentProcess) {
            let messageStr = JSON.stringify(message);
            logger.info('Write to client > ' + messageStr);
            currentProcess.stdin.write(messageStr + '\n');
        }
    } catch (e) {
        logger.error(e);
    }
}

function copyAndLinkBinaries() {
    let sourceDir = dirs.native + '/plugins';
    let destDir = options.cwd + '/plugins';
    try {
        fs.accessSync(destDir, fs.F_OK);
    } catch (error) {
        fs.symlinkSync(sourceDir, destDir, 'junction');
    }
}

function stopProcess(immediately) {
    writeToProcess(messenger.getKill(immediately));
    incrementContributedTime(true);
}

function startProcess() {
    incrementContributedTime(false);
    try {
        if (currentProcess) {
            writeToProcess(messenger.getResume());
        } else {
            copyAndLinkBinaries();
            const gcompPath = getGcompExecutablePath();
            try {
                fs.chmodSync(gcompPath, 0o755);
            } catch (err){
                logger.error('Unable to chmod the the gcomp file');
            }
            currentProcess = childProcess.spawn(gcompPath, ['client.js'], options);

            currentProcess.stdout.on('data', (data) => {
                splitLines(data);
            });

            currentProcess.stderr.on('data', (data) => {
                logger.error(`stderr: ${data}`);
            });

            currentProcess.on('close', (code) => {
                currentProcess = null;
                logger.debug(`child process exited with code ${code}`);
            });
        }
    } catch (err){
        logger.error(err);
    }
}

function incrementContributedTime(contributionStopped) {
    let currentTime = process.uptime() * 1000; //process.uptime() returns the time in seconds
    if (lastTimeChecked > 0) {
        let contributedTime = Math.floor(currentTime - lastTimeChecked);
        let totalLocalContributedTime = global.contributionTimeData.incrementLocalContributedTime(contributedTime);
        let consecutiveContributedTime = Math.floor(currentTime - contributionStartTime);
        global.scores.submitScore({localScore: totalLocalContributedTime, consecutiveContributedTime: consecutiveContributedTime});

        let formattedTime = global.contributionTimeData.getTotalContributedTime(true);
        global.sendMessage(global.eventMessages.gotPlayerScore, formattedTime);
    }

    if (contributionStopped) {
        logger.debug(`Contribution timer cleared`);
        clearInterval(contributionUpdateTimer);
        contributionUpdateTimer = null;
    } else if (!contributionUpdateTimer) {
        logger.debug(`Contribution timer started`);
        contributionUpdateTimer = setInterval(incrementContributedTime, CONTRIBUTION_UPDATE_INTERVAL);
        global.scores.unlockAchievement(global.gameIds.ACH_START_FOLDING);
    }

    lastTimeChecked = contributionStopped ? 0 : currentTime;

    //initialize the contributionStartTime as the following: if the contribution has stopped, set it to 0
    //if not, set it to the current time only if it's previous value is 0 (which means it has not been initialized)
    contributionStartTime = contributionStopped ? 0 : (contributionStartTime === 0 ? currentTime : contributionStartTime);
}

module.exports = {
    startProcess: startProcess,
    stopProcess: stopProcess
};

