/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var EventEmitter = require('events').EventEmitter
var assert = require("assert");
var path = require('path');
var consts_tests = require("./Util/constants_tests.js");
var helper = require(path.resolve(__dirname, consts_tests.PATH_JOBSERVER_TEST_HELPER));
var env = require(consts_tests.PATH_CONSTANTS_CLIENTE);

var Client = require(consts_tests.PATH_MAIN_CLIENT);

var mock_io = new EventEmitter();

var mock_write = function(message) {
  mock_io.emit('data', JSON.stringify(message) + '\n');
}

function send_to_client(client, message) {
  client.host_handle_message(JSON.stringify(message) + '\n');
}

function assertGetKey(client, callback) {
  mock_io.once('data', function (data) {
    var msg = JSON.parse(data);
    assert.equal('get_key', msg.action, "Host expects get_key action from client.");
    callback();
  });
  client.client_fsm.start();
};

function assertKeyAccepted(client, callback) {
  mock_io.once('data', function (data) {
    var msg = JSON.parse(data);
    assert.equal('key_accepted', msg.action, "Host expects key_accepted action from client.");
    callback();
  });
  send_to_client(client, {action:'key', content:{key: 'clientkey', uuid: 'DUMMY_UUID'}});
};

function assertTxJobReceived(client, callback) {
  mock_io.once('data', function (data) {
    var msg = JSON.parse(data);
    assert.equal('job_received', msg.action, "Host expects job_received action from client.");
    callback();
  });
};

function assertExecutingJob(client, callback) {
	  mock_io.once('data', function (data) {
	    var msg = JSON.parse(data);
	    assert.equal('executing_job', msg.action, "Host expects executing_job action from client.");
	    callback();
	  });
	};

function assertTxJobFinished(client, callback) {
  mock_io.once('data', function (data) {
    var msg = JSON.parse(data);
    assert.equal('job_finished', msg.action, "Host expects job_finished action from client.");
    callback();
  });
}

function assertTxClientKilled(client, callback) {
  mock_io.once('data', function (data) {
    var msg = JSON.parse(data);
    assert.equal('client_killed', msg.action, "Host expects client_killed action from client.");
    callback();
  });
  send_to_client(client, {action:'kill', content:'SIGKILL'});
}

function assertNumberOfUsersReceived(client, callback) {
  mock_io.once('data', function (data) {
    var msg = JSON.parse(data);
    assert.equal('number_of_users', msg.action, "Host expects number_of_users action from client.");
    callback();
  });
}

describe('Client state machine.', function() {
  var client = null;

  beforeEach(function(done) {
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
    client.setHostSend(mock_io, mock_write);

    // Startup servers
    helper.initJobServer(function() {
      helper.flushDB(function() {
        helper.initPMServer();
        done();
      });
    });
  });

  afterEach(function(done) {
    helper.flushDB(done);
  });


  it('Should be able to execute a complete cicle.', function(done) {
    helper.addJobFromPm(function(err) {
      assertGetKey(client, function() {
        assertKeyAccepted(client, function(){
          assertNumberOfUsersReceived(client, function() {
            assertTxJobReceived(client, function(){
              assertExecutingJob(client, function(){
                assertTxJobFinished(client, function(){
                  assertTxClientKilled(client, done);
                });
              });
            });
          });
        });
      });
    });
  });

});
