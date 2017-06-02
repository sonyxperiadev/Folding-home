/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var helper = require('./Util/jobservertesthelper.js');
var consts = require('../src/consts_server.js');

describe('Data Description tests', function() {

  before(function(done) {
    helper.initJobServer(function() {
      helper.flushDB(done);
    });
  });

  afterEach(function(done) {
    helper.flushDB(done);
  });

  function checkJobDescriptionFormat(dataDescription) {
    assert.notEqual(dataDescription, undefined);
    assert.equal(Object.keys(dataDescription).length, 5, 'the job description should have exactly 5 keys');

    assert.notEqual(dataDescription.path, undefined, 'retrieved job description should have a path key');
    assert.notEqual(dataDescription.client_key, undefined, 'retrieved job description should have a clientkey key');
    assert.notEqual(dataDescription.client_cert, undefined, 'retrieved job description should have a clientcert key');
    assert.notEqual(dataDescription.project_public_certificate, undefined, 'retrieved job description should have a project public certificate key');
    assert.notEqual(dataDescription.statistics, undefined, 'retrieved job description should have a statistics key');
  }

  it('should be able to add (remote) and retrieve (remote) a jobdescription', function(done) {
    helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
      assert.ifError(err);
      helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, data) {
        assert.ifError(err);
        checkJobDescriptionFormat(data);
        helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, data) {
          assert.equal(err.http_code, consts.STATUS_CODE_NO_CONTENT);
          assert.equal(data, null);
          done();
        }, {category: null, UUID: null});
      });
    });
  });

  it('should not be able to retrieve a jobdescription of an unknown category', function(done) {
    var jobDescription = helper.getExpectedDescription();
    jobDescription.secure = true;
    var data = JSON.stringify(jobDescription);
    var postJobOptions = helper.getPostJobOptions();
    postJobOptions.headers = {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': data.length
    };
    helper.addJobRemote(postJobOptions, data, function(err) {
      assert.ifError(err);
      helper.getJobRemote(helper.getJobDescriptionOptionsSSL, function(err, data) {
        assert.equal(err.http_code, consts.STATUS_CODE_EMPTY_QUEUE);
        done();
      }, {category: 'unknownCategory', UUID: 'uuid'});
    });
  });

});
