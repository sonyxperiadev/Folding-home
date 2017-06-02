/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var https = require('https');
var helper = require('./Util/jobservertesthelper.js');

describe('Report Violations', function() {

  before(function(done) {
    helper.initJobServer(function() {
      helper.flushDB(done);
    });
  });

  afterEach(function(done) {
    helper.flushDB(done);
  });

  it('should reply with 200 on the /report path', function(done) {
    helper.postReport(null, done);
  });
});
