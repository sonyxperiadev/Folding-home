/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require("assert");
var consts_tests = require("./Util/constants_tests.js");
var Client = require(consts_tests.PATH_MAIN_CLIENT);
var path = require('path');
var helper = require(path.resolve(__dirname,
  consts_tests.PATH_JOBSERVER_TEST_HELPER));

describe("Client Job Description with secure connection tests.", function(){

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

  it("Should get a job description even with insecure connection.", function(done) {
    helper.addJobFromPm(function(err) {
      client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        assert.equal(err, null);
        client.parseJobDescription(encoding, buffer, function(err, jobDesc) {
          assert.equal(err, null);
          assert.notEqual(jobDesc, undefined);
          done();
        });
      });
    });
  });


  it("Should get a job description using a secure connection.", function(done) {
    helper.addJobFromPm(function(err) {
        client.getJobDescription(helper.getJobDescriptionOptionsSSL, function(err, encoding, buffer) {
          assert.equal(err, null);
          client.parseJobDescription(encoding, buffer, function(err, jobDesc) {
            assert.equal(err, null);
            assert.notEqual(jobDesc, undefined);
            done();
        });
      });
    });
  });

  it("Using a wrong key, should get a job description.", function(done) {
    var descriptionOptions = JSON.parse(JSON.stringify(helper.getJobDescriptionOptionsSSL));
    descriptionOptions.key = "";
    helper.addJobFromPm(function(err) {
        client.getJobDescription(descriptionOptions, function(err, encoding, buffer) {
          assert.equal(err, null);
          client.parseJobDescription(encoding, buffer, function(err, jobDesc) {
            assert.equal(err, null);
            assert.notEqual(jobDesc, undefined);
            done();
        });
      });
    });
  });


  it("Using a wrong certificate, should not get a job description.", function(done) {
    var descriptionOptions = JSON.parse(JSON.stringify(helper.getJobDescriptionOptionsSSL));
    descriptionOptions.ca = descriptionOptions.cert;
    helper.addJobFromPm(function(err) {
        client.getJobDescription(descriptionOptions, function(err, encoding, buffer) {
          assert.notEqual(err, null);
          assert.equal(encoding, null);
          assert.equal(buffer, undefined);
          done();
      });
    });
  });
});
