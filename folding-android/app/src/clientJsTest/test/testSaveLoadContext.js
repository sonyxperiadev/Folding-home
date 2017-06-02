/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
var fs = require("fs");
var assert = require("assert");
var consts_tests = require("./Util/constants_tests.js");
var Client = require(consts_tests.PATH_MAIN_CLIENT);
var client_consts =  require(consts_tests.PATH_CONSTANTS_CLIENTE);

var path = require('path');

describe("Client Save Context", function(){
  var script = fs.readFileSync('test/Util/scriptSaveContext.js').toString();

  function getCurrentContext() {
    var context = null;
    try {
      context = JSON.parse(fs.readFileSync(consts_tests.PATH_CLIENT_ASSETS +
          client_consts.JOB_SCRIPT_CONTEXT_PATH).toString());
    } catch (e) {
      context = null;
    }
    return context;
  }

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

  function getJobAttributes() {
    var attributes = null;
    try {
      attributes = JSON.parse(fs.readFileSync(consts_tests.PATH_CLIENT_ASSETS +
          client_consts.JOB_ATTRIBUTES_PATH).toString());
    } catch (e) {
      attributes = null;
    }
    return attributes;
  }

  it("Should be able to save, kill and load job script", function(done) {
    var client = new Client();
    assert.ok(createJobAttributes(client_consts.JOB_ATTRIBUTES_PATH), "Creating job attributes failed.");
    client.runJob(script, function(code) {});
    setTimeout(function() {
      client.actionKill();
      setTimeout(function() {
        assert.equal(getCurrentContext().value, 1, "Script should save context");
        assert.notEqual(getJobAttributes(), null, "Job attributes should be saved");
        client.runJob(script, function(code) {});
        setTimeout(function() {
          client.actionKill();
          setTimeout(function() {
            assert.equal(getCurrentContext().value, 2, "Script should save context");
            assert.notEqual(getJobAttributes(), null, "Job attributes should be saved");
            client.runJob(script, function(code) {});
            setTimeout(function() {
              assert.equal(getCurrentContext(), null, "After job finished, context should be removed");
              assert.equal(getJobAttributes(), null, "After job finished, Job attributes should be removed");
              done();
            }, 100);
          }, 100);
        }, 100);
      }, 100);
    }, 100);
  });


  it("Should not save when exceeds storage limit", function(done) {
    var client = new Client();
    assert.ok(createJobAttributes('storage_exceeded_' + client_consts.JOB_ATTRIBUTES_PATH), "Creating job attributes failed.");
    client.runJob(script, function(code) {});
    setTimeout(function() {
      client.actionKill();
      setTimeout(function() {
        assert.equal(getCurrentContext(), null, "Script should not save context when storage exceeds limit.");
        assert.equal(getJobAttributes(), null, "After a violation, job attributes should be removed");
        client.clearPausedState();
        done();
      }, 100);
    }, 100);
  });
});
