/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const ONE_MINUTE_IN_MILLIS = 1000 * 60;
const ONE_HOUR_IN_MILLIS = ONE_MINUTE_IN_MILLIS * 60;

const ONE_WEEK_IN_DAYS = 7;
const ONE_DAY_IN_HOURS = 24;
const ONE_MINUTE_IN_SECONDS = 60;

function formatDisplayTime(time) {
    let minutes = Math.floor(time / ONE_MINUTE_IN_MILLIS);
    let hours = Math.floor(time / ONE_HOUR_IN_MILLIS);
    let days = Math.floor(hours / ONE_DAY_IN_HOURS);
    let weeks = Math.floor(days / ONE_WEEK_IN_DAYS);

    days = days % ONE_WEEK_IN_DAYS;
    hours = hours % ONE_DAY_IN_HOURS;
    minutes = minutes % ONE_MINUTE_IN_SECONDS;

    if (weeks > 0) {
        return global.i18n.__('time_string_weeks', weeks, days, hours);
    } else if (days > 0) {
        return global.i18n.__('time_string_days', days, hours, minutes);
    } else {
        return global.i18n.__('time_string_hours', hours, minutes);
    }
}

module.exports = {
    formatDisplayTime: formatDisplayTime
};

