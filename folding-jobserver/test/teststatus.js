/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var helper = require('./Util/jobservertesthelper.js');
var consts = require('../src/consts_server.js');

describe('Job Server', function() {

  before(function(done) {
    helper.initJobServer(function() {
      helper.flushDB(done);
    });
  });

  afterEach(function(done) {
    helper.flushDB(done);
  });

  it('Should get server status with no jobs queued.', function(done) {
    helper.getServerStatus(helper.getQueueStatusOptions(), function(err, data) {
      assert.equal(err, null);
      assert.equal(JSON.stringify(data), '{}');
      done();
    });
  });

  it('Should get server status with jobs queued.', function(done) {
    helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
      helper.getServerStatus(helper.getQueueStatusOptions(), function(err, data) {
        assert.equal(err, null);
        assert.equal(data['default:insecure'], 1);
        done();
      });
    });
  });

  it('The status should report {} if the queues are drained', function(done) {
    helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
      helper.getServerStatus(helper.getQueueStatusOptions(), function(err, data) {
        assert.equal(err, null);
        assert.equal(data['default:insecure'], 1);
        helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, data) {
          assert.ifError(err);
          helper.getServerStatus(helper.getQueueStatusOptions(), function(err, data) {
            assert.equal(err, null);
            assert.equal(JSON.stringify(data), '{}');
            done();
          });
        });
      });
    });
  });

  it('Should not response for different paths.', function(done) {
    var statusOptions = helper.getQueueStatusOptions();
    statusOptions['path'] = '/';
    helper.getServerStatus(statusOptions, function(err, data) {
      assert.notEqual(err, null);
      statusOptions['path'] = '/anything';
      helper.getServerStatus(statusOptions, function(err, data) {
        assert.notEqual(err, null);
        done();
      });
    });
  });

  it('Healthcheck with server online should have returned status code 200.', function(done) {
    var statusOptions = helper.getStatusOptions();
    helper.getServerStatus(statusOptions, function(err, data) {
      assert.equal(err, null);
      done();
    });
  });

});
