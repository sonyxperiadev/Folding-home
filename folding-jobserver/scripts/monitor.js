/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

'use strict';

var spawn = require('child_process').spawn;
var loggly = require('loggly');
var logglyConfig = require('../src/<PLACE_YOUR_LOGGLY_CREDENTIONS_JSON_FILE>');
var consts = require('../src/consts_server.js');

var logglyClient = loggly.createClient(logglyConfig);

function log(logString, sendToLoggly, tags, callback) {
  console.log(Date() + ' ' + logString);
  if (sendToLoggly) {
    logglyClient.log(logString, tags, callback);
  }

}

function startChildProcess() {
  var child = spawn(process.execPath, [process.argv[2]], {stdio: ['pipe', 'pipe', 'pipe']});

  child.stdout.on('data', function(data) {
    log(data.toString(), false);
  });

  child.stderr.on('data', function(data) {
    log(data.toString(), true, ['JobServer', 'Crash']);
  });

  child.on('close', function(code, signal) {

    var timeoutID = setTimeout(function() {
      log('Could not send report to loggly on a reasonable time. Forcing exit', false);
      startChildProcess();
    }, consts.TIMEOUT_REQUEST);

    log('Server exited with code: ' + code + ' and signal: ' + signal, true, ['JobServer', 'Crash'], function() {
      log('Respawning server...', true, ['JobServer']);
      clearTimeout(timeoutID);
      startChildProcess();
    });
  });

  process.on('SIGTERM', function() {
    log('Monitor received SIGTERM', true);
    log('Terminating server', true);
    process.kill(child.pid, 'SIGTERM');
  });
}

startChildProcess();
