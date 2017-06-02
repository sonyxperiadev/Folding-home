/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

var http = require('http');
var aws = require('aws-sdk');


aws.config.loadFromPath(__dirname + '/aws-region.json');
var ec2 = new aws.EC2();


/**
 * Searchs for the value of the tag called Name on a list of tags.
 *
 * @param {array} tagList a list of tags.
 * @return {string} the value of tag name, or undefined if it doesn't exist.
 */
function searchNameTag(tagList) {
  for (var i in tagList) {
    if (tagList[i].Key == 'Name') {
      return tagList[i].Value;
    }
  }
  return undefined;
}


/**
 * Send a request to EC2 metadata server for the security-groups the instance is
 * registered to and calls the given callback with the response, if no error
 * occurs.
 *
 * @param {function} callback The function(error, data) to call when the answer
 * is available.
 */
function requestInstanceName(callback) {
  var options = {
    hostname: '169.254.169.254',
    path: '/latest/meta-data/instance-id',
    port: 80,
    timeout: 200
  };

  var req = http.get(options, function(res) {
    res.setEncoding('utf8');

    res.on('data', function(data) {
      var params = {
        InstanceIds: [data]
      };
      ec2.describeInstances(params, function(err, describeInstanceData) {
        if (err) callback(err, null); // an error occurred
        else {
          var instanceName = searchNameTag(
              describeInstanceData.Reservations[0].Instances[0].Tags);
          callback(null, instanceName);
        }
      });
    });

  }).on('error', function(e) {
    callback(e, null);
  });

  req.setTimeout(1000, function() {
    req.abort();
  });
}


/**
 * Exports.
 */
module.exports = {
  getInstanceName: requestInstanceName
};
