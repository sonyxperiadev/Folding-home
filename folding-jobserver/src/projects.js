/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var fs = require('fs');
var resPath = __dirname + '/';

var certificates = {
  'localhost':
      fs.readFileSync(resPath + '../test/Util/pmserver-cert.pem').toString(),
};

function getProjectCertificates() {
  var certs = [];
  Object.keys(certificates).forEach(function(key) {
    certs.push(certificates[key]);
  });
  return certs;
}


/**
 * Exports
 */
module.exports = {
  certificates: certificates,
  getProjectCertificates: getProjectCertificates
};
