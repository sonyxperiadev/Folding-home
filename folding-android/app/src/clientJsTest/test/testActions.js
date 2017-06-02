/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
var assert = require("assert");
var consts_tests = require("./Util/constants_tests.js");
var Client = require(consts_tests.PATH_MAIN_CLIENT);
var consts_client = require(consts_tests.PATH_CONSTANTS_CLIENTE);

var path = require('path');
var helper = require(path.resolve(__dirname,
  consts_tests.PATH_JOBSERVER_TEST_HELPER));

describe("Client Action", function(){
  var client = new Client();

  it("Should be able to load key", function(done) {
    var keyData = {key: 'clientkey'};
    client.actionKey(keyData);
    assert.notEqual(client.jobclient.key, null);
    assert.notEqual(client.jobclient.cert, null);
    keyData = {key: "[no-key]"};
    client.actionKey(keyData);
    assert.equal(client.jobclient.key, null);
    assert.equal(client.jobclient.cert, null);
    done();
  });

  it("Should be able to kill the job script", function(done) {
    client.runJob("script.on('PAUSE', function () {}); setTimeout(function() {console.log('Job Finished!')}, 5000);", function(code) {});
    assert.notEqual(client.getJobProc(), null);
    setTimeout(function() {
      consts_client.TIMEOUT_PAUSE_JOB = 0;
      consts_client.TIMEOUT_TERM_JOB = 0;
      client.setShouldKill(true);
      client.actionKill();
      setTimeout(function() {
        assert.equal(client.getJobProc(), null);
        done();
      }, 100);
    }, 100);
  });
});
