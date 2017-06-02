/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

/**
 * Jobs queue stored into a Redis database.
 *
 * @module jobs_queue
 */

'use strict';

var consts_server = require('./consts_server.js');
var fs = require('fs');
var dynamodb = require('./dynamodb_interface.js');

dynamodb.createTables();

var log;


/**
 * Setups a log for the jobs queue.
 *
 * @param {Logger} logger Logger instance used for logging.
 */
exports.setup = function(logger) {
  log = logger;
  dynamodb.setupLog(logger);
};


/**
 * Load previous saved cache.
 *
 * @param {string} instanceName The name of this ec2 instance.
 */
exports.loadPreviousCache = function(instanceName) {
  dynamodb.loadPreviousCache(instanceName);
};


/**
 * Callback called after add.
 *
 * @callback addCallback
 * @param {Error} err null if no error.
 */


/**
 * Inserts the specified job into the jobs queue.
 *
 * @param {Object} queueKey The queue to store de job
 * @param {Object} job job to be stored.
 * @param {boolean} numberOfCopies How many of these jobs needs to be duplicated
 * @param {addCallback} callback Callback function for add request
 */
exports.add = function(queueKey, job, numberOfCopies, callback) {
  //TODO: Implement using SQS
  var err = new Error();
  err.http_code = consts_server.STATUS_CODE_INTERNAL_SERVER_ERROR;
  callback(err);
};


/**
 * Callback called after polling.
 *
 * @callback pollCallback
 * @param {Error} err null if no error.
 * @param {Object} response Job polled from job queue.
 */


/**
 * Retrieves and removes the head job of the jobs queue.
 * If the queue is empty the callback error is set and the value is null.
 *
 * @param {Object} queueKey The queue to retrieve the job
 * @param {pollCallback} callback Callback function for polling request.
 */
exports.poll = function(queueKey, callback) {
  //TODO: Implement using SQS
  var err = new Error();
  err.http_code = consts_server.STATUS_CODE_INTERNAL_SERVER_ERROR;
  callback(err, null);
};

/**
 * Callback called after queue length request.
 *
 * @callback lengthCallback
 * @param {Error} err null if no error.
 * @param {Object} Object containing the number of jobs of each queue or an empty object
                  ({}) if all the queues are empty
 */


/**
 * Gets the length of the jobs queue.
 *
 * @param {lengthCallback} callback Callback function for length request.
 */
exports.length = function(callback) {
  //TODO: Implement using SQS
  var err = new Error();
  err.http_code = consts_server.STATUS_CODE_INTERNAL_SERVER_ERROR;
  callback(err, null);
};


/**
 * Increment the number of users of the current day
 *
 * @param {String} uuid the user UUID
 */
exports.incrementNumberOfUsers = function(uuid) {
  dynamodb.countUser(uuid);
};

/**
 * Callback called after queue length request.
 *
 * @callback numberOfUsersCallback
 * @param {Error} err null if no error.
 * @param {Object} Object containing the number of users
 */


/**
 * Get the number of users of the last day
 * @param {numberOfUsersCallback} callback
 */
exports.getTotalNumberOfUsers = function(callback) {
  callback(null, dynamodb.getTotalNumberOfUsers());
};


/**
 * Callback called after queue length request.
 *
 * @callback flushDBCallback
 * @param {Error} err null if no error.
 */


/**
 * Flush the DB.
 * @param {flushDBCallback} callback
 */
exports.flushDB = function(callback) {
  //TODO: Implement using SQS
  var err = new Error();
  err.http_code = consts_server.STATUS_CODE_INTERNAL_SERVER_ERROR;
  callback(err);
};
