/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
var fs = require('fs');
var resPath = __dirname + '/';

var internal_environment_production = {
  JOB_SERVER_ADDRESS: '<PLACE_YOUR_JOB_SERVER_ADDRESS_HERE>',
  PORT_JOB_SERVER: 443,
  PATH_JOB_SERVER: '/getjob',
  PATH_REPORT: '/report',
  TIMEOUT_REQUEST: 60000,
  TIMEOUT_TEST_MODE: 2000, //miliseconds
  TIMEOUT_NOW_TRY: 0, //miliseconds
  TIMEOUT_MIN_TRY: 1000, //miliseconds
  TIMEOUT_MAX_TRY: 3600000, //miliseconds
  TIMEOUT_PAUSE_JOB: 20000, // miliseconds
  TIMEOUT_TERM_JOB: 10000 // miliseconds
};

var current = internal_environment_production;

/**
 * Exports
 */
module.exports = {
  current: current
};
