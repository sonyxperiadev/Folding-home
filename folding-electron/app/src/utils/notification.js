/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const notifier = require('node-notifier');
const DEFAULT_TITLE = 'Folding@Home';

function buildAndShowNotification(config) {
    config.title = config.title ? config.title : DEFAULT_TITLE;
    config.icon = config.icon ? config.icon : dirs.images + '/notification-icon.png';
    config.sound = true;
    config.wait = true;

    logger.debug('buildAndShowNotification: ', config);
    notifier.notify(config, function (err, response) {
    });
}

function downloadIcon(url, cb) {
    if (!fs.existsSync(DATA_DIR)) {
        fs.mkdirSync(DATA_DIR);
    }

    let dest = DATA_DIR + '/temp_notification.png';
    let file = fs.createWriteStream(dest);
    https.get(url, function (response) {
        response.pipe(file);
        file.on('finish', function () {
            file.close(function () {
                cb(dest);
            });
        });
    });
}

function showNotification(config) {
    if (config.iconUrl) {
        global.fileDownloader.getFile(config.iconUrl, function (imagePath) {
            config.icon = imagePath;
            buildAndShowNotification(config);
        });
    } else {
        buildAndShowNotification(config);
    }
}

module.exports = {
    showNotification: showNotification
};
