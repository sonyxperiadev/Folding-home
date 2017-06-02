/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var consts_tests = require("./Util/constants_tests.js");
var Client = require(consts_tests.PATH_MAIN_CLIENT);
var path = require('path');
var fs = require('fs');
var createServerScript = fs.readFileSync(__dirname + '/Util/createserver.js');
var requestInvalidHost = fs.readFileSync(__dirname + '/Util/requestblacklisthost.js');

describe('Run job tests', function() {
  var client = new Client();

  it('A clean script should run and exit code should be 0', function(done) {
    client.runJob("var a = 10; var b = 10; var c =  a + b;", function(code) {
      assert.equal(code, 0, "Code should be 0");
      done();
    });
  });

  it('A script should be able to require the https module', function(done) {
    client.runJob("var https = require('https');", function(code) {
      assert.equal(code, 0, "Code should be 0");
      done();
    });
  });

  it('Should not run a script with non allowed requires', function(done) {
    client.runJob("var m = require('module')", function(code) {
      assert.notEqual(code, 0, "The code should be different than zero");
      done();
    });
  });

  it('Should not run a script with try to create a server', function(done) {
    client.runJob(createServerScript, function(code) {
      assert.notEqual(code, 0, "The code should be different than zero");
      done();
    });
  });

  it('Should not run a script which try to contact a non white listed host', function(done) {
    client.runJob(requestInvalidHost, function(code) {
      assert.notEqual(code, 0, "The code should be different than zero");
      done();
    });
  });

});
