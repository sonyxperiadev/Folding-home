/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var consts = require('./consts_helper_tests.js');
var consts_server = require('../../src/consts_server.js');
var server = require('../../src/server.js');
var jobserver_pmserver = require('../../src/jobserver_pmserver.js');
var job_queue = require('../../src/jobs_queue_redis.js');
var pmserver = require('./clpmserver.js');
var https = require('https');
var fs = require('fs');
var zlib = require('zlib');
var analyticshelper = require('../../src/analytics.js');

var isUp = false;
var isPMUp = false;

var resPath = __dirname + '/';

var serverport = consts.PORT_SERVER_DESC_TESTS;
var serveraddress = 'localhost';

var clientkey = fs.readFileSync(resPath + 'pmclient-key.pem').toString();
var clientcert = fs.readFileSync(resPath + 'pmclient-cert.pem').toString();
var clientca = [fs.readFileSync(resPath + 'pmclient-cert.pem')].toString();

function getExpectedDescription() {
  var expectedDescription = {
    path: '/script',
    client_key: clientkey,
    client_cert: clientcert,
    job_count: 1,
    secure: false,
  };
  return expectedDescription;
}
var data = JSON.stringify(getExpectedDescription());

function getSimulatedJobDescription() {
  var simulatedDescription = {
    path: '/script',
    client_key: clientkey,
    client_cert: clientcert,
    project_public_certificate: fs.readFileSync(resPath + 'pmserver-cert.pem').toString(),
    statistics: {number_of_users: 1234}
  };
  return simulatedDescription;
}

var jobserver = {
  address: 'localhost',
  port: consts.PORT_PM_SERVER_TESTS_NGINX,
  key: fs.readFileSync(resPath + 'pmserver-key.pem').toString(),
  cert: fs.readFileSync(resPath + 'pmserver-cert.pem').toString(),
  ca: [fs.readFileSync(resPath + 'jobserver-cacert.pem').toString()]
};

function getJobDescriptionOptions() {
  var jobDescriptionOptions = {
    headers: { 'Accept-Encoding': 'gzip', 'Cache-Control': 'no-cache' },
    host: 'localhost',
    port: consts.PORT_JOB_SERVER_TESTS_NGINX,
    path: '/getjob',
    method: 'POST',
    agent: false,
    ca: jobserver.ca,
    rejectUnauthorized: true
  };
  return jobDescriptionOptions;
}

var postReportOptions = {
  host: 'localhost',
  port: consts.PORT_JOB_SERVER_TESTS_NGINX,
  path: '/report',
  method: 'POST',
  agent: false,
  rejectUnauthorized: false
};

var getJobDescriptionOptionsSSL = {
  headers: { 'Accept-Encoding': 'gzip', 'Cache-Control': 'no-cache' },
  host: 'localhost',
  port: consts.PORT_JOB_SERVER_TESTS_NGINX,
  path: '/getjob',
  method: 'POST',
  agent: false,
  key: fs.readFileSync(resPath + 'client-key.pem').toString(),
  cert: fs.readFileSync(resPath + 'client-cert.pem').toString(),
  ca: jobserver.ca,
  passphrase: 'clientkey',
  rejectUnauthorized: true
};

function getPostJobOptions() {
  var postJobOptions = {
    host: jobserver.address,
    port: jobserver.port,
    ca: jobserver.ca,
    key: jobserver.key,
    cert: jobserver.cert,
    requestCert: true,
    rejectUnauthorized: true,
    path: '/job',
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': data.length
    },
    agent: false
  };
  return postJobOptions;
}

function getQueueStatusOptions() {
  var queueStatusOptions = {
    host: 'localhost',
    port: consts.PORT_JOB_SERVER_TESTS_NGINX,
    path: '/queue_status',
    method: 'GET',
    agent: false,
    rejectUnauthorized: false
  };
  return queueStatusOptions;
}

function getStatusOptions() {
  var statusOptions = {
    host: 'localhost',
    port: consts.PORT_JOB_SERVER_TESTS_NGINX,
    path: '/status',
    method: 'GET',
    agent: false,
    rejectUnauthorized: false
  };
  return statusOptions;
}

var flushDB = function(done) {
  job_queue.flushDB(done);
};

var getServerStatus = function(options, callback) {
  var req = https.request(options, function(res) {
    if (res.statusCode != consts_server.STATUS_CODE_OK) {
      var err = new Error('Status code not ' + consts_server.STATUS_CODE_OK
            + '. Got: ' + res.statusCode);
      err.http_code = res.statusCode;
      callback(err, null);
    } else {
      var data = '';
      res.on('data', function(chunk) {
        data += chunk;
      });

      res.on('end', function() {
        // If we have data.. parse from JSON
        if (data) {
          data = JSON.parse(data);
        }
        callback(null, data);
      });

      res.on('error', function(err) {
        callback(err, null);
      });
    }
  });

  req.on('error', function(err) {
    callback(err, null);
  });

  req.end();
};

var getJobRemote = function(reqOptions, callback, data) {
  if (!data)
    data = {
      category: consts.CATEGORY,
      UUID: consts.UUID
    };

  data = JSON.stringify(data);

  reqOptions.headers = {
    'Content-Type': 'application/x-www-form-urlencoded',
    'Content-Length': data.length
  };

  var req = https.request(reqOptions, function(res) {
    if (res.statusCode != consts_server.STATUS_CODE_OK) {
      var err = new Error('Status code not ' + consts_server.STATUS_CODE_OK
                        + '. Got: ' + res.statusCode);
      err.http_code = res.statusCode;
      callback(err, null);
    } else {
      var encoding = res.headers['content-encoding'];

      if (encoding == 'gzip')
        res.setEncoding('base64');
      else
        res.setEncoding('utf8');

      var data = '';

      res.on('data', function(chunk) {
        data += chunk;
      });

      res.on('end', function() {
        if (encoding !== 'gzip') {
          data = JSON.parse(data);
          callback(null, data);
        } else {
          gunzipData(data, function(err, data) {
            callback(err, data);
          });
        }
      });

      res.on('error', function(err) {
        callback(err, null);
      });
    }
  });

  req.on('error', function(err) {
    callback(err, null);
  });

  req.write(data);
  req.end();
};

function getJobRemoteRaw(reqOptions, callback, data) {
  if (!data)
    data = {
      category: consts.CATEGORY,
      UUID: consts.UUID
    };

  data = JSON.stringify(data);

  reqOptions.headers = {
    'Accept-Encoding': reqOptions.headers['Accept-Encoding'],
    'Content-Type': 'application/x-www-form-urlencoded',
    'Content-Length': data.length
  };

  var req = https.request(reqOptions, function(res) {
    if (res.statusCode != consts_server.STATUS_CODE_OK) {
      var err = new Error('Status code not ' + consts_server.STATUS_CODE_OK
                        + '. Got: ' + res.statusCode);
      err.http_code = res.statusCode;
      callback(err, null);
    } else {
      var encoding = res.headers['content-encoding'];

      if (encoding == 'gzip')
        res.setEncoding('base64');
      else
        res.setEncoding('utf8');

      var data = '';

      res.on('data', function(chunk) {
        data += chunk;
      });

      res.on('end', function() {
        callback(null, data)
      });

      res.on('error', function(err) {
        callback(err, null);
      });
    }
  });

  req.on('error', function(err) {
    callback(err, null);
  });

  req.write(data);
  req.end();
};

function gunzipData(data, callback) {
  var buffer = new Buffer(data, 'base64');
    zlib.gunzip(buffer, function(err, decoded) {
      if (!err)
        decoded = JSON.parse(decoded.toString());
      callback(err, decoded);
  });
}

var addJobRemote = function(options, descriptionData, callback) {
  var req = https.request(options, function(res) {
    if (res.statusCode != consts_server.STATUS_CODE_OK) {
      var err = new Error('Status code not ' + consts_server.STATUS_CODE_OK
                        + '. Got: ' + res.statusCode);
      err.http_code = res.statusCode;
      callback(err, null);
    } else {

      var data = '';

      res.on('data', function(chunk) {
        data += chunk;
      });

      res.on('end', function() {
        if (data)
          callback(null, JSON.parse(data));
        else
          callback(null);
      });
    }
  });

  req.on('error', function(err) {
    callback(err, null);
  });

  req.write(descriptionData);
  req.end();
};

function postReport(options, callback) {
  if (!options)
    options = postReportOptions;

  options['headers'] = {
      'Content-Type': 'application/json',
      'Content-Length': ''.length
    };

  var req = https.request(options, function(res) {
    if (res.statusCode != 200) {
      var err = new Error('Status code not ' + consts_server.STATUS_CODE_OK +
                    '. Got: ' + res.statusCode);
      err.http_code = res.statusCode;
      callback(err);
    } else {
      var data = '';
      res.on('data', function(chunk) {
        data += chunk;
      });

      res.on('end', function() {
        callback(null);
      });

      res.on('error', function(err) {
        callback(err);
      });
    }
  });

  req.on('error', function(err) {
    callback(err);
  });

  req.write('');
  req.end();
}

var initJobServer = function(callback) {
  if (!isUp) {
    var pm_connpoint = jobserver_pmserver.startPMConnectionPoint(consts.PORT_PM_SERVER_TESTS);
    pm_connpoint.on('listening', function() {
      var jobserver = server.startJobServer(consts.PORT_JOB_SERVER_TESTS);
      jobserver.on('listening', function() {
        isUp = true;
        callback();
      });
    });
  } else {
    callback();
  }
};

var addJobFromPm = function(callback) {
  pmserver.addJob(function(err) {
    setTimeout(function() {
      callback(err);
    }, 500);
  });
};

var initPMServer = function() {
  if (!isPMUp) {
    pmserver.start();
    isPMUp = true;
  }
};

var closePMServer = function() {
  if (isPMUp) {
    pmserver.close();
    isPMUp = false;
  }
};

module.exports = {
  getJobRemote: getJobRemote,
  getServerStatus: getServerStatus,
  initJobServer: initJobServer,
  addJobRemote: addJobRemote,
  getPostJobOptions: getPostJobOptions,
  getExpectedDescription: getExpectedDescription,
  getJobDescriptionOptions: getJobDescriptionOptions,
  getJobDescriptionOptionsSSL: getJobDescriptionOptionsSSL,
  getQueueStatusOptions: getQueueStatusOptions,
  getStatusOptions: getStatusOptions,
  data: data,
  initPMServer: initPMServer,
  closePMServer: closePMServer,
  addJobFromPm: addJobFromPm,
  analyticshelper: analyticshelper,
  flushDB: flushDB,
  getJobRemoteRaw: getJobRemoteRaw,
  postReport: postReport,
  getSimulatedJobDescription: getSimulatedJobDescription
};
