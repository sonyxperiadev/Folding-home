/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var x509 = require('x509');
var fs = require('fs');
var assert = require('assert');
var projects = require('../src/projects.js');
var consts = require('../src/consts_server.js');
var helper = require('./Util/jobservertesthelper.js');

var resPath = __dirname + '/';

describe('Project Certificates tests', function() {

  before(function(done) {
    helper.initJobServer(function() {
      helper.flushDB(done);
    });
  });

  afterEach(function(done) {
    helper.flushDB(done);
  });

  it('Project Certificates should have required attributes', function(done) {
    var certs = projects.getProjectCertificates();
    certs.forEach(function(cert) {
      var x509Cert = x509.parseCert(cert);
      var extraAttrb = JSON.parse(x509Cert.subject[consts.PROJECT_ATTRIBUTES_OID]);

      assert.notEqual(x509Cert.subject.commonName, undefined , 'Error in commonName');
      assert.notEqual(x509Cert.issuer.commonName, undefined , 'Error in commonName');
      assert.notEqual(extraAttrb.category, undefined,'Error in category');
      assert.notEqual(extraAttrb.server_address, undefined,'Error in server_address');
      assert.notEqual(extraAttrb.server_port, undefined,'Error in server_port');
      assert.notEqual(extraAttrb.max_job_count, undefined,'Error in max_job_count');
      assert.notEqual(extraAttrb.run_time_limit, undefined,'Error in run_time_limit');
      assert.notEqual(extraAttrb.execution_time_limit, undefined,'Error in execution_time_limit');
    });
    done();
  });

  it('should reject a job if the PM is not in the projects list', function(done) {
    var postOptions = helper.getPostJobOptions();
    postOptions.key = fs.readFileSync(resPath + 'Util/pmserver-wrongCN-key.pem').toString();
    postOptions.cert = fs.readFileSync(resPath + 'Util/pmserver-wrongCN-cert.pem').toString();
    helper.addJobRemote(postOptions, helper.data, function(err) {
      assert.equal(err.http_code, consts.STATUS_CODE_FORBIDDEN);
      done();
    });
  });
});
