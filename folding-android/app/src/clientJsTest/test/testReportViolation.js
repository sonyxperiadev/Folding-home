/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var path = require('path');
var assert = require('assert');
var consts_tests = require("./Util/constants_tests.js");
var helper = require(path.resolve(__dirname,
  consts_tests.PATH_JOBSERVER_TEST_HELPER));
var env = require(consts_tests.PATH_CONSTANTS_CLIENTE);

var Client = require(consts_tests.PATH_MAIN_CLIENT);

describe('Client report violations.', function() {
  var client = null;

  before(function(done) {
    var mock_consts = {};
    for (var key in env) {
      mock_consts[key] = env[key];
    }
    mock_consts['PORT_JOB_SERVER'] = helper.getJobDescriptionOptions().port;
    mock_consts['JOB_SERVER_ADDRESS'] = helper.getJobDescriptionOptions().host;
    mock_consts['JOB_SERVER_CACERT'] = helper.getJobDescriptionOptions().ca;

    // Load client.js
    client = new Client();
    client.setConsts(mock_consts);

    // Startup servers
    helper.initJobServer(function() {
      helper.flushDB(function() {
        helper.initPMServer();
        done();
      });
    });
  });

  after(function(done) {
    helper.flushDB(done);
  });

  it('Should be able to send violations to jobserver.', function(done) {
    client.postReportViolation({
      event: 'violation',
      data: {
        violation: 'host',
        message: 'Tried to connect with a non-allowed host: test.com'
      }
    }, function(err, res) {
      assert(!err);
      assert.equal(200, res.statusCode);
      done();
    });
  });

  it('The child process should fire a report violation event to parent process when a access to disk is attempted' , function (done) {
    var child_process = client.runJob("var a = require('fs')", function() {});

    assert.notEqual(child_process, null);

    var messageReceived = false;

    child_process.on('message', function(evt) {
      assert.notEqual(evt, null);
      assert.notEqual(evt.data, null);
      assert.notEqual(evt.data.violation, null);
      assert.equal(evt.data.violation, 'require_module');
      assert.notEqual(evt.event, null);
      assert.equal(evt.event, 'violation');
      messageReceived = true;
    });

    child_process.on('close', function(code) {
      assert.notEqual(code, 0);
      assert.ok(messageReceived);
      done();
    });
  });
  it('The child process should fire a report violation event to parent process when a access to a non-allowed host' , function (done) {
    var nonAllowedHosts = [
      '192.168.1.2',
      'localhost',
      '255.255.255.255',
      'FF00:0000:0000:0000:0000:0000:0000:0000',
      '127.0.0.1',
      '198.18.88.10'
    ];
    // Increase the Timeout
    // (this should be scaled according to the size of nonAllowedHosts)
    this.timeout(5000);

    function testHost(host_index) {
      if (host_index == nonAllowedHosts.length) {
        done(); // No more hosts to test
      }

      var host = nonAllowedHosts[host_index];
      var child_process = client.runJob("var https = require('https'); https.request({host: '" + host + "', port: 443, method: 'GET'})", function() {});

      assert.notEqual(child_process, null);

      var messageReceived = false;

      child_process.on('message', function(evt) {
        assert.notEqual(evt, null);
        assert.notEqual(evt.data, null);
        assert.notEqual(evt.data.violation, null);
        assert.equal(evt.data.violation, 'host');
        assert.notEqual(evt.event, null);
        assert.equal(evt.event, 'violation');
        messageReceived = true;
      });

      child_process.on('close', function(code) {
        assert.notEqual(code, 0);
        assert.ok(messageReceived);
        // Test next host
        testHost(host_index + 1);
      });
    }

    // Test host at index 0
    testHost(0);
  });
});
