/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

'use strict';

var AWS = require('aws-sdk');
var child_process = require('child_process');

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
var PERIOD = 86400; // One day, expressed as seconds
var AWS_ENDPOINT = 'https://monitoring.eu-west-1.amazonaws.com';

var runningFromCmd = require.main === module;
function consolelog(string) {
  if (runningFromCmd)
    console.log(string);
}

try {
  AWS.config.loadFromPath('../src/aws-credentials.json');
} catch (e) {
  if (process.env.AWS_ACCESS_KEY_ID !== undefined &&
      process.env.AWS_SECRET_ACCESS_KEY !== undefined &&
      process.env.AWS_REGION !== undefined) {
    AWS.config.update({accessKeyId: process.env.AWS_ACCESS_KEY_ID,
      secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
      region: process.env.AWS_REGION});
  } else {
    consolelog('Please configure the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_REGION');
    process.exit(1);
  }
}

var ep = new AWS.Endpoint(AWS_ENDPOINT);
var cloudwatch = new AWS.CloudWatch({endpoint: ep});

function instanceid(callback) {
  child_process.exec('ec2metadata --instance-id', function(err, stdout) {
    var id = 'nonEC2instance';
    if (!err)
      id = stdout.toString().trim();

    callback(id);
  });
}

function constructMetricDataRequest(namespace, metricName, dimensionName,
    dimensionValue, startTime, endTime, unit) {

  var metricData = {
    'Namespace': namespace,
    'MetricName': metricName,
    'Dimensions': [{
      'Name': dimensionName,
      'Value': dimensionValue
    }],
    'StartTime': startTime,
    'EndTime': endTime,
    'Period': PERIOD,
    'Statistics': ['Sum'],
    'Unit': unit
  };

  return metricData;
}

function getMetricStatistics(callback) {
  var now = new Date();
  var oneWeekBack = new Date(now);
  oneWeekBack.setDate(oneWeekBack.getDate() - 7);

  instanceid(function(id) {
    var dataToRetrieve = [
      constructMetricDataRequest(NAMESPACE, METRIC_NAME_TOTAL_JOBS_SENT,
          INSTANCEID_DIMENSION_NAME, id,
          oneWeekBack, now, UNIT_JOBS_SENT),

      constructMetricDataRequest(NAMESPACE, METRIC_NAME_TOTAL_JOBS_RECEIVED,
          INSTANCEID_DIMENSION_NAME, id,
          oneWeekBack, now, UNIT_JOBS_RECEIVED),

      constructMetricDataRequest(NAMESPACE, METRIC_NAME_TOTAL_AVG_JOBS_SENT,
          INSTANCEID_DIMENSION_NAME, id,
          oneWeekBack, now, UNIT_AVG_JOBS_SENT),

      constructMetricDataRequest(NAMESPACE, METRIC_NAME_TOTAL_AVG_JOBS_RECEIVED,
          INSTANCEID_DIMENSION_NAME, id,
          oneWeekBack, now, UNIT_AVG_JOBS_RECEIVED)
    ];

    for (var metric in dataToRetrieve) {
      //TODO use the data properly, right now we are just dumping it away.
      getData(dataToRetrieve[metric], callback);
    }
  });
}

function getData(metric) {
  cloudwatch.getMetricStatistics(metric, function(err, data) {
    if (err) {
      consolelog(err);
    }

    consolelog(JSON.stringify(data));
  });
}

getMetricStatistics();
