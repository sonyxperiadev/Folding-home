/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var consts_tests = require("./Util/constants_tests.js");
var Client = require(consts_tests.PATH_MAIN_CLIENT);
var path = require('path');
var helper = require(path.resolve(__dirname,
  consts_tests.PATH_JOBSERVER_TEST_HELPER));

describe('Client execute tests', function() {

  var client = new Client();

	before(function(done) {
    client.setUUID("XXX-XXX");
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

  it('Should get a job and next timout should be null.', function(done) {
		helper.addJobFromPm(function(err) {
      client.execute(null, helper.getJobDescriptionOptions(), function (err, script, timeout){
        assert.equal(err, null);
        assert.equal(timeout, null);
        assert.notEqual(script, null);
        done();
      });
    });
  });

  it('Should not get a job when options are wrong.', function(done) {
		helper.addJobFromPm(function(err) {
      var options = helper.getJobDescriptionOptions();
      options.port = 1234;
      client.execute(null, options, function (err, script, timeout){
        assert.notEqual(err, null);
        assert.notEqual(timeout, null);
        assert.equal(script, null);
        done();
      });
    });
  });

  it('Should not get a job when PM server is down.', function(done) {
		helper.addJobFromPm(function(err) {
      helper.closePMServer();
      client.execute(null, helper.getJobDescriptionOptions(), function (err, script, timeout){
        assert.notEqual(err, null);
        assert.notEqual(timeout, null);
        assert.equal(script, null);
        done();
      });
    });
  });
});
