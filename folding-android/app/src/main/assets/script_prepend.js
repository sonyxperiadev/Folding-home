function enable() {

  var https = require('https');
  var fs = require('fs');

  process.stdin.resume();
  var allowedReqs = ['https', 'readContext', 'zlib', 'url', 'openmm', 'crypto'];
  var prohibitedHosts = ['localhost'];

  var REQUEST_TIMEOUT = 30000;

  var checkProhibitedPattern = function(addr) {
    return addr !== undefined && (
        // IPv4
        // "This" Network
        addr.match(/^0\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        // Private-Use Networks
        addr.match(/^10\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        // Loopback Interface
        addr.match(/^127\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        // Link Local
        addr.match(/^169\.254\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        // Private-Use Networks
        addr.match(/^172\.(1[6-9]|2\d|30|31)\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        // IETF Protocol Assignments
        addr.match(/^192\.0\.0\.([0-9]{1,3})/) !== null ||
        // TEST-NET-1
        addr.match(/^192\.0\.2\.([0-9]{1,3})/) !== null ||
        // 6to4 Relay Anycast
        addr.match(/^192\.88\.99\.([0-9]{1,3})/) !== null ||
        // Priva-Use Networks
        addr.match(/^192\.168\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        // Network Interconnect - Device Benchmark Testing
        addr.match(/^198\.18\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        addr.match(/^198\.19\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        // TEST-NET-2
        addr.match(/^198\.51\.100\.([0-9]{1,3})/) !== null ||
        // TEST-NET-3
        addr.match(/^203\.0\.113\.([0-9]{1,3})/) !== null ||
        // Multicast
        addr.match(/^2(2[4-9]|3[1-9])\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        // Reserved for future use
        addr.match(/^2(4[0-9]|5[0-5])\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})/) !== null ||
        // Limited Broadcast
        addr.match(/^255\.255\.255\.255/) !== null ||

        // IPv6
        // Loopback
        /^::1/.test(addr) ||
        // Unique-local
        addr.match(/^fc00:/) !== null ||
        // Link-Scoped Unicast
        addr.match(/^fe80:/) !== null ||
        // Documentation Prefix
        addr.match(/^2001:0DB8:/) !== null ||
        // 6to4
        addr.match(/^2002:/) !== null ||
        // Teredo
        addr.match(/^2001:0000:/) !== null ||
        // 6bone
        addr.match(/^5F/) !== null ||
        addr.match(/^3FFE:/) !== null ||
        // ORCHID
        addr.match(/^2001:001/) !== null ||
        // Multicast
        addr.match(/^FF/) !== null
    );
  };

  var sendHostViolationError = function(host) {
    if (typeof(process.send) == 'function') {
      process.send({
        event: 'violation',
        data: {
          violation: 'host',
          message: 'Tried to connect with a non-allowed host: ' + host
        }
      });
    }
    process.exit(1);
  };

  var sendRequireViolationError = function(module) {
    if (typeof(process.send) == 'function') {
      process.send({
        event: 'violation',
        data: {
          violation: 'require_module',
          message: 'Tried to load non-allowed module: ' + module
        }
      });
    }
    process.exit(1);
  };

  process.on('SIGALRM', function() {
    if (script !== undefined && typeof(script.emit) == 'function') {
      script.emit('PAUSE');
    }
  });

  process.on('SIGCONT', function() {
    if (script !== undefined && typeof(script.emit) == 'function') {
      script.emit('CONTINUE');
    }
  });

  process.on('SIGTERM', function() {
    if (script !== undefined && typeof(script.emit) == 'function') {
      script.emit('STOP');
    }
  });

  var isProhibited = function(host) {
    return (host !== undefined &&
        (prohibitedHosts.indexOf(host) != -1) || checkProhibitedPattern(host));
  };

  var httpsRequestMock = function(options, callback) {
    if (isProhibited(options.host) || isProhibited(options.hostname)) {
      if (options.host !== undefined) {
        sendHostViolationError(options.host);
      } else {
        sendHostViolationError(options.hostname);
      }
    } else {
      var req = https.request(options, callback);
      req.setTimeout(REQUEST_TIMEOUT, function() {
        req.abort();
      });
      return req;
    }
  };

  var httpsGetMock = function(options, callback) {
    if (isProhibited(options.host) || isProhibited(options.hostname)) {
      if (options.host !== undefined) {
        sendHostViolationError(options.host);
      } else {
        sendHostViolationError(options.hostname);
      }
    } else {
      var req = https.get(options, callback);
      req.setTimeout(REQUEST_TIMEOUT, function() {
        req.abort();
      });
      return req;
    }
  };

  var httpsMock = {
    Server: null,
    createServer: null,
    request: httpsRequestMock,
    get: httpsGetMock,
    Agent: https.Agent,
    globalAgent: https.globalAgent
  };

  function getContext() {
    var context = null;

    try {
      context = JSON.parse(fs.readFileSync('job_script_context.json').toString());
    } catch (e) {
      console.log(e);
      context = null;
    }

    return context;
  }

  var fsMock = {
    getContext: getContext
  };

  var mocks = {'https': httpsMock, 'readContext': fsMock};

  var hookedLoader = function(request, parent, isMain) {
    if (allowedReqs.indexOf(request) == -1) {
      sendRequireViolationError(request);
    } else {
      if (mocks[request]) {
        return mocks[request];
      } else {
        return originalLoader(request, parent, isMain);
      }
    }
  };

  var m = require('module');
  var originalLoader = m._load;
  m._load = hookedLoader;

  setTimeout(function() {
    if (script !== undefined && typeof(script.emit) == 'function') {
      script.emit('START');
    }
  }, 200);
}

function saveContext(context) {
  if (typeof(process.send) == 'function') {
    process.send({
      event: 'save_context',
      data: context
    });
  }
}

function loadContext() {
  return require('readContext').getContext();
}

function setResearchDetails(details) {
  if (details === undefined) {
    details = {};
  }
  if (typeof(process.send) == 'function') {
    process.send({
      event: 'research_details',
      data: details
    });
  }
}

var script = new (require('events').EventEmitter)();

enable();
