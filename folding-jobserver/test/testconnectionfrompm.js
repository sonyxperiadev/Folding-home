/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var server = require('../src/server.js');
var https = require('https');
var fs = require('fs');
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

	it('Using the correct certificate/ca/key, should be able to push a job.', function(done) {
		helper.addJobRemote(helper.getPostJobOptions(), helper.data, function(err) {
			assert.ifError(err);
			done();
		});
	});

	it('Using a wrong key certificate, should not be able to push a job.', function(done) {
		var postJobOptions = helper.getPostJobOptions();
		postJobOptions.key = '';
		helper.addJobRemote(postJobOptions, helper.data, function(err) {
			assert.notEqual(err, null);
			done();
		});
	});


	it('Using a wrong certificate, should not be able to push a job.', function(done) {
		var postJobOptions = helper.getPostJobOptions();
		postJobOptions.cert = [''];
		helper.addJobRemote(postJobOptions, helper.data, function(err) {
			assert.notEqual(err, null);
			done();
		});
	});

	it('Using a wrong ca certificate, should not be able to push a job.', function(done) {
		var postJobOptions = helper.getPostJobOptions();
		postJobOptions.ca = [''];
		helper.addJobRemote(postJobOptions, helper.data, function(err) {
			assert.notEqual(err, null);
			done();
		});
	});

	it('Using a wrong path should not be able to push a job.', function(done) {
		var postJobOptions = helper.getPostJobOptions();
		postJobOptions.path = '/wrongPath';
		helper.addJobRemote(postJobOptions, helper.data, function(err) {
			assert.notEqual(err, null);
			done();
		});
	});

});
