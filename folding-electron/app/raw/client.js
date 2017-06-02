/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
'use strict';

// A simple GComp client implementation. Downloads jobs from the job
// server and executes them.  This can be executed stand-alone as well
// and should be maintained as such to ensure continued multi-platform
// compatibility.
/*jshint devel: true, browser: true, node: true  */
/*global process: false, require: false, module: false, __dirname: false, Buffer: false */
var consts = require('./consts_client.js');
var Fsm = require('./fsm.js');
var https = require('https');
var fs = require('fs');
var child_process = require('child_process');
var readline = require('readline');
var zlib = require('zlib');
var x509 = require('x509');
var dns = require('dns');

var debug = true;
var runningFromCmd = require.main === module;
var consolelog = (runningFromCmd && debug) ? console.log : function() {};
var consoledir = (runningFromCmd && debug) ? console.dir : function() {};
var processexit = (runningFromCmd && debug) ? process.exit : function() {};
var host_communication = null;
var resPath = __dirname + '/';
var execPath = process.argv[0];

/**
 * Creates a new Client.
 *
 * @constructor
 */
function Client() {
  this.testmode = process.argv.length > 2;

  this.cproc = null;
  this.expectedKill = false;
  this.shouldKill = false;
  this.killTimeout = null;
  this.cancelJobTimerID = null;
  this.client_consts = consts;

  this.job_category = this.client_consts.DEFAULT_JOB_CATEGORY;
  this.UUID = null;
  this.app_version = null;
  this.client_data = {platform:'', os_version:'', arch:''};
  this.ipAddress = null;

  this.jobclient = {
  };

  consolelog('testmode is: ' + (this.testmode ? 'active' : 'inactive'));

  this.createFsm();
}


/**
   * Creates the finite state machine.
   */
Client.prototype.createFsm = function() {
  // Client FSM

  var self = this;

  // Creating States
  var starting_client_state = {};

  /**
   * Executed when FSM enters in starting_client_state.
   */
  starting_client_state.in = function() {
    consolelog('FSM: Starting Client');
    self.startCommunication();
    self.client_fsm.next('TxGetKey');
  };

  var waiting_key_state = {};


  /**
   * Executed when FSM enters in waiting_key_state.
   */
  waiting_key_state.in = function() {
    consolelog('FSM: Waiting key');
    // Request host key
    self.host_send({action: 'get_key', content: {}});
  };

  var reading_key_state = {};


  /**
   * Executed when FSM enters in reading_key_state.
   *
   * @param {Object} key_data Key data for the reading_key_state.
   */
  reading_key_state.in = function(key_data) {
    consolelog('FSM: Reading key:');
    self.actionKey(key_data);
    self.client_fsm.next('TxKeyAccepted');
  };

  var getting_job_state = {};


  /**
   * Executed when FSM enters in getting_job_state.
   *
   * @param {int} timeout Timeout in ms to wait before requesting a new job from jobserver.
   */
  getting_job_state.in = function(timeout) {
    consolelog('FSM: Getting job');
    self.execute(timeout, null, function(err, script, nextJobTimeout) {
      if (err) {
        consolelog(err);
        self.client_fsm.next('RxGetJobError', nextJobTimeout);
      } else {
        var data = { script: script, nextJobTimeout: nextJobTimeout };
        self.client_fsm.next('TxJobReceived', data);
      }
    });
  };

  var executing_job_state = {};

  /**
   * Executed when FSM enters in executing_job_state.
   *
   * @param {Object} data JSON containing script to be executed during executing_job_state
   * and next job timeout.
   */
  executing_job_state.in = function(data) {
    consolelog('FSM: Executing job');
    self.host_send({action: 'executing_job', content: {}});
    self.runJob(data.script, function(code, errorMsg) {
      if (code) {
        self.host_send({action: 'job_execution_error',
          content: {exit_code: code, error: errorMsg}});
        self.client_fsm.next('TxJobFinished', data.nextJobTimeout);
      } else {
        self.host_send({action: 'job_finished', content: {exit_code: code}});
        self.client_fsm.next('TxJobFinished');
      }
    });
  };

  var killing_client_state = {};


  /**
   * Executed when FSM enters in killing_client_state.
   */
  killing_client_state.in = function() {
    consolelog('FSM: Killing client now');
    if (self.cproc !== null) {
      self.cproc.kill('SIGKILL');
    }
    self.client_exit();
  };

  var resuming_job_state = {};


  /**
   * Executed when FSM enters in resuming_job_state.
   */
  resuming_job_state.in = function() {
    var script = self.getPausedScript();
    if (script !== undefined && script !== null) {
      self.client_fsm.next('WasPaused', script);
    } else {
      // We just started the client.js.
      // To avoid a lot of requests to server lets *really* start after a random delay.
      // 0s ~ 300s of delay.
      var startDelay = 0; // Miliseconds.

      consolelog('Waiting ' + startDelay + 'ms before start');
      setTimeout(function() {
        self.client_fsm.next('WasNotPaused');
      }, startDelay);
    }
  };

  // Create FSM
  this.client_fsm = new Fsm(starting_client_state);

  // Filling Transitions

  this.client_fsm.set_transition(starting_client_state, 'TxGetKey', waiting_key_state);
  this.client_fsm.set_transition(waiting_key_state, 'RxKey', reading_key_state);
  this.client_fsm.set_transition(reading_key_state, 'TxKeyAccepted', resuming_job_state);
  this.client_fsm.set_transition(resuming_job_state, 'WasNotPaused', getting_job_state);
  this.client_fsm.set_transition(resuming_job_state, 'WasPaused', executing_job_state);
  this.client_fsm.set_transition(getting_job_state, 'TxJobReceived', executing_job_state);
  this.client_fsm.set_transition(getting_job_state, 'RxGetJobError', getting_job_state);
  this.client_fsm.set_transition(executing_job_state, 'TxJobFinished', getting_job_state);
  this.client_fsm.set_transition(starting_client_state, 'RxKillClient', killing_client_state);
  this.client_fsm.set_transition(waiting_key_state, 'RxKillClient', killing_client_state);
  this.client_fsm.set_transition(reading_key_state, 'RxKillClient', killing_client_state);
  this.client_fsm.set_transition(resuming_job_state, 'RxKillClient', killing_client_state);
  this.client_fsm.set_transition(getting_job_state, 'RxKillClient', killing_client_state);
  this.client_fsm.set_transition(executing_job_state, 'RxKillClient', killing_client_state);

};


/**
 * Gets the Ip Address of a requisition.
 *
 * @param {Object} req The request.
 * @return {String} The ip address
 */
Client.prototype.getIpAddress = function(req) {
  if (req !== null && req.connection !== null) {
    return req.headers['x-forwarded-for'] ||
        req.connection.remoteAddress;
  }
  return undefined;
};


/**
 * Runs the given script.
 *
 * @param {String} script The script to be executed.
 * @param {Function} callback The callback to be called when the script is done.
 * @return {Object} The process
 */
Client.prototype.runJob = function(script, callback) {

  var startTime = Date.now();
  var errorMsg = '';
  //gcomp_script is a version of nodejs incapable
  //of accessing the filesystem freely or creating child processes.
  script = fs.readFileSync(resPath + this.client_consts.SCRIPT_PREPEND_PATH).toString()
      + "var platform = '" + this.client_data.platform + "';"
      + "var os_version = '" + this.client_data.os_version + "';"
      + "var arch = '" + this.client_data.arch + "';"
      + script;
  //consolelog(script);
  this.cproc = child_process.spawn(execPath, ['--harden'],
      { stdio: ['pipe', 'pipe', 'pipe', 'ipc'],
        cwd: resPath});
  this.cproc.stdin.end(script);
  this.cproc.stdin.destroySoon();

  var self = this;

  this.cproc.on('message', function(evt) {
    try {
      consolelog('Script sent event: "' + evt.event + '", with data: ' + JSON.stringify(evt.data));
      if (evt.event === 'violation') {
        self.handleJobViolation(evt.data);
      } else if (evt.event === 'save_context') {
        self.handleJobSaveContext(evt.data);
      } else if (evt.event === 'research_details') {
        self.handleResearchDetails(evt.data);
      } else {
        consolelog('No handler for script event: "' + evt.event + '", with data: ' +
            JSON.stringify(evt.data));
      }
    } catch (e) {
      consolelog('Error while receiving message.');
    }
  });

  this.cproc.stdout.on('data', function(data) {
    consolelog(data.toString());
  });

  this.cproc.stderr.on('data', function(data) {
    consolelog('stderr: ' + data.toString());
    errorMsg += data.toString();
  });

  this.cproc.stdin.on('error', function() {});

  this.cproc.on('close', function(code, signal) {
    self.cproc = null;
    if (signal === 'SIGKILL') {
      // The job has expired while running or killed after a pause
      if (self.expectedKill) {
        self.expectedKill = false;
        self.clearPausedState();
      } else {
        var now = Date.now();
        var elapsedTime = now - startTime;
        self.decrementJobRemaningRunTime(elapsedTime);

        if (!this.shouldKill) {
          // Should not die, so we restart the process
          consolelog('restarting child process...');
          self.createFsm();
          self.client_fsm.start();
        }
      }
      code = 1;
    } else {
      self.clearPausedState();
    }

    if (self.cancelJobTimerID !== null) {
      clearTimeout(self.cancelJobTimerID);
      self.cancelJobTimerID = null;
    }

    consolelog('child process exited with code ' + code);
    callback(code, errorMsg);
  });

  return this.cproc;
};


/**
 * Cancels the job.
 */
Client.prototype.cancelJob = function() {
  if (this.cproc !== null) {
    this.expectedKill = true;
    this.cproc.kill('SIGKILL');
  }
};


/**
 * Decrements the job remaining runtime.
 *
 * @param {int} elapsedTime The elapsed time.
 */
Client.prototype.decrementJobRemaningRunTime = function(elapsedTime) {
  try {
    if (fs.existsSync(resPath + this.client_consts.JOB_ATTRIBUTES_PATH)) {
      var attributes = JSON.parse(fs.readFileSync(resPath +
          this.client_consts.JOB_ATTRIBUTES_PATH));
      attributes.remaining_run_time = attributes.remaining_run_time - elapsedTime;

      fs.writeFileSync(resPath +
          this.client_consts.JOB_ATTRIBUTES_PATH, JSON.stringify(attributes));
    }
  } catch (e) {
    consoledir(e);
    this.host_send({action: 'limit_storage', content: {}});
    this.cancelJob();
  }
};


/**
 * Sets the client constants.
 *
 * @param {Object} newConsts The new client constants.
 */
Client.prototype.setConsts = function(newConsts) {
  this.client_consts = newConsts;
};


/**
 * Sets the host send function
 *
 * @param {Function} io The io function (ignored).
 * @param {Function} write The host send function.
 */
Client.prototype.setHostSend = function(io, write) {
  this.host_send = write;
};


/**
 * Gets the job description.
 *
 * @param {Object} reqOptions The request options.
 * @param {Function} callback The callback to be used.
 */
Client.prototype.getJobDescription = function(reqOptions, callback) {
  if (!reqOptions) {
    reqOptions = {
      headers: { 'Accept-Encoding': 'gzip',
        'Cache-Control' : 'no-cache' },

      //Server details
      host: this.client_consts.JOB_SERVER_ADDRESS,
      port: this.client_consts.PORT_JOB_SERVER,
      path: this.client_consts.PATH_JOB_SERVER,
      method: 'POST',
      agent: false,
      rejectUnauthorized: false
    };
  } else if (!reqOptions.headers) {
    reqOptions.headers = {};
  }

  var data = {
    category: this.job_category,
    UUID: this.UUID,
    app_version: this.app_version,
    app_platform: this.client_data.platform,
    os_version: this.client_data.os_version,
    arch: this.client_data.arch
  };
  data = JSON.stringify(data);
  var self = this;

  consolelog('Requesting job...');
  zlib.gzip(data, function(err, compressed) {
    if (err) {
      callback(err, null);
    } else {
      reqOptions.headers['Content-Length'] = compressed.length;
      reqOptions.headers['Content-Encoding'] = 'gzip';

      var req = https.request(reqOptions, function(res) {
        if (res.statusCode != self.client_consts.STATUS_CODE_OK) {
          consolelog('statusCode: ' + res.statusCode);
          var err = new Error('Response status not 200');
          err.http_code = res.statusCode;
          callback(err, null);
        } else {
          consolelog('OK! getting job... ');

          var encoding = res.headers['content-encoding'];

          if (encoding === 'gzip')
            res.setEncoding('base64');
          else
            res.setEncoding('utf8');

          var data = '';
          res.on('data', function(chunk) {
            data += chunk;
          });

          self.ipAddress = self.getIpAddress(res);

          res.on('end', function() {
            callback(null, encoding, data);
          });

          res.on('error', function(err) {
            consoledir(err);
            err = new Error('Error while receiving the job description.');
            callback(err, null);
          });
        }
      });

      req.setTimeout(consts.TIMEOUT_REQUEST, function() {
        req.abort();
        consolelog('Request timeout');
      });

      req.on('error', function(err) {
        consoledir(err);
        callback(err, null);
      });

      req.write(compressed);
      req.end();
    }
  });
};


/**
 * Checks the format of a job description by Own.
 *
 * @param {Object} jobDesc The job description.
 * @return {Boolean} True if the format is Ok, False otherwise.
 */
Client.prototype.isByOwn = function(jobDesc) {
  return jobDesc.path !== undefined &&
      jobDesc.client_key !== undefined &&
      jobDesc.client_cert !== undefined &&
      jobDesc.project_public_certificate !== undefined;
};


/**
 * Checks the format of a job description by S3.
 *
 * @param {Object} jobDesc The job description.
 * @return {Boolean} True if the format is Ok, False otherwise.
 */
Client.prototype.isByS3 = function(jobDesc) {
  return jobDesc.s3_config !== undefined;
};


/**
 * Checks the format of the job description.
 *
 * @param {Object} jobDesc The job description.
 * @return {Boolean} True if the format is Ok, False otherwise.
 */
Client.prototype.checkJobDescriptionFormat = function(jobDesc) {
  var self = this;
  var own = self.isByOwn(jobDesc);
  var s3 = self.isByS3(jobDesc);

  var ok = ((own && !s3) || (!own && s3)) &&
      jobDesc.statistics !== undefined &&
      jobDesc.statistics.number_of_users !== undefined;

  return ok;
};


/**
 * Checks the project subject format.
 *
 * @param {Object} subject The subject.
 * @return {Boolean} True if the format is Ok, False otherwise.
 */
Client.prototype.checkProjectSubjectFormat = function(subject) {
  var ok = subject[this.client_consts.PROJECT_ATTRIBUTES_OID] !== undefined;
  if (ok) {
    try {
      var attributes = JSON.parse(subject[this.client_consts.PROJECT_ATTRIBUTES_OID]);
      ok = attributes.server_address !== undefined &&
          attributes.server_port !== undefined &&
          attributes.category !== undefined &&
          attributes.max_job_count !== undefined &&
          attributes.run_time_limit !== undefined &&
          attributes.execution_time_limit !== undefined &&
          attributes.storage_limit !== undefined;
    } catch (e) {
      return false;
    }
  }
  return ok;
};


/**
 * Checks the S3 configuration format.
 *
 * @param {Object} config The Configuration.
 * @return {Boolean} True if the format is Ok, False otherwise.
 */
Client.prototype.checkS3ConfigFormat = function(config) {
  var ok = config.credentials !== undefined &&
      config.params !== undefined &&
      config.credentials.accessKeyId !== undefined &&
      config.credentials.secretAccessKey !== undefined &&
      config.credentials.region !== undefined &&
      config.params.Bucket !== undefined &&
      config.params.Key !== undefined &&
      config.params.request_date !== undefined;
  return ok;
};


/**
 * Adds the project attributes.
 *
 * @param {Object} data The data.
 * @param {Function} callback The callback to be used.
 */
Client.prototype.addProjectAttributes = function(data, callback) {
  var self = this;
  var err;
  try {
    var jobJson = JSON.parse(data.toString());

    if (this.checkJobDescriptionFormat(jobJson)) {
      var attributes;
      if (self.isByOwn(jobJson)) {
        var subject = x509.getSubject(jobJson.project_public_certificate);

        if (this.checkProjectSubjectFormat(subject)) {
          attributes = JSON.parse(subject[this.client_consts.PROJECT_ATTRIBUTES_OID]);
          callback(null, jobJson, attributes, this.client_consts.REQUEST_JOB_BY_OWN);
        } else {
          err = new Error('Public Project Certificate in bad format.');
          callback(err, null, null, null);
        }
      } else if (self.isByS3(jobJson)) {

        if (this.checkS3ConfigFormat(jobJson.s3_config)) {
          attributes = jobJson.s3_config;
          callback(null, jobJson, attributes, this.client_consts.REQUEST_JOB_BY_S3);
        } else {
          err = new Error('S3 Configuration in bad format.');
          callback(err, null, null, null);
        }
      } else {
        err = new Error('Job description in bad format.');
        callback(err, null, null, null);
      }
    } else {
      err = new Error('Job description in bad format.');
      callback(err, null, null, null);
    }
  } catch (e) {
    err = new Error('Error while parsing the job description.');
    callback(err, null, null, null);
  }
};


/**
 * Parses a job description.
 *
 * @param {String} encoding The job encoding.
 * @param {Buffer} buffer The buffer containing the job.
 * @param {Function} callback The callback to be used.
 */
Client.prototype.parseJobDescription = function(encoding, buffer, callback) {
  var self = this;
  if (encoding === 'gzip') {
    buffer = new Buffer(buffer, 'base64');
    zlib.gunzip(buffer, function(err, decoded) {
      if (!err) {
        self.addProjectAttributes(decoded, callback);
      } else {
        callback(err, null, null);
      }
    });
  } else {
    self.addProjectAttributes(buffer, callback);
  }
};


/**
 * Gets a job.
 *
 * @param {Object} jobDesc The job description.
 * @param {Object} attributes The server attributes.
 * @param {Object} headers The headers to be sent on request.
 * @param {Function} callback The callback to be called.
 */
Client.prototype.getJob = function(jobDesc, attributes, headers, callback) {

  var self = this;
  if (headers !== null)
    headers.Authorization = 'openmm_script_key'; // TODO: Get from GTagManager

  var req = https.request({
    headers: headers,
    //Provided connection details.
    host: attributes.server_address,
    port: attributes.server_port,
    path: jobDesc.path,
    method: 'GET',
    agent: false,
    rejectUnauthorized: false
  }, function(res) {
    if (res.statusCode != self.client_consts.STATUS_CODE_OK) {
      consolelog('statusCode: ' + res.statusCode);
      var err = new Error('Response status not 200');
      err.http_code = res.statusCode;
      callback(err, null);
    } else {
      var encoding = res.headers['content-encoding'];

      if (encoding === 'gzip')
        res.setEncoding('base64');
      else
        res.setEncoding('utf8');

      var script = '';
      res.on('data', function(chunk) {
        script += chunk;
      });

      res.on('end', function() {
        if (encoding === 'gzip') {
          var buffer = new Buffer(script, 'base64');
          zlib.gunzip(buffer, function(err, decoded) {
            if (!err)
              callback(null, decoded.toString());
            else
              callback(err, null);
          });
        } else {
          callback(null, script.toString());
        }
      });

      res.on('error', function(err) {
        consoledir(err);
        err = new Error('Error while receiving the job from PM');
        callback(err, null);
      });
    }
  });

  req.setTimeout(self.client_consts.TIMEOUT_REQUEST, function() {
    req.abort();
    consolelog('Request timeout');
  });

  req.on('error', function(err) {
    consoledir(err);
    err = new Error('Error while connecting to PM Server');
    callback(err, null);
  });

  req.end();
};


/**
 * Handles a Job Violation.
 *
 * @param {Object} violation The job violation.
 */
Client.prototype.handleJobViolation = function(violation) {
  if (typeof violation === 'object') {
    violation.project_public_certificate = this.getPausedProjectCertificate();
    this.postReportViolation(violation, function(err, response) {
      if (err) {
        consolelog(err.message);
      } else {
        consolelog('Report violation success: Status was ' + response.statusCode);
      }
    });
  } else {
    consolelog('Error while handling job violation.');
  }
};


/**
 * Handles a Job Save Context.
 *
 * @param {Object} context The job context.
 */
Client.prototype.handleJobSaveContext = function(context) {
  try {
    var contextString = JSON.stringify(context);
    var jobAttributes;
    jobAttributes = JSON.parse(fs.readFileSync(resPath +
        this.client_consts.JOB_ATTRIBUTES_PATH).toString());

    if ((contextString.length / (1024 * 1024)) <= jobAttributes.storage_limit) {
      fs.writeFileSync(resPath + this.client_consts.JOB_SCRIPT_CONTEXT_PATH, contextString);
    } else {
      var violationData = {
        violation: 'context_length',
        message: 'Max context length exceeded: ' + contextString.length
      };
      this.handleJobViolation(violationData);
      this.host_send({action: 'limit_storage', content: {}});
      this.cancelJob();
    }
  } catch (e) {
    consoledir(e);
    this.cancelJob();
  }
};


/**
 * Handles the research_type operation.
 *
 * @param {string} data The type of research
 */
Client.prototype.handleResearchDetails = function(data) {
  consolelog('Received research details: ' + data);
  this.host_send({action: 'research_details',
    content: {title: data.title,
      description: data.description,
      url: data.url,
      target_id: data.target_id,
      stream_id: data.stream_id}});
};


/**
 * Posts a error report about access violations.
 *
 * @param {Object} violation Violation data.
 * @param {functin} callback Callback called after post execution response.
 */
Client.prototype.postReportViolation = function(violation, callback) {
  var data = JSON.stringify(violation);
  var self = this;

  zlib.gzip(data, function(err, compressed) {
    if (err) {
      callback(new Error('Report violation error: ' + err.message), null);
    } else {
      var options = {
        hostname: self.client_consts.JOB_SERVER_ADDRESS,
        port: self.client_consts.PORT_JOB_SERVER,
        path: self.client_consts.PATH_REPORT,
        headers: {
          'Content-Length': compressed.length,
          'Content-Encoding': 'gzip',
          'Accept-Encoding': 'gzip'
        },
        method: 'POST',
        agent: false,
        rejectUnauthorized: false
      };
      var req = https.request(options, function(res) {
        if (res.statusCode === self.client_consts.STATUS_CODE_OK) {
          callback(null, res);
        } else {
          callback(new Error('Report violation error: Status not 200'), null);
        }

      });
      req.on('error', function(e) {
        callback(new Error('Report violation error: ' + e.message), null);
      });
      req.write(compressed);
      req.end();
    }
  });
};


/**
 * Gets a job script from the own PM Server.
 *
 * @param {Object} jobDesc The job description.
 * @param {Object} attributes The server attributes.
 * @param {int} nextJobTimeout Timeout to be used.
 * @param {Function} callback The callback to be called.
 */
Client.prototype.getJobByOwn = function(jobDesc, attributes, nextJobTimeout, callback) {
  var self = this;
  consolelog('Getting job by Own');
  var headers = { 'Accept-Encoding': 'gzip' };
  self.getJob(jobDesc, attributes, headers, function(err, script) {
    if (!err) {
      //Write a job startup notification to the parent process.
      self.host_send({action: 'job_received', content: {'ip_address': self.ipAddress}});
      self.clearPausedState();

      try {
        attributes.remaining_run_time = attributes.execution_time_limit *
            self.client_consts.HOUR_IN_MILLISECONDS;
        var now = new Date();
        now.setDate(now.getDate() + attributes.run_time_limit);
        attributes.expiry_date = now.getTime();

        attributes.storage_limit = attributes.storage_limit -
            (script.length + JSON.stringify(attributes).length +
            jobDesc.project_public_certificate.length) / (1024 * 1024);

        fs.writeFileSync(resPath +
            self.client_consts.CURRENT_SCRIPT_PATH, script);
        fs.writeFileSync(resPath +
            self.client_consts.JOB_ATTRIBUTES_PATH, JSON.stringify(attributes));
        fs.writeFileSync(resPath +
            self.client_consts.JOB_PROJECT_CERTIFICATE_PATH,
            jobDesc.project_public_certificate);

        callback(null, script, null);
      } catch (e) {
        self.host_send({action: 'limit_storage', content: {}});
        callback(e, null, nextJobTimeout);
      }
    } else {
      callback(err, null, nextJobTimeout);
    }
  });
};


/**
 * Gets a job script from S3.
 *
 * @param {Object} jobDesc The job description.
 * @param {Object} attributes The server attributes.
 * @param {int} nextJobTimeout Timeout to be used.
 * @param {Function} callback The callback to be called.
 */
Client.prototype.getJobByS3 = function(jobDesc, attributes, nextJobTimeout, callback) {
  var self = this;
  consolelog('Getting job by S3');
  var bucket = attributes.params.Bucket;
  var key = attributes.params.Key;
  var date_str = attributes.params.request_date;
  var access_key = attributes.credentials.accessKeyId;
  var secret_key = attributes.credentials.secretAccessKey;
  var region = attributes.credentials.region;

  var host = 's3-' + region + '.amazonaws.com';
  var path = '/' + bucket + '/' + key;
  console.log('job: ' + host + path);

  dns.lookup(host, function(err, s3IpAddress, family) {
    //Write a job startup notification to the parent process.
    if (err) {
      self.host_send({action: 'dns_error', content: {'error': err}});
    } else {
      self.host_send({action: 'job_received', content: {'ip_address': s3IpAddress}});
    }
  });

  var string_to_sign = 'GET\n\n\n\nx-amz-date:' + date_str + '\n/' + bucket + '/' + key;

  var crypto = require('crypto');
  var shasum = crypto.createHmac('sha1', secret_key);
  shasum.update(string_to_sign);
  var sig = shasum.digest('base64');
  var headers = { 'Authorization' : 'AWS ' + access_key + ':' + sig, 'X-Amz-Date' : date_str };
  var options = {
    hostname: host,
    path: path,
    headers: headers,
    method: 'GET'
  };

  var req = https.request(options, function(res) {
    res.setEncoding('utf8');
    var data = '';
    res.on('data', function(chunk) {
      data += chunk;
    });

    res.on('end', function() {
      if (res.statusCode != self.client_consts.STATUS_CODE_OK) {
        consolelog('statusCode: ' + res.statusCode);
        var err = new Error('Response status not 200');
        err.http_code = res.statusCode;
        callback(err, null, nextJobTimeout);
      } else {
        callback(null, data, nextJobTimeout);
      }
    });
  });

  req.on('error', function(e) {
    callback(e, null, nextJobTimeout);
  });

  req.end();
};


/**
 * Execute the script.
 *
 * @param {int} timeout Timeout to be used.
 * @param {Object} options The script options.
 * @param {Function} callback The callback to be used when finished.
 */
Client.prototype.execute = function(timeout, options, callback) {
  //On any error, this defines the next timeout level.
  if (!timeout) {
    timeout = this.client_consts.TIMEOUT_NOW_TRY;
  }
  //After a few retries and no received job, let the device sleep for the night instead
  var nextJobTimeout = Math.min(this.testmode ? this.client_consts.TIMEOUT_TEST_MODE :
      (timeout * 2 + this.client_consts.TIMEOUT_MIN_TRY), this.client_consts.TIMEOUT_MAX_TRY);

  consolelog('Waiting ' + timeout + 'ms before requesting new job');

  var self = this;
  setTimeout(function() {
    self.getJobDescription(options, function(err, encoding, buffer) {
      if (!err) {
        self.parseJobDescription(encoding, buffer, function(err, jobDesc, attributes, requestType) {
          if (!err) {
            self.host_send({action: 'number_of_users',
              content: {'number_of_users': jobDesc.statistics.number_of_users}});

            if (requestType == self.client_consts.REQUEST_JOB_BY_OWN) {
              self.getJobByOwn(jobDesc, attributes, nextJobTimeout, callback);
            } else if (requestType == self.client_consts.REQUEST_JOB_BY_S3) {
              self.getJobByS3(jobDesc, attributes, nextJobTimeout, callback);
            } else {
              err = new Error('Unknown Request Type' + requestType);
              callback(err, null, nextJobTimeout);
            }
          } else {
            callback(err, null, nextJobTimeout);
          }
        });
      } else {
        callback(err, null, nextJobTimeout);
      }
    });
  }, timeout);

};


// Client Host Communication


/**
 * Sends a message.
 *
 * @param {String} message The message to send.
 */
Client.prototype.host_send = function(message) {
  if (runningFromCmd) {
    process.stdout.write(JSON.stringify(message) + '\n');
  }
};


/**
 * Handles the message from the host.
 *
 * @param {String} message The message sent.
 */
Client.prototype.host_handle_message = function(message) {
  var msg = null;
  try {
    msg = JSON.parse(message);
    if (msg) {
      consolelog(msg);
      if (msg.action === 'key') {
        this.client_fsm.next('RxKey', msg.content);
      } else if (msg.action === 'kill') {
        if (!this.shouldKill) {
          this.shouldKill = true;
          if (msg.content === 'SIGKILL') {
            this.client_fsm.next('RxKillClient');
          } else {
            consolelog('FSM: Killing client gracefully');
            this.actionKill();
          }
        }
      } else if (msg.action === 'continue') {
        this.shouldKill = false;
        if (this.killTimeout !== null) {
          clearTimeout(this.killTimeout);
          this.killTimeout = null;
          if (this.cproc !== null) {
            this.cproc.kill('SIGCONT');
          }
        }
      }
    }
  } catch (e) {
    consoledir(e);
    this.client_exit(5);
  }
};


/**
 * Exits the script.
 *
 * @param {int} code The code to be used when exiting script.
 */
Client.prototype.client_exit = function(code) {
  if (this.shouldKill) {
    this.host_send({action: 'client_killed', content: {}});
    if (code) {
      processexit(code);
    } else {
      processexit();
    }
  }
};


/**
 * Returns the current job process.
 *
 * @return {Object} cproc The current job process.
 */
Client.prototype.getJobProc = function() {
  return this.cproc;
};


/**
 * Sets the flag should kill of the client.
 *
 * @param {Boolean} newShouldKill The new flag for should kill.
 */
Client.prototype.setShouldKill = function(newShouldKill) {
  this.shouldKill = newShouldKill;
};


/**
 * Set the UUID to be used on this client.
 *
 * @param {String} uuid New UUID.
 */
Client.prototype.setUUID = function(uuid) {
  this.UUID = uuid;
};


/**
 * Action to check the content received by the key action and load
 * the private key and certificate.
 *
 * @param {Object} key_data Key data used to decrypt the client private key.
 */
Client.prototype.actionKey = function(key_data) {
  if (key_data) {
    this.UUID = key_data.uuid;
    this.app_version = key_data.app_version;

    this.client_data.platform = key_data.platform;
    this.client_data.os_version = key_data.os_version;
    this.client_data.arch = key_data.arch;

     consolelog('App version is ' + key_data.app_version);
     consolelog('App platform is ' + this.client_data.platform);
     consolelog('OS version is ' + this.client_data.os_version);
     consolelog('App arch is ' + this.client_data.arch);
  }
  // Notify key accepted.
  this.host_send({action: 'key_accepted', content: {}});
};


/**
 * Action to kill the current running job.
 */
Client.prototype.actionKill = function() {
  var self = this;
  if (self.cproc !== null) {
    self.cproc.kill('SIGALRM'); //Send a catchable SIGALRM to allow the job to end gracefully.
  }
  self.killTimeout = setTimeout(function() {
    if (self.cproc !== null) {
      self.cproc.kill('SIGTERM');
    }
    // Kill the process after timeout_pause_job.
    // When we reach this point, continues will not work.
    setTimeout(function() {
      self.client_fsm.next('RxKillClient');
    }, self.client_consts.TIMEOUT_TERM_JOB);
  }, self.client_consts.TIMEOUT_PAUSE_JOB);
};


/**
 * Starts the communication with the client.
 */
Client.prototype.startCommunication = function() {

  if (host_communication !== null) {
    host_communication.close();
  }

  // Start host communication
  host_communication = readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });
  var self = this;
  host_communication.on('line', function(line) {
    self.host_handle_message(line);
  });

  if (runningFromCmd) {
    host_communication.on('close', function() {
      self.cancelJob();
      process.exit(1);
    });
  }

  // Handle uncaught exceptions.
  process.removeAllListeners('uncaughtException'); // Remove old leaked Listeners
  process.on('uncaughtException', function(err) {
    consoledir(err);
    self.cancelJob();
    self.client_exit(5);
  });
};


/**
 * Safely remove a file.
 *
 * @param {String} path The path of the file to remove.
 */
Client.prototype.safeFileRemove = function(path) {
  try {
    fs.unlinkSync(path);
  } catch (ignore) {}
};


/**
 * Clears the paused state.
 */
Client.prototype.clearPausedState = function() {
  this.safeFileRemove(resPath + this.client_consts.CURRENT_SCRIPT_PATH);
  this.safeFileRemove(resPath + this.client_consts.JOB_ATTRIBUTES_PATH);
  this.safeFileRemove(resPath + this.client_consts.JOB_SCRIPT_CONTEXT_PATH);
  this.safeFileRemove(resPath + this.client_consts.JOB_PROJECT_CERTIFICATE_PATH);
};


/**
 * Gets the paused script.
 * @return {Object} The paused script.
 */
Client.prototype.getPausedScript = function() {
  var script = null;
  try {
    script = fs.readFileSync(resPath + this.client_consts.CURRENT_SCRIPT_PATH).toString();
    var jobAttributes = JSON.parse(fs.readFileSync(resPath +
        this.client_consts.JOB_ATTRIBUTES_PATH).toString());

    var now = Date.now();
    var remaining_day_time = jobAttributes.expiry_date - now;

    if (!jobAttributes.expiry_date || !jobAttributes.remaining_run_time ||
        remaining_day_time <= 0 || jobAttributes.remaining_run_time <= 0) {
      script = null;
    } else {
      var remaining_run_time = remaining_day_time < jobAttributes.remaining_run_time ?
                               remaining_day_time : jobAttributes.remaining_run_time;

      // Setup a timer so if the job expires during the calculation
      // it will be aborted.
      var self = this;
      this.cancelJobTimerID = setTimeout(function() {
        self.cancelJob();
      }, remaining_run_time);
    }
  } catch (e) {
    script = null;
  }

  return script;
};


/**
 * Gets the paused project certificate.
 * @return {Object} The project certificate.
 */
Client.prototype.getPausedProjectCertificate = function() {
  var projectCertificate = null;
  try {
    projectCertificate = fs.readFileSync(resPath +
        this.client_consts.JOB_PROJECT_CERTIFICATE_PATH).toString();
  } catch (e) {
    projectCertificate = null;
  }

  return projectCertificate;
};

if (runningFromCmd && debug) {
  var client = new Client();
  client.client_fsm.start();
}


/**
 * Module's exports.
 */
module.exports = Client;
