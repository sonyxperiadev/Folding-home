/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var consts_tests = require("./Util/constants_tests.js");
var Client = require(consts_tests.PATH_MAIN_CLIENT);
var client_consts = require(consts_tests.PATH_CONSTANTS_CLIENTE);
var fs = require('fs');

describe('Paused Job Expiration Limit Tests', function() {

  var client = new Client();

  function createCurrentScript(scriptName) {
    var ok;

    try {
      var content = fs.readFileSync(__dirname + '/Util/' + scriptName );
      fs.writeFileSync(consts_tests.PATH_CLIENT_ASSETS +
        client_consts.CURRENT_SCRIPT_PATH, content);
      ok = true;
    } catch (e) {
      ok = false;
    }

    return ok;
  };

  function createJobAttributes(attributesName) {
    var ok;

    try {
      var content = fs.readFileSync(__dirname + '/Util/' + attributesName );
      fs.writeFileSync(consts_tests.PATH_CLIENT_ASSETS +
        client_consts.JOB_ATTRIBUTES_PATH, content);
      ok = true;
    } catch (e) {
      ok = false;
    }

    return ok;
  };

  afterEach(function() {
    client.clearPausedState();
  });

  it('Should get a script when the limits are ok', function() {
    assert.ok(createCurrentScript(client_consts.CURRENT_SCRIPT_PATH), "Creating current script failed.");
    assert.ok(createJobAttributes(client_consts.JOB_ATTRIBUTES_PATH), "Creating job attributes failed.");
    var script = client.getPausedScript();
    assert.notEqual(script, null, 'Script should not be null');
    assert.notEqual(script, undefined, 'Script should not be undefined');
  });

  it('Should not get a script when run_time_limite is expired', function() {
    assert.ok(createCurrentScript(client_consts.CURRENT_SCRIPT_PATH), "Creating current script failed.");
    assert.ok(createJobAttributes('run_time_expired_' + client_consts.JOB_ATTRIBUTES_PATH), "Creating job attributes failed.");
    var script = client.getPausedScript();
    assert.equal(script, null, 'Script should be null');
  });

  it('Should not get a script when execution_time_limit is expired', function() {
    assert.ok(createCurrentScript(client_consts.CURRENT_SCRIPT_PATH), "Creating current script failed.");
    assert.ok(createJobAttributes('execution_time_limit_' + client_consts.JOB_ATTRIBUTES_PATH), "Creating job attributes failed.");
    var script = client.getPausedScript();
    assert.equal(script, null, 'Script should be null');
  });

  it('Should not get a script if remaining_run_time key is missing', function() {
    assert.ok(createCurrentScript(client_consts.CURRENT_SCRIPT_PATH), "Creating current script failed.");
    assert.ok(createJobAttributes('missing_remaining_run_time_' + client_consts.JOB_ATTRIBUTES_PATH), "Creating job attributes failed.");
    var script = client.getPausedScript();
    assert.equal(script, null, 'Script should be null');
  });

  it('Should not get a script if expiry_date key is missing', function() {
    assert.ok(createCurrentScript(client_consts.CURRENT_SCRIPT_PATH), "Creating current script failed.");
    assert.ok(createJobAttributes('missing_expiry_date_' + client_consts.JOB_ATTRIBUTES_PATH), "Creating job attributes failed.");
    var script = client.getPausedScript();
    assert.equal(script, null, 'Script should be null');
  });

  it('Should not get a script if the job attributes is not a JSON', function() {
    assert.ok(createCurrentScript(client_consts.CURRENT_SCRIPT_PATH), "Creating current script failed.");
    assert.ok(createJobAttributes('bad_format_json_' + client_consts.JOB_ATTRIBUTES_PATH), "Creating job attributes failed.");
    var script = client.getPausedScript();
    assert.equal(script, null, 'Script should be null');
  });

});
