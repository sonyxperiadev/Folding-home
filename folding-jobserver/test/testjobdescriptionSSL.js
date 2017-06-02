/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var helper = require('./Util/jobservertesthelper.js');


describe('Job Description with secure connection tests.', function() {

	before(function(done) {
		helper.initJobServer(function() {
			helper.flushDB(done);
		});
	});

	afterEach(function(done) {
		helper.flushDB(done);
	});

	it('Should get a job description even with insecure connection.', function(done) {
		helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
			helper.getJobRemote(helper.getJobDescriptionOptions(), function(err, data) {
				assert.ifError(err);
				assert.notEqual(data, undefined);
				done();
			});
		});
	});


	it.skip('Should get a job description using a secure connection.', function(done) {
    var jobDescription = helper.getExpectedDescription();
    jobDescription.secure = true;
    var data = JSON.stringify(jobDescription);
    var postJobOptions = helper.getPostJobOptions();
    postJobOptions.headers = {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': data.length
    };
    helper.addJobRemote(postJobOptions, data, function(err) {
      helper.getJobRemote(helper.getJobDescriptionOptionsSSL, function(err, data) {
				assert.ifError(err);
				assert.notEqual(data, undefined);
				done();
			});
		});
	});

	it('Using a wrong certificate, should not get a job description.', function(done) {
		helper.getJobDescriptionOptionsSSL.ca = helper.getJobDescriptionOptionsSSL.cert;
		helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
			helper.getJobRemote(helper.getJobDescriptionOptionsSSL, function(err, data) {
				assert.notEqual(err, null);
				assert.equal(data, undefined);
				done();
			});
		});
	});
});
