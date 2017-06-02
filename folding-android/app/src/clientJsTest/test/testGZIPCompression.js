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

describe("Client GZIP", function(){

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

  var data;

  it("Should be able to get a job description using gzip accept-encoding", function(done) {
    helper.addJobFromPm(function(err) {
      client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        assert.equal(err, null);
        assert.equal(encoding, 'gzip');
        client.parseJobDescription(encoding, buffer, function(err, jobDesc) {
          assert.equal(err, null);
          assert.notEqual(jobDesc, undefined);
          done();
        });
      });
    });
  });

  it("Should be able to get a job using gzip accept-encoding", function(done) {
    helper.addJobFromPm(function(err) {
      client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        assert.equal(err, null);
        assert.equal(encoding, 'gzip');
        client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
          assert.equal(err, null);
          assert.notEqual(jobDesc, undefined);
          var headers = { 'Accept-Encoding': 'gzip' };
          client.getJob(jobDesc, attributes, headers, function(err, script) {
            assert.notEqual(script, null);
            assert.equal(err, null);
            // console.log(script);
            done();
          });
        });
      });
    });
  });

  it("Should be able to get a job using gzip accept-encoding", function(done) {
    helper.addJobFromPm(function(err) {
      client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        assert.equal(err, null);
        assert.equal(encoding, 'gzip');
        client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
          assert.equal(err, null);
          assert.notEqual(jobDesc, undefined);
          client.getJob(jobDesc, attributes, null, function(err, script) {
            assert.notEqual(script, null);
            assert.equal(err, null);
            done();
          });
        });
      });
    });
  });

  it("Should be able to get a job description not using gzip accept-encoding", function(done) {
    var opts = helper.getJobDescriptionOptions();
    delete opts["headers"];

    helper.addJobFromPm(function(err) {
      client.getJobDescription(opts, function(err, encoding, buffer) {
        assert.equal(err, null);
        assert.equal(encoding, undefined)
        client.parseJobDescription(encoding, buffer, function(err, jobDesc) {
          assert.equal(err, null);
          assert.notEqual(jobDesc, undefined);
          done();
        });
      });
    });
  });

  it("parseJobDescription(...) Should not break when a bad formatted buffer is passed", function(done) {
    var buffer = 'ZZZZzZZzZZZ';
    var encoding = 'gzip';

    client.parseJobDescription(encoding, buffer, function(err, jobDesc) {
      assert.notEqual(err, null);
      assert.equal(jobDesc, undefined);
      done();
    });
  });

});
