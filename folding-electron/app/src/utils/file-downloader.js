/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const url = require('url');

const app = electron.app;
const DATA_DIR = app.getPath('appData');
const HTTP_ADAPTER = {
    'http:': require('http'),
    'https:': require('https'),
};

function getFileHash(url) {
    return crypto.createHash('md5').update(url).digest("hex");
}

function getAdapter(inputUrl) {
    return HTTP_ADAPTER[url.parse(inputUrl).protocol];
}

function getFile(url, cb) {
    let hash = getFileHash(url);
    let dest = path.join(DATA_DIR, hash);
    if (fs.existsSync(dest)) {
        logger.debug(`get file: ${url} exists under hash: ${hash}`);
        cb(dest);
    } else {
        logger.debug(`get file: ${url} does not exists, downloading and saving under hash: ${hash}`);
        if (!fs.existsSync(DATA_DIR)) {
            fs.mkdirSync(DATA_DIR);
        }
        let file = fs.createWriteStream(dest);
        try {
            getAdapter(url).get(url, function (response) {
                response.pipe(file);
                file.on('finish', function () {
                    file.close(function () {
                        cb(dest);
                    });
                });
            });
        } catch (err) {
            cb('');
        }
    }
}

module.exports = {
    getFile: getFile
};