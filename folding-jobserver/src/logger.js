/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

/**
 * Provides basic logger functionality.
 *
 * @module logger
 */

'use strict';

var INFO = 4;
var WARNING = 3;
var ERROR = 2;
var OFF = 1;

var validLogLevels = [OFF, ERROR, INFO, WARNING];

var defaults = {
  logLevel: WARNING,
  errorLabel: 'ERROR: ',
  infoLabel: 'INFO: ',
  warningLabel: 'WARN: ',
  logFunction: console.log
};


function typeSafe(object, typename) {
  if (typeof object != typename) {
    throw new Error('Illegal argument: ' + object);

  }

  return object;
}


function valueSafe(value, valueArray) {
  for (var i in valueArray) {
    if (value == valueArray[i]) {
      return value;

    }
  }

  throw new Error('Illegal argument: ' + value);
}



/**
 * @constructor
 * @this Logger
 * @param {Object} - options Logger options. May include:
 *  - logLevel: INFO, ERROR, WARNING or OFF (defined in logger.js), the default
 *  is WARNING;
 *  - logFunction: must receive a single string, defaults to
 *  console.log;
 *  - infoLabel: a String, defaults to 'INFO :';
 *  - errorLabel: a String, defaults to 'ERROR: ';
 *  - warningLabel: a String, defaults to 'WARN: ';
 */
function Logger(options) {
  options = options || {};

  this.logLevel = valueSafe(
      (options.logLevel || defaults.logLevel),
      validLogLevels
      );

  this.logFunction = typeSafe(
      (options.logFunction || defaults.logFunction),
      'function'
      );

  this.errorLabel = typeSafe(
      (options.errorLabel || defaults.errorLabel),
      'string'
      );

  this.warningLabel = typeSafe(
      (options.warningLabel || defaults.warningLabel),
      'string'
      );

  this.infoLabel = typeSafe(
      (options.infoLabel || defaults.infoLabel),
      'string'
      );
}


/**
 * Logs an error message if logLevel is ERROR or greater.
 *
 * @param {String} message The message to log.
 */
Logger.prototype.error = function(message) {
  if (this.logLevel >= ERROR) {
    this.logFunction(this.errorLabel + message);

  }
};


/**
 * Logs an info message if logLevel is INFO or greater.
 *
 * @param {String} message The message to log.
 */
Logger.prototype.info = function(message) {
  if (this.logLevel >= INFO) {
    this.logFunction(this.infoLabel + message);

  }
};


/**
 * Logs a warning message if logLevel is WARNING or greater.
 *
 * @param {String} message The message to log.
 */
Logger.prototype.warn = function(message) {
  if (this.logLevel >= WARNING) {
    this.logFunction(this.warningLabel + message);

  }
};


/**
 * Exports.
 */
module.exports = {
  Logger: Logger,
  INFO: INFO,
  WARNING: WARNING,
  ERROR: ERROR,
  OFF: OFF
};

