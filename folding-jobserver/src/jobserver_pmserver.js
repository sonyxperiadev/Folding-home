/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

'use strict';

//We need this to not turning our tests into a mess of logs
var runningFromCmd = require.main === module;
var debug = true;
var jobs_queue;

if (runningFromCmd) {
  jobs_queue = require('./jobs_queue_aws.js');
} else {
  jobs_queue = require('./jobs_queue_redis.js');
}

require('newrelic');
var consts = require('./consts_server.js');
var http = require('http');
var url = require('url');
var analytics = require('./analytics.js');
var projects = require('./projects.js');
var x509 = require('x509');
var logger = require('./logger');

var logLevel;
if (runningFromCmd && debug) {
  logLevel = logger.INFO;
} else if (runningFromCmd) {
  logLevel = logger.WARN;
} else {
  logLevel = logger.OFF;
}

var log;
function setupLog(logFunction) {
  log = new logger.Logger({logLevel: logLevel, logFunction: logFunction});
  jobs_queue.setup(log);
}

setupLog(function(message) {
  console.log('[' + Date() + '] ' + message);
});

//Setup Analytics with our own redisClient.
analytics.startTracking(runningFromCmd);

// Checks if job request is well defined and contains all required fields.
function checkJobRequestFormat(jobRequest) {
  var ok = false;
  var requiredFieldsPresent = jobRequest.path !== undefined &&
      jobRequest.client_key !== undefined &&
      jobRequest.client_cert !== undefined &&
      jobRequest.job_count !== undefined &&
      jobRequest.secure !== undefined &&
      jobRequest.project_id !== undefined &&
      jobRequest.category !== undefined;

  if (requiredFieldsPresent) {
    // Length and limits checking.
    ok = jobRequest.path.length < consts.DATA_JOB_PATH_MAX;
  }
  return ok;
}

function addJob(data, callback) {
  var err;
  var dc = data;
  data = {};

  if (checkJobRequestFormat(dc)) {
    //Clone only accepted fields. Ignore any others.
    data.path = dc.path;
    data.client_key = dc.client_key;
    data.client_cert = dc.client_cert;
    //TODO: id is TBD (e.g. mac of server and port).
    data.project_id = dc.project_id;

    log.info('Adding ' + dc.job_count + ' jobs!');

    var queueKey = dc.category + ':' + (dc.secure ? 'secure' : 'insecure');

    jobs_queue.add(queueKey, data, dc.job_count, function(err) {
      analytics.addJobReceived(dc.job_count);
      if (!err)
        log.info('Jobs added successfully');
      callback(err);
    });
  } else {
    err = new Error();
    err.http_code = consts.STATUS_CODE_NO_CONTENT;
    log.error('Error while adding job. Wrong job format.');
    callback(err, null);
  }
}

//Connection point for a project management server
var pm_server = http.createServer(function(req, res) {
  log.info('Received a request from PM Server.');
  var q = url.parse(req.url, true);
  if (!pm_f.emit(q.pathname, res, req)) {
    log.error('The request was for a wrong path.');
    res.writeHead(consts.STATUS_CODE_NOT_FOUND, 'File Not Found');
    res.end();
  }
});

var startPMConnectionPoint = function(port) {
  pm_server.listen(port);
  pm_server.timeout = consts.TIMEOUT_REQUEST;
  return pm_server;
};

//PM interface server
var pm_f = new (require('events').EventEmitter)();

pm_f.on('/job', function(res, req) {
  if (req.method != 'POST') return;
  log.info('The request (POST) is to add a job.');

  req.setEncoding('utf8');

  req.data = '';

  req.on('data', function(chunk) {
    req.data += chunk;
  });

  req.on('end', function() {
    var jobData = {};
    var addJobCallback = function(err) {
      if (err) {
        res.writeHead(err.http_code);
      } else {
        res.writeHead(consts.STATUS_CODE_OK);
      }

      res.end();
    };

    //Handle input as JSON or url encoded string.
    try {
      jobData = JSON.parse(req.data.toString());
    } catch (e) {
      jobData = url.parse('?' + req.data.toString(), true).query;
    }
    jobData.project_id = req.headers.cn;
    var project_cert = projects.certificates[jobData.project_id];

    if (project_cert) {
      var subject = x509.getSubject(project_cert);

      try {
        var attributes = JSON.parse(subject[consts.PROJECT_ATTRIBUTES_OID]);

        if (attributes.max_job_count >= jobData.job_count) {
          jobData.category = attributes.category;
          addJob(jobData, addJobCallback);
        } else {
          log.warn('PM server tried to add an exceded job count. Rejecting...');
          res.writeHead(consts.STATUS_CODE_FORBIDDEN);
          res.end();
        }
      } catch (e) {
        log.warn('Project certificate for this PM server is bad formated. Rejecting...');
        res.writeHead(consts.STATUS_CODE_FORBIDDEN);
        res.end();
      }

    } else {
      log.warn('An unknown PM server tried to post a job description. Rejecting...');
      res.writeHead(consts.STATUS_CODE_FORBIDDEN);
      res.end();
    }
  });
});

if (runningFromCmd) {
  log.info('Starting Job Server PM...');
  startPMConnectionPoint(consts.PORT_PM_CONNECTION_POINT);
  log.info('Job Server PM Started');
}

process.on('SIGTERM', function() {
  log.info('Received SIGTERM signal');
  pm_server.close(function() {
    analytics.stopTracking();
    log.info('Closed out remaining connections.');
    process.exit(0);
  });

  setTimeout(function() {
    analytics.stopTracking();
    log.error('Could not close connections in time, forcefully shutting down.');
    process.exit(1);
  }, consts.TIMEOUT_REQUEST);
});


/**
  Exports
*/
module.exports = {
  startPMConnectionPoint: startPMConnectionPoint,
  addJob: addJob
};
