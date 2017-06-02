var https = require('https');
var req = https.request({host: 'localhost', port: 1337, path: '/', method: 'GET', agent: false, rejectUnauthorized: false });
req.on('error', function() {});
req.end();