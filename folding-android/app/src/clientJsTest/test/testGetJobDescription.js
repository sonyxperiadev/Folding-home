/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require("assert");
var consts_tests = require("./Util/constants_tests.js");
var Client = require(consts_tests.PATH_MAIN_CLIENT);
var path = require('path');
var nock = require('nock');
var helper = require(path.resolve(__dirname,
  consts_tests.PATH_JOBSERVER_TEST_HELPER));
var consts = require(path.resolve(__dirname,
  consts_tests.PATH_JOBSERVER_CONSTANTS_HELPER_TEST));

describe("Job description.", function(){

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

  it("If the server has no jobs, should not get a job description.", function(done) {
    client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
      assert.notEqual(err, null);
      done();
    });
  });

  it("Should get a job description.", function(done) {
    helper.addJobFromPm(function(err) {
      client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        assert.equal(err, null);
        client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
          assert.equal(err, null);
          assert.notEqual(jobDesc, null);
          assert.notEqual(attributes, null);
          data = jobDesc;
          done();
        });
      });
    });
  });

  it("Should have a path key.", function() {
    assert.notEqual(data.path, undefined);
  });

  it("Should have a clientkey key.", function() {
    assert.notEqual(data.client_key, undefined);
  });

  it("Should have a clientcert key.", function() {
    assert.notEqual(data.client_cert, undefined);
  });

  it("Should have a project_public_certificate key.", function() {
    assert.notEqual(data.project_public_certificate, undefined);
  });

  it("Should have a statistics key.", function() {
    assert.notEqual(data.statistics, undefined);
  });

  it.skip("If the request options are wrong, should not get a job description.", function(done) {
    var reqOptions = {
      //Server details
      host: 's0ny-2.embedded.ufcg.edu.br',
      port: client.PORT_JOB_SERVER
    }

    client.getJobDescription(reqOptions, function(err, encoding, buffer) {
      assert.notEqual(err, null);
      done();
    });
  });

  it('If the response status code is different from 200 should not get script.', function(done) {
    nock('https://localhost:' + consts.PORT_JOB_SERVER_TESTS_NGINX).post('/getjob').reply(404, 'No job description for you');
    client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
        assert.notEqual(err, null);
        assert.equal(encoding, null);
        assert.equal(buffer, null);
        done();
    });
  });

  it("If the project public certificate is bad formated, should get an error.", function(done) {
    nock('https://localhost:' + consts.PORT_JOB_SERVER_TESTS_NGINX).post('/getjob').reply(200,
        JSON.stringify(helper.getSimulatedJobDescription()));

    client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
      assert.equal(err, null);
      client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
        assert.equal(err.message, 'Public Project Certificate in bad format.');
        assert.equal(jobDesc, null);
        assert.equal(attributes, null);
        done();
      });
    });
  });

  it("If the job description is not a String (JSON) should get an error.", function(done) {
    nock('https://localhost:' + consts.PORT_JOB_SERVER_TESTS_NGINX).post('/getjob').reply(200,
        helper.getSimulatedJobDescription());

    client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
      assert.equal(err, null);
      client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
        assert.equal(err.message, 'Error while parsing the job description.');
        assert.equal(jobDesc, null);
        assert.equal(attributes, null);
        done();
      });
    });
  });

  it("If the job description is bad formated, should get an error.", function(done) {
    var simulatedJobDescription = helper.getSimulatedJobDescription();
    delete simulatedJobDescription['path'];
    nock('https://localhost:' + consts.PORT_JOB_SERVER_TESTS_NGINX).post('/getjob').reply(200,
        JSON.stringify(simulatedJobDescription));

    client.getJobDescription(helper.getJobDescriptionOptions(), function(err, encoding, buffer) {
      assert.equal(err, null);
      client.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes) {
        assert.equal(err.message, 'Job description in bad format.');
        assert.equal(jobDesc, null);
        assert.equal(attributes, null);
        done();
      });
    });
  });
});
