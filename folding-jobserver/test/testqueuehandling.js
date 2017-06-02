/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var server_consts = require('../src/consts_server.js');
var assert = require('assert');
var server = require('../src/server.js');
var https = require('https');
var fs = require('fs');
var helper = require('./Util/jobservertesthelper.js');

describe('Queue Handling tests', function() {

  before(function(done) {
    helper.initJobServer(function() {
      helper.flushDB(done);
    });
  });

  afterEach(function(done) {
    helper.flushDB(done);
  });

  var expectedDescription2 = helper.getExpectedDescription();
  expectedDescription2.path = 'script2';
  expectedData2 = JSON.stringify(expectedDescription2);

  var postJobOption2 = helper.getPostJobOptions();

  postJobOption2.headers = {
    'Content-Type': 'application/x-www-form-urlencoded',
    'Content-Length': expectedData2.length
  };

  var checkJobDescriptionFormat = function(actual, expected) {
    assert.notEqual(actual, undefined);
    assert.equal(actual.path, expected.path);
    assert.equal(actual.client_key, expected.client_key);
    assert.equal(actual.client_cert, expected.client_cert);
  };

  it('Should not return a jobdescription', function(done) {
    helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, data) {
      if (err) {
        if (err.http_code === server_consts.STATUS_CODE_EMPTY_QUEUE)
          done();
        else
          done(err);
      } else {
        done(new Error('Queue not empty'));
      }
    });
  });


  //TODO rewrite this test
  it('should to return the jobs in the same way it were posted', function(done) {
    helper.addJobRemote(postJobOption2, expectedData2, function(err) {
      assert.ifError(err);
      helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
        assert.ifError(err);
        helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, data) {
          assert.ifError(err);
          checkJobDescriptionFormat(data, expectedDescription2);
          helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, data) {
            assert.ifError(err);
            checkJobDescriptionFormat(data, helper.getExpectedDescription());
            done();
          });
        });
      });
    });
  });

  it('after N jobs added and after N retrieved jobs the queue should be empty.', function(done) {
    this.timeout(10 * 1000); //This test need to have a bigger timeout, the default one can break (2s);

    var numberOfExpectedJobs = Math.floor(Math.random() * 10 + 1); //(1 <= N <=10)

    expectedDescription2.job_count = numberOfExpectedJobs;
    expectedData2 = JSON.stringify(expectedDescription2);
    postJobOption2.headers = {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': expectedData2.length
    },

    helper.addJobRemote(postJobOption2, expectedData2, function(err) {
      if (err)
        done(err);
    });

    var retrievedJobs = 0;
    var removeJob = function() {
      helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, data) {
        if (err) {
          if (err.http_code === server_consts.STATUS_CODE_EMPTY_QUEUE && retrievedJobs === numberOfExpectedJobs)
            done();
          else
            done(err);
        } else {
          retrievedJobs++;
          setTimeout(removeJob, 0);
        }
      });
    };

    // We give 1s to the jobserver add all the jobs to it's queue before we start returning the jobs.
    setTimeout(removeJob, 1000);
  });

});
