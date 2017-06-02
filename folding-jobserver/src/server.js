/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

'use strict';

//We need this to not turn our tests into a mess of logs
var runningFromCmd = require.main === module;
var debug = true;
var jobs_queue;

var aws;
var ec2_utils;
var instanceName;

if (runningFromCmd) {
  ec2_utils = require('./ec2_utils');
  jobs_queue = require('./jobs_queue_aws.js');
  aws = require('aws-sdk');
  aws.config.loadFromPath(__dirname + '/aws-region.json');
} else {
  jobs_queue = require('./jobs_queue_redis.js');
}

var consts = require('./consts_server.js');
var http = require('http');
var projects = require('./projects.js');
var defaultJobDesc = require('<PLACE_YOUR_DEFAULT_JOB_DESCRIPTION_HERE>');
var router = require('router');
var logger = require('./logger.js');

var logLevel;
if (runningFromCmd && debug) {
  logLevel = logger.INFO;
} else if (runningFromCmd) {
  logLevel = logger.WARN;
} else {
  logLevel = logger.OFF;
}

var useDefaultJobDesc = true;
// JobScript version mapping. Currently we only have the default.
// This variable updates at startup using version_mappings.json from S3.
var version_mapping = {'android': {'0' : 'FAHjobscript.js'}};

var log;
function setupLog(logFunction) {
  log = new logger.Logger({logLevel: logLevel, logFunction: logFunction});
  jobs_queue.setup(log);
}

setupLog(function(message) {
  console.log('[' + Date() + '] ' + message);
});

function loadVersionMappingFromS3() {
  var s3 = new aws.S3();

  var data = '';
  var mapping;

  var params = {
    Bucket: instanceName + consts.BUCKETS.JobserverFiles,
    Key: consts.BUCKETS.VersionMappingFile
  };

  log.info('Loading ' + params.Key + ' from bucket ' + params.Bucket);

  s3.getObject(params)
    .on('httpData', function(chunk) { data += chunk; })
    .on('httpDone', function() {
        try {
          version_mapping = JSON.parse(data);
          log.info('Version Mapping Loaded: ' + data);
        } catch (exception) {
          log.error(exception);
        }
      })
    .on('error', function(error) {
        log.error(error);
      }).send();
}

if (aws) {
  ec2_utils.getInstanceName(function(error, data) {
    if (error) {
      log.warn('Got error: ' + error.message + ' (ok if not running in EC2)');
      log.warn('Fallback log function (console.log) will be used');
    } else {
      instanceName = data;
      log.info('Instance Name is ' + instanceName);

      log = new logger.Logger({
        logLevel: logLevel,
        logFunction: function(message) {
          console.log('[' + Date() + '] ' + message);
        }
      });

      loadVersionMappingFromS3();
      jobs_queue.loadPreviousCache(instanceName);
    }
  });
}

function sendJobRequestToClient(res, data) {
  res.writeHead(consts.STATUS_CODE_OK,
      'OK', {'Content-Type': 'text/plain; charset=\"utf8\"'});
  res.write(data);
  res.end();
}

// Adds number of users contributing to the job description.
function getModifiedDescription(jobDesc, numberOfUsers) {
  var project_cert = projects.certificates[jobDesc.project_id];
  if (project_cert) {
    jobDesc.project_public_certificate = project_cert;
    jobDesc.statistics = {};
    jobDesc.statistics[consts.NUMBER_OF_USERS_KEY] = numberOfUsers;
    return jobDesc;
  } else {
    return null;
  }
}

function getJobRequest(category, secure_flag, callback) {
  var queueKey = category + ':' + (secure_flag ? 'secure' : 'insecure');

  jobs_queue.poll(queueKey, function(err, data) {
    if (err) {
      //No jobs available demanding the secure flag in this category, retry without the flag.
      if (secure_flag) {
        getJobRequest(category, false, callback);
      } else {
        log.error('Error while getting job: ' + err.http_code);
        callback(err, null);
      }
    } else {
      jobs_queue.getTotalNumberOfUsers(function(err, numberOfUsers) {
        numberOfUsers = err ? 0 : numberOfUsers;
        try {
          data = getModifiedDescription(JSON.parse(data), numberOfUsers);
          if (data) {
            var keysToRemove = ['project_id', 'remaining'];

            keysToRemove.forEach(function(key) {
              delete data[key];
            });

            log.info('Got job successfully.');
            callback(null, JSON.stringify(data));
          } else {
            err = new Error();
            err.http_code = consts.STATUS_CODE_NO_CONTENT;
            log.info('Received a request of an unknown project ID. Rejecting...');
            callback(err, null);
          }
        } catch (e) {
          err = new Error();
          err.http_code = consts.STATUS_CODE_NO_CONTENT;
          log.info('Error while parsing the request. Rejecting...');
          callback(err, null);
        }
      });
    }
  });
}

function getQueueStatus(conn) {
  jobs_queue.length(function(q_err, data) {
    if (!q_err) {
      if (!data)
        data = {};
      conn.res.write(JSON.stringify(data));
      conn.res.end();
      log.info('Sent queue status successfully');
    } else {
      conn.res.writeHead(consts.STATUS_CODE_INTERNAL_SERVER_ERROR);
      conn.res.end();
      log.error('Error while sending queue status');
    }
  });
}

function getJobForVersion(app_version, app_platform) {
  var current_mapping = getPlatformVersionMapping(app_platform);

  var keys = Object.keys(current_mapping).sort();
  // Convert to int in case it comes as a string
  app_version = parseInt(app_version);
  // WARNING: We consider all keys are integers.
  // and the lowest value is 0.
  for (var i = 0; i < keys.length; i++) {
    var keyValue = parseInt(keys[i]);
    if (app_version < keyValue) {
      // If find a key higher than client's version. Get the previous.
      return current_mapping[keys[i - 1]];
    } else if (app_version == keyValue || i == keys.length - 1) {
      // If we find a key with the same value as client's version or
      // It is the last key, then get this key.
      return current_mapping[keys[i]];
    }
  }
  // If everything goes well, the algorithm should never reach this point.
  // But just in case, we return the fallback which is the first key.
  return current_mapping[keys[0]];
}

function getPlatformVersionMapping(app_platform) {
  // this function return android mapping if app_platform is null
  var default_platform = app_platform ? app_platform.toLowerCase() : "android";
  var value = version_mapping[default_platform];
  return value ? value : {'0' : 'FAHjobscript.js'} ;
}

function sendDefaultJobDesc(res, app_version, app_platform) {
  var jobDesc = defaultJobDesc;

  jobs_queue.getTotalNumberOfUsers(function(err, numberOfUsers) {
    numberOfUsers = err ? 0 : numberOfUsers;
    jobDesc.statistics = {};
    jobDesc.statistics[consts.NUMBER_OF_USERS_KEY] = numberOfUsers;

    // Modifies the key from s3_config to the mapped from versions_mapping.json
    jobDesc.s3_config.params.Key = getJobForVersion(app_version, app_platform);
    jobDesc.s3_config.params.Bucket = instanceName + consts.BUCKETS.Jobscripts;
    jobDesc.s3_config.params.request_date = (new Date()).toGMTString();
    sendJobRequestToClient(res, JSON.stringify(jobDesc));
  });
}

//Client job fetch server
var client_server = http.createServer(function(req, res) {
  /*if (req.headers['x-client-verified'] !== undefined &&
      req.headers['x-client-verified'] !== null &&
      req.headers['x-client-verified'] === 'SUCCESS') {
    log.info('Received an authorized request from client.');
    req.connection.authorized = true;
  } else {
    log.warn('Received an unauthorized request from client.');
    req.connection.authorized = false;
  }*/
  client_interface_router(req, res);
});

var startJobServer = function(port) {
  client_server.listen(port);
  client_server.timeout = consts.TIMEOUT_REQUEST;
  return client_server;
};

//Jobserver clients interface
var client_interface_router = router();

var getJobEndpoint = function(req, res) {
  var data = '';
  req.on('data', function(chunk) {
    data += chunk;
  });

  req.on('end', function() {
    var category = null;
    var UUID = null;
    var app_version = null;
    var app_platform = null;
    try {
      data = JSON.parse(data);
      category = data.category;
      UUID = data.UUID;
      app_version = data.app_version;
      app_platform = data.app_platform;
    } catch (e) {
      log.error('Bad request format.');
    }

    if (category !== undefined && category !== null &&
        UUID !== undefined && UUID !== null) {
      log.info('Request format is ok. Getting job.');
      if (useDefaultJobDesc) {
        sendDefaultJobDesc(res, app_version, app_platform);
        log.info('Sent job successfully.');
        jobs_queue.incrementNumberOfUsers(UUID);
      } else {
        getJobRequest(category, req.connection.authorized, function(err, data) {
          if (data) {
            sendJobRequestToClient(res, data);
            log.info('Sent job successfully.');
            jobs_queue.incrementNumberOfUsers(UUID);
          } else {
            res.writeHead(err.http_code);
            res.end();
          }
        });
      }

    } else {
      log.error('Bad request format (missing UUID or category).');
      res.writeHead(consts.STATUS_CODE_NO_CONTENT);
      res.end();
    }
  });
};


client_interface_router.post('/getjob', function(req, res) {
  log.info('The request (POST) is for job.');
  getJobEndpoint(req, res);
});

client_interface_router.get('/status', function(req, res) {
  log.info('The request (GET) is for status.');
  // If the request reached this point, then everything is OK
  res.writeHead(consts.STATUS_CODE_OK);
  res.end();
});

client_interface_router.get('/queue_status', function(req, res) {
  log.info('The request (GET) is for queue status.');
  // Checks the status of the Queue
  var conn = {
    res: res,
    req: req
  };
  getQueueStatus(conn);
});

client_interface_router.post('/report', function(req, res) {
  log.info('The request (POST) is for report.');
  var data = '';

  req.on('data', function(chunk) {
    data += chunk;
  });

  req.on('end', function() {
    var report = null;
    try {
      report = JSON.parse(data);
      //TODO: do something with report.
    } catch (e) {
      log.error('Error while processing report.');
    }
    log.info('Report processed successfully.');
    res.writeHead(consts.STATUS_CODE_OK);
    res.end();
  });
});

if (runningFromCmd) {
  log.info('Starting Job Server...');
  startJobServer(consts.PORT_JOB_SERVER);
  log.info('Job Server Started.');
}

process.on('SIGTERM', function() {
  log.warn('Received SIGTERM signal');
  client_server.close(function() {
    log.warn('Closed out remaining connections.');
    process.exit(0);
  });

  setTimeout(function() {
    log.error('Could not close connections in time, forcefully shutting down.');
    process.exit(1);
  }, consts.TIMEOUT_REQUEST);
});


/**
 * Exports
 */
module.exports = {
  startJobServer: startJobServer,
  getJob: getJobRequest
};
