/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var consts_tests = require("./Util/constants_tests.js");
var Client = require(consts_tests.PATH_MAIN_CLIENT);
var path = require('path');
var nock = require('nock');
var helper = require(path.resolve(__dirname,
  consts_tests.PATH_JOBSERVER_TEST_HELPER));

describe('Get Job from PmServer: ', function() {

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

  it('Should have a job script', function(done) {
    helper.addJobFromPm(function(err) {
      client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        if (err)
          done(err);
        else {
          client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
            client.getJob(jobDesc, attributes, null, function(err, script) {
              assert.notEqual(script, undefined);
              done();
            });
          });
        }
      });
    });
  });

  it('If the request options are wrong, should get no script.', function(done) {
    helper.addJobFromPm(function(err) {
      client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        if (err)
          done(err);
        else {
          client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
            attributes.server_port = 12345;
            client.getJob(jobDesc, attributes, null, function(err, script) {
              assert.equal(script, null);
              assert.notEqual(err, null);
              done();
            });
          });
        }
      });
    });
  });

  it('If the response status code is different from 200 should not get script.', function(done) {
    helper.addJobFromPm(function(err) {
      client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        if (err)
          done(err);
        else {
          client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
            nock('https://localhost:23456').get('/script').reply(404, 'No job for you');
            client.getJob(jobDesc, attributes, null, function(err, script) {
              assert.equal(script, null);
              assert.notEqual(err, null);
              done();
            });
          });
        }
      });
    });
  });


  var checkJobDescriptionFormat = function(data) {

    var dataDescription = data;

    assert.notEqual(dataDescription.path, undefined, 'retrieved job description should have a path key');
    assert.notEqual(dataDescription.client_key, undefined, 'retrieved job description should have a clientkey key');
    assert.notEqual(dataDescription.client_cert, undefined, 'retrieved job description should have a clientcert key');
    assert.notEqual(dataDescription.project_public_certificate, undefined, 'retrieved job description should have a project_public_certificate key');
    assert.notEqual(dataDescription.statistics, undefined, 'retrieved job description should have a statistics key');
  };

  it('If the format is wrong, job should be rejected.', function(done) {
    helper.addJobFromPm(function(err) {
      client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        if (err)
          done(err);
        else {
          client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
            jobDesc.port = 12345;
            client.getJob(jobDesc, attributes, null, function(err, script) {
              checkJobDescriptionFormat(jobDesc);
              done();
            });
          });
        }
      });
    });
  });
});
