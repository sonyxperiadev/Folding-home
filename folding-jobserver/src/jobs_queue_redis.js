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
var redis = require('redis');
var fs = require('fs');

//Patch the redis module with the lua support
require('redis-lua').attachLua(redis);

//Constants
var ERR_MSG_EMPTY_QUEUE = 'No jobs queued';
var QUEUES_LENGHT_KEY = 'queues_lenght';

// Module variables
var client = redis.createClient('6379', 'localhost');
client.select(0);

client.on('error', function(err) {
  log.error(err);
  setRedisUnavailable(true);
});

client.on('connect', function() {
  setRedisUnavailable(false);
});

var log;
var redisUnavailable = false;

var addJobRequestLuaScrip = fs.readFileSync(__dirname + '/' + 'addJobRequest.lua');
var pollJobRequestLuaScrip = fs.readFileSync(__dirname + '/' + 'pollJobRequest.lua');

redis.lua('addjobrequest', 1, addJobRequestLuaScrip);
redis.lua('polljobrequest', 1, pollJobRequestLuaScrip);


/**
 * Setups the jobs queue.
 *
 * @param {Logger} logger Logger instance used for logging.
 */
exports.setup = function(logger) {
  log = logger;
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
  job.remaining = numberOfCopies;
  var data = JSON.stringify(job);
  client.addjobrequest(queueKey, data, numberOfCopies, callback);
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
  if (isRedisUnavailable()) {
    var err = new Error();
    err.http_code = consts_server.STATUS_CODE_INTERNAL_SERVER_ERROR;
    callback(err, null);
  } else {
    client.polljobrequest(queueKey, function(err, data) {
      if ((data === null) && !err) {
        var error = new Error(ERR_MSG_EMPTY_QUEUE);
        error.http_code = consts_server.STATUS_CODE_EMPTY_QUEUE;
        callback(error, null);
      } else {
        callback(err, data);
      }
    });
  }
};

/**
 * Callback called after queue length request.
 *
 * @callback lengthCallback
 * @param {Error} err null if no error.
 * @param {Object} Object containing the number of jobs of each queue or a empty object
                  ({}) if all the queues are empty
 */


/**
 * Gets the length of the jobs queue.
 *
 * @param {lengthCallback} callback Callback function for length request.
 */
exports.length = function(callback) {
  client.hgetall(QUEUES_LENGHT_KEY, callback);
};


/**
 * Increment the number of users of the current day
 *
 * @param {String} uuid the user UUID
 */
exports.incrementNumberOfUsers = function(uuid) {
  var day = getToday().toJSON();
  client.hincrby(day, uuid, 1);
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
  client.hlen(getYesterday().toJSON(), callback);
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
  client.flushdb(callback);
};

//Helpers

function isRedisUnavailable() {
  return redisUnavailable;
}

function setRedisUnavailable(newRedisState) {
  redisUnavailable = newRedisState;
}

function getToday() {
  var now = new Date();
  //Remove the hours/minutes/seconds/miliseconds
  now = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0);
  return now;
}

function getYesterday() {
  var now = getToday();
  var yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  return yesterday;
}
