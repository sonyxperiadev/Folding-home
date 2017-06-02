/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var helper = require('./Util/jobservertesthelper.js');

describe('Job server', function() {

  before(function(done) {
    helper.initJobServer(function() {
      helper.flushDB(done);
    });
  });

  afterEach(function(done) {
    helper.flushDB(done);
  });

  function newJobRequestWithoutKey(key) {
    var jobRequest = helper.getExpectedDescription();
    delete jobRequest[key];
    jobRequest = JSON.stringify(jobRequest);

    return jobRequest;
  }

  function newPostJobOptions(jobRequest) {
    var postJobOptions = helper.getPostJobOptions();
    postJobOptions.headers['Content-Length'] = jobRequest.length;

    return postJobOptions;
  }

  function shouldNotAddHandler(err, done) {
    if (!err) {
      done(new Error('Should not add'));
    } else {
      done();
    }
  }

  it('should reject a job request if it is missing the path key', function(done) {
    var jobRequest = newJobRequestWithoutKey('path');
    var postJobOptions = newPostJobOptions(jobRequest);

    helper.addJobRemote(postJobOptions, jobRequest, function(err) {
      shouldNotAddHandler(err, done);
    });
  });

  it('should reject a job request if it is missing the client_key key', function(done) {
    var jobRequest = newJobRequestWithoutKey('client_key');
    var postJobOptions = newPostJobOptions(jobRequest);

    helper.addJobRemote(postJobOptions, jobRequest, function(err) {
      shouldNotAddHandler(err, done);
    });
  });

  it('should reject a job request if it is missing the client_cert key', function(done) {
    var jobRequest = newJobRequestWithoutKey('client_cert');
    var postJobOptions = newPostJobOptions(jobRequest);

    helper.addJobRemote(postJobOptions, jobRequest, function(err) {
      shouldNotAddHandler(err, done);
    });
  });

  it('should reject a job request if it is missing the job_count key', function(done) {
    var jobRequest = newJobRequestWithoutKey('job_count');
    var postJobOptions = newPostJobOptions(jobRequest);

    helper.addJobRemote(postJobOptions, jobRequest, function(err) {
      shouldNotAddHandler(err, done);
    });
  });

  it('should reject a job request if it is missing the secure key', function(done) {
    var jobRequest = newJobRequestWithoutKey('secure');
    var postJobOptions = newPostJobOptions(jobRequest);

    helper.addJobRemote(postJobOptions, jobRequest, function(err) {
      shouldNotAddHandler(err, done);
    });
  });
});
