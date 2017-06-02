/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const contributionTimeKey = 'contribution_time';
const localContributedTimeKey = 'local_contributed_time';
const serverContributedTimeKey = 'server_contributed_time';

const moment = require('moment');
const fs = require('fs');

function getAttribute(key, defaultValue) {
    let value = database.get(contributionTimeKey + '.' + key).value();
    return value ? value : defaultValue;
}

function setAttribute(key, value) {
    if (key) {
        database.set(contributionTimeKey + '.' + key, value).value();
    }
}

function getLocalContributedTime() {
    return getAttribute(localContributedTimeKey, 0);
}

function setLocalContributedTime(value) {
    setAttribute(localContributedTimeKey, value);
}

function incrementLocalContributedTime(valueToIncrement) {
    saveReportDataToChart(valueToIncrement);
    let totalValue = getLocalContributedTime() + valueToIncrement;
    setLocalContributedTime(totalValue);
    return totalValue;
}

function getServerContributedTime() {
    return getAttribute(serverContributedTimeKey, 0);
}

function setServerContributedTime(value) {
    setAttribute(serverContributedTimeKey, value);
}

function getTotalContributedTime(formatted) {
    let serverTime = getServerContributedTime();
    let localTime = getLocalContributedTime();
    let totalTime = serverTime + localTime;
    if (formatted) {
        return global.formatter.formatDisplayTime(totalTime);
    } else {
        return totalTime;
    }
}

/*Function to create/update data that is used to draw the charts about contribution time*/
function saveReportDataToChart(timeToIncrement) {
    let contributionTimeJson = [];
    let currentDate = moment().format("YYYY-MM-DD");
    let currentDateTimeToJson = [{date: currentDate, contributed_time:0}];
    fs.readFile(dirs.home + '/db_report.json', 'utf8', (err, data) => {

     if (err instanceof Error && err.code === 'ENOENT') {
        return fs.writeFileSync(dirs.home + '/db_report.json', JSON.stringify(currentDateTimeToJson));
      }

     try {
        contributionTimeJson = JSON.parse(data);
        if (!Array.isArray(contributionTimeJson)){
            throw "invalid array";
        }
      } catch (e) {
        return fs.writeFileSync(dirs.home + '/db_report.json', JSON.stringify(currentDateTimeToJson));
      }

      completeMissingDays(contributionTimeJson);

      fs.writeFileSync(dirs.home + '/db_report.json', JSON.stringify(contributionTimeJson));

     if (timeToIncrement) {
       if (contributionTimeJson[0].date == currentDate){
          let sumTime = contributionTimeJson[0].contributed_time + timeToIncrement;
          contributionTimeJson[0].contributed_time = sumTime;
       }
        fs.writeFileSync(dirs.home + '/db_report.json', JSON.stringify(contributionTimeJson));
      }
   });

    /*Function to complete missing days (when the user pass some days without using the application)*/
    function completeMissingDays(array){
        if(array.length > 1){
            let lastDayOn = moment(array[1].date);
            let recentDayOn = moment();

            let diffDays = Math.abs(recentDayOn.diff(lastDayOn, 'days') - 1);

            for (let i = 0; i < diffDays; i++) {
                let dateToFill = moment().subtract(i, 'days').format("YYYY-MM-DD");
                array.push({"date": dateToFill, "contributed_time": 0});
            }
            array.sort(sortArrayByDateDESC);
        }else if (array.length == 1 && array[0].date != currentDate){
            let previousDate = moment(array[0].date).add(1, 'days').format("YYYY-MM-DD");
            array.push({"date": previousDate, "contributed_time": 0});
            array.sort(sortArrayByDateDESC);
            fs.writeFileSync(dirs.home + '/db_report.json', JSON.stringify(array));
            completeMissingDays(array);
        }
    }
    function sortArrayByDateDESC(a, b) {
        return moment(b.date).valueOf() - moment(a.date).valueOf();
    }
}

saveReportDataToChart();

module.exports = {
    getLocalContributedTime: getLocalContributedTime,
    setLocalContributedTime: setLocalContributedTime,
    incrementLocalContributedTime: incrementLocalContributedTime,
    getServerContributedTime: getServerContributedTime,
    setServerContributedTime: setServerContributedTime,
    getTotalContributedTime: getTotalContributedTime,
};
