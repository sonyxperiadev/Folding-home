/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var log4js = require('log4js');
let appenders = [];

if (global.buildConfig.print_logs) {
    appenders.push({type: 'console', category: 'Folding@Home'});
}

//if (global.buildConfig.write_logs) {
appenders.push({
    type: 'file',
    filename: global.dirs.home + '/folding.log',
    'maxLogSize': 20480,
    category: 'Folding@Home'
});


log4js.configure({
    appenders: appenders
});
const logger = log4js.getLogger('Folding@Home');

module.exports = logger;