/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var server = require('../src/server.js');
var https = require('https');
var fs = require('fs');
var helper = require('./Util/jobservertesthelper.js');
var zlib = require('zlib');

describe('Job Server ', function() {

  before(function(done) {
    helper.initJobServer(function() {
      helper.flushDB(done);
    });
  });

  afterEach(function(done) {
    helper.flushDB(done);
  });

  function assertSameData(data, anotherData) {
    assert.equal(data.server, anotherData.server);
    assert.equal(data.port, anotherData.port);
    assert.equal(data.path, anotherData.path);
    assert.equal(data.client_key, anotherData.client_key);
    assert.equal(data.client_cert, anotherData.client_cert);
    assert.equal(data.category, anotherData.category);
  }

  it('Should send a job description encoded with Gzip.', function(done) {
    helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
      helper.getJobRemoteRaw(helper.getJobDescriptionOptions(), function(err, data) {
        assert.ifError(err);
        var buffer = new Buffer(data, 'base64');
        zlib.gunzip(buffer, function(err, decoded) {
          assert.ifError(err);
          assertSameData(JSON.parse(decoded.toString()), JSON.parse(helper.data.toString()));
          done();
        });
      });
    });
  });

  it('Should send a job description in plain text.', function(done) {
    helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
      var jobDescriptionOptions = helper.getJobDescriptionOptions();
      delete jobDescriptionOptions['headers'];
      helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, data) {
        assert.ifError(err);
        assertSameData(data, JSON.parse(helper.data.toString()));
        done();
      });
    });
  });

  it('Should be able to receive from pm server a job description in plain text.', function(done) {
		helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
      assert.ifError(err);
      helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, jobDesc) {
        assert.ifError(err);
        assertSameData(jobDesc, JSON.parse(helper.data.toString()));
        done();
      });
    });
  });

  it('Should be able to receive from pm server a job description in Gzip.', function(done) {
    var postOptions = helper.getPostJobOptions();
    postOptions.headers['Content-Encoding'] = 'gzip';
    zlib.gzip(helper.data, function(err, encoded) {
      assert.ifError(err);
      postOptions.headers['Content-Length'] = encoded.length;
      helper.addJobRemote(postOptions, encoded, function(err) {
        assert.ifError(err);
        helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, jobDesc) {
          assert.ifError(err);
          var dataJson =
          assertSameData(jobDesc, JSON.parse(helper.data.toString()));
          done();
        });
      });
    });
  });
});
