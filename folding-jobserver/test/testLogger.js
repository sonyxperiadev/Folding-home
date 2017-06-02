/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var assert = require('assert');
var logger = require('../src/logger');

var validInfoLabel = 'I: ';
var validErrorLabel = 'E: ';
var validWarningLabel = 'W: ';
var validLogFunction = function(str) { };

describe('Logger tests', function() {
  it('Should initialize fields correctly if no options are passed', function() {
    var log = new logger.Logger();
    assert(log.errorLabel === 'ERROR: ');
    assert(log.infoLabel === 'INFO: ');
    assert(log.warningLabel === 'WARN: ');
    assert(log.logFunction === console.log);
    assert(log.logLevel === logger.WARNING);
  });

  it('Should initialize fields correctly if options are omitted', function() {
    var log = new logger.Logger();
    assert(log.errorLabel === 'ERROR: ');
    assert(log.infoLabel === 'INFO: ');
    assert(log.warningLabel === 'WARN: ');
    assert(log.logFunction === console.log);
    assert(log.logLevel === logger.WARNING);
  });

  it('Should initialize logLevel correctly', function() {
    var log1 = new logger.Logger({logLevel: logger.INFO});
    assert(log1.logLevel === logger.INFO);

    var log2 = new logger.Logger({logLevel: logger.WARNING});
    assert(log2.logLevel === logger.WARNING);

    var log3 = new logger.Logger({logLevel: logger.ERROR});
    assert(log3.logLevel === logger.ERROR);

    var log4 = new logger.Logger({logLevel: logger.OFF});
    assert(log4.logLevel === logger.OFF);
  });

  it('Should initialize labels correctly', function() {
    var log = new logger.Logger({
      errorLabel: validErrorLabel,
      infoLabel: validInfoLabel,
      warningLabel: validWarningLabel
    });

    assert(log.errorLabel === validErrorLabel);
    assert(log.infoLabel === validInfoLabel);
    assert(log.warningLabel === validWarningLabel);
  });

  it('Should initialize log function correctly', function() {
    var log = new logger.Logger({logFunction: validLogFunction});
    assert(log.logFunction === validLogFunction);
  });

  //TODO: test unexpected values
  //TODO: test log functions

});

