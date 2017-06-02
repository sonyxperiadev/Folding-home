/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

/**
 * Provides an interface to interact with dynamodb.
 *
 * @module dynamodb_interface
 */

'use strict';

var aws = require('aws-sdk');
var schedule = require('node-schedule');
var consts = require('./consts_server.js');

var dynamoDb = new aws.DynamoDB();
var s3 = new aws.S3();

var USERS_TABLE_NAME = 'users';

var log;
var numberOfUsers = 0;
var cacheUUIDs = {};
var instanceName;

function updateNumberOfUsers() {
  aws.config.getCredentials(function(err) {

    if (err) {
      log.error(err.stack); // credentials not loaded
    } else {
      var params = {
        TableName: USERS_TABLE_NAME
      };

      dynamoDb.describeTable(params, function(error, data) {
        if (error) {
          log.warn('Error reading user count: ');
          log.warn('    ' + error);
        }
        try {
          numberOfUsers = data.Table.ItemCount;
        } catch (e) {
          log.warn('Error reading Table ItemCount: ');
          log.warn(e);
        }
      });
    }
  });
  setTimeout(updateNumberOfUsers, 60 * 60 * 1000); //update hourly
}

updateNumberOfUsers();

var jobClearCacheUUIDs = schedule.scheduleJob({hour: 12, minute: 0, dayOfWeek: 1},
    function() {
      saveCache(function() {
        //clear cache weekly (every monday at midday)
        log.info('Clearing UUIDs Cache..');
        cacheUUIDs = {};
      });
    });

function saveCache(callback) {
  if (instanceName) {
    aws.config.getCredentials(function(err) {
      if (err) {
        log.error(err.stack); // credentials not loaded
        callback();
      } else {
        var data = '';

        var params = {
          Bucket: instanceName + consts.BUCKETS.JobserverFiles,
          Key: consts.BUCKETS.PreviousCacheFile,
          Body: JSON.stringify(cacheUUIDs)
        };

        log.info('Saving ' + params.Key + ' in bucket ' + params.Bucket);

        s3.putObject(params, function(err, data) {
          if (err) {
            log.error(err);
          } else {
            log.info('Saved cache successfully.');
          }
          callback();
        });
      }
    });
  } else {
    log.error('Instance name not loaded.');
    callback();
  }
}


/**
 * Load previous saved cache from S3.
 *
 * @param {string} ec2InstanceName The name of this ec2 instance.
 */
exports.loadPreviousCache = function(ec2InstanceName) {
  aws.config.getCredentials(function(err) {
    if (err) {
      log.error(err.stack); // credentials not loaded
    } else {
      instanceName = ec2InstanceName;
      var data = '';

      var params = {
        Bucket: instanceName + consts.BUCKETS.JobserverFiles,
        Key: consts.BUCKETS.PreviousCacheFile
      };

      log.info('Loading ' + params.Key + ' from bucket ' + params.Bucket);

      s3.getObject(params)
        .on('httpData', function(chunk) { data += chunk; })
        .on('httpDone', function() {
            try {
              cacheUUIDs = JSON.parse(data);
              log.info('Previous cache loaded.');
            } catch (exception) {
              log.error(exception);
            }
          })
        .on('error', function(error) {
            log.error(error);
          }).send();
    }
  });
};


/**
 * Creates the tables necessary to the application in dynamodb. Ignores errors.
 */
exports.createTables = function() {
  aws.config.getCredentials(function(err) {
    if (err) {
      log.error(err.stack); // credentials not loaded
    } else {
      dynamoDb.createTable({
        'AttributeDefinitions': [
          {
            'AttributeName': 'uuid',
            'AttributeType': 'S'
          }
        ],
        'TableName': USERS_TABLE_NAME,
        'KeySchema': [
          {
            'AttributeName': 'uuid',
            'KeyType': 'HASH'
          }
        ],
        'ProvisionedThroughput': {
          'ReadCapacityUnits': 5,
          'WriteCapacityUnits': 80
        }
      }, function(error, data) {
        if (error) {
          log.warn('WARNING: could not create users table' +
              '(maybe it already exists)');
          log.warn('    ' + error);
        }
      });
    }
  });
};


/**
 *  Increments the number of users that made job requests since ever. A user is
 *  not counted twice.
 *
 *  @param {string} uuid The uuid of the user to count.
 */
exports.countUser = function(uuid) {
  if (cacheUUIDs[uuid] === undefined) {
    aws.config.getCredentials(function(err) {
      if (err) {
        log.error(err.stack); // credentials not loaded
      } else {
        dynamoDb.putItem({
          'TableName': USERS_TABLE_NAME,
          'Item': { 'uuid': { 'S': uuid}},
          'Expected': { 'uuid': { 'Exists': false } }
        }, function(error, data) {
          if (error) {
            log.warn('Error adding user ' + uuid + ' to table ' + USERS_TABLE_NAME +
                '. Probably because this user already exists.');
            log.warn(error);
          }
          cacheUUIDs[uuid] = 1;
        });
      }
    });
  }
};


/**
 * Sends a request to dynamodb querying the number of users that received job requests since ever.
 *
 * @return {int} The number of users
 */
exports.getTotalNumberOfUsers = function() {
  return numberOfUsers;
};


/**
 * Sets up a logger.
 *
 * @param {Logger} logger Logger instance to use.
 */
exports.setupLog = function(logger) {
  log = logger;
};

