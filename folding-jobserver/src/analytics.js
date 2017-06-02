/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */


'use strict';

var schedule = require('node-schedule');
var AWS = require('aws-sdk');
var child_process = require('child_process');
var redisClient = require('redis').createClient();
var logger = require('./logger.js');
redisClient.select(0);

//Redis Constants
var NUMBER_JOBS_SENT_KEY = 'numberOfJobsSent';
var NUMBER_JOBS_RECEIVED_KEY = 'numberOfJobsReceived';
var METRICS_TO_RESEND_KEY = 'metricsToResend';


//Metrics Constants
var NAMESPACE = 'Computing Jobserver';
var METRIC_NAME_TOTAL_JOBS_SENT = 'Total of jobs sent';
var METRIC_NAME_TOTAL_JOBS_RECEIVED = 'Total of jobs received';
var METRIC_NAME_TOTAL_AVG_JOBS_SENT = 'Average number of jobs sent';
var METRIC_NAME_TOTAL_AVG_JOBS_RECEIVED = 'Average number of jobs received';
var UNIT_JOBS_SENT = 'Count';
var UNIT_JOBS_RECEIVED = 'Count';
var UNIT_AVG_JOBS_SENT = 'Count/Second';
var UNIT_AVG_JOBS_RECEIVED = 'Count/Second';
var INSTANCEID_DIMENSION_NAME = 'InstanceId';
var AWS_ENDPOINT = 'https://monitoring.eu-west-1.amazonaws.com';

var log = new logger.Logger({
  logLevel: logger.INFO,
  logFunction: function(message) {
    if (debug) {
      console.log('[' + Date() + '] ' + message);

    }
  }
});

try {
  AWS.config.loadFromPath(__dirname + '/aws-credentials-analytics.json');
} catch (e) {
  if (process.env.AWS_ACCESS_KEY_ID !== undefined &&
      process.env.AWS_SECRET_ACCESS_KEY !== undefined &&
      process.env.AWS_REGION !== undefined) {
    AWS.config.update({accessKeyId: process.env.AWS_ACCESS_KEY_ID,
      secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
      region: process.env.AWS_REGION});
  } else {
    log.error('Please configure the AWS_ACCESS_KEY_ID,' +
        ' AWS_SECRET_ACCESS_KEY and AWS_REGION');
  }
}

var ep = new AWS.Endpoint(AWS_ENDPOINT);
var cloudwatch = new AWS.CloudWatch({endpoint: ep});

var debug;



function instanceid(callback) {
  child_process.exec('ec2metadata --instance-id', function(err, stdout) {
    var id = 'nonEC2instance';
    if (!err)
      id = stdout.toString().trim();

    callback(id);
  });
}

function addJobSent(numberOfJobs) {
  numberOfJobs = numberOfJobs || 1;
  redisClient.incrby(NUMBER_JOBS_SENT_KEY, numberOfJobs);
}

function addJobReceived(numberOfJobs) {
  numberOfJobs = numberOfJobs || 1;
  redisClient.incrby(NUMBER_JOBS_RECEIVED_KEY, numberOfJobs);
}

function getTotalJobSent(callback) {
  redisClient.getset(NUMBER_JOBS_SENT_KEY, 0, function(err, n) {
    callback((!err && n) ? parseInt(n, 10) : 0);
  });
}

function getTotalJobReceived(callback) {
  redisClient.getset(NUMBER_JOBS_RECEIVED_KEY, 0, function(err, n) {
    callback((!err && n) ? parseInt(n, 10) : 0);
  });
}

function getSummarizedData(callback) {
  var summarized_data = {
    total_jobs_received: 0,
    total_jobs_sent: 0,
    avg_jobs_received: 0,
    avg_jobs_sent: 0
  };

  var totalHours = 24;
  getTotalJobReceived(function(n1) {
    summarized_data.total_jobs_received = n1;
    summarized_data.avg_jobs_received = n1 / totalHours;

    getTotalJobSent(function(n2) {
      summarized_data.total_jobs_sent = n2;
      summarized_data.avg_jobs_sent = n2 / totalHours;
      callback(summarized_data);
    });
  });
}

function constructMetricData(metricName, dimensionName,
    dimensionValue, metricValue, timestamp, unit) {
  var metricData = {
    'MetricName': metricName,
    'Dimensions': [{
      'Name': dimensionName,
      'Value': dimensionValue
    }],
    'Value': metricValue,
    'Timestamp': timestamp,
    'Unit': unit
  };

  return metricData;
}

function sendAnalytics(callback) {
  getSummarizedData(function(summarized_data) {
    var date = new Date();
    instanceid(function(id) {
      var params = {
        'Namespace': NAMESPACE,

        'MetricData': [
          constructMetricData(METRIC_NAME_TOTAL_JOBS_SENT,
              INSTANCEID_DIMENSION_NAME,
              id,
              summarized_data.total_jobs_sent,
              date,
              UNIT_JOBS_SENT),

          constructMetricData(METRIC_NAME_TOTAL_JOBS_RECEIVED,
              INSTANCEID_DIMENSION_NAME,
              id,
              summarized_data.total_jobs_received,
              date,
              UNIT_JOBS_RECEIVED),

          constructMetricData(METRIC_NAME_TOTAL_AVG_JOBS_SENT,
              INSTANCEID_DIMENSION_NAME,
              id,
              summarized_data.avg_jobs_sent,
              date,
              UNIT_AVG_JOBS_SENT),

          constructMetricData(METRIC_NAME_TOTAL_AVG_JOBS_RECEIVED,
              INSTANCEID_DIMENSION_NAME,
              id,
              summarized_data.avg_jobs_received,
              date,
              UNIT_AVG_JOBS_RECEIVED)
        ]
      };

      postAnalytics(params, function(err) {
        if (err) {
          saveFailedData(params);
          log.error('Failed to send data: ' + JSON.stringify(params) +
              ' Error: ' + err);
        }

        callback(err);
      });
    });
  });
}

function postAnalytics(metricData, callback) {
  cloudwatch.putMetricData(metricData, function(err) {
    callback(err);
  });
}

function sendSavedData(callback) {
  getFailedData(function(err, data) {
    if (data) {
      postAnalytics(data, function(err) {
        if (err) {
          callback(err);
        } else {
          popFailedData(function(err) {
            if (err) {
              callback(err);
            } else {
              sendSavedData(callback);
            }
          });
        }
      });
    } else {
      callback(err);
    }
  });
}

function saveFailedData(dataToSave) {
  dataToSave = JSON.stringify(dataToSave);
  redisClient.lpush(METRICS_TO_RESEND_KEY, dataToSave);
}

function getFailedData(callback) {
  redisClient.lindex(METRICS_TO_RESEND_KEY, 0, function(err, data) {
    if (err) {
      callback(err, null);
    } else {
      if (data)
        data = JSON.parse(data);
      callback(null, data);
    }
  });
}

function popFailedData(callback) {
  redisClient.lpop(METRICS_TO_RESEND_KEY, function(err, data) {
    if (data) {
      data = JSON.parse(data);
    }
    callback(err, data);
  });
}

function startTracking(debugMode) {
  debug = debugMode;

  //This trigger the report every 24hrs. Starting from the next midnight.
  schedule.scheduleJob({hour: 0, minute: 0}, doSchedule);
}

function doSchedule(callback) {
  sendAnalytics(function(err) {
    if (err)
      log.error('Error sending analytics data. ' + err);
    sendSavedData(function(err) {
      if (err) {
        log.error('Error sending analytics saved data. ' + err);
      }
    });
  });
}

function stopTracking() {
  for (var job in schedule.scheduledJobs) {
    var success = schedule.cancelJob(job);
    if (!success) {
      log.error('Error canceling scheduled jobs');
    }
  }
  redisClient.quit();
}


/**
  Exports
 */
module.exports = {
  getTotalJobReceived: getTotalJobReceived,
  getTotalJobSent: getTotalJobSent,
  getSummarizedData: getSummarizedData,
  sendAnalytics: sendAnalytics,
  addJobSent: addJobSent,
  addJobReceived: addJobReceived,
  startTracking: startTracking,
  doSchedule: doSchedule,
  stopTracking: stopTracking,
  getFailedData: getFailedData,
  sendSavedData: sendSavedData,
  METRIC_NAME_TOTAL_JOBS_SENT: METRIC_NAME_TOTAL_JOBS_SENT,
  METRIC_NAME_TOTAL_JOBS_RECEIVED: METRIC_NAME_TOTAL_JOBS_RECEIVED,
  METRIC_NAME_TOTAL_AVG_JOBS_SENT: METRIC_NAME_TOTAL_AVG_JOBS_SENT,
  METRIC_NAME_TOTAL_AVG_JOBS_RECEIVED: METRIC_NAME_TOTAL_AVG_JOBS_RECEIVED
};
