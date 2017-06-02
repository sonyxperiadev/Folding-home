/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const crypto = require('crypto');
const path = require('path');
const low = require('lowdb');

//TODO generate/store the secret key in a secure, non user readable, way
const secret = 'secretkey';
const algorithm = 'aes-256-ctr';
const inputEncoding = 'utf8';
const outputEncoding = 'hex';

const format = {
    deserialize: (str) => {
        // let decipher = crypto.createDecipher(algorithm, secret);
        // let decrypted = decipher.update(str, outputEncoding, inputEncoding);
        // decrypted += decipher.final(inputEncoding);
        // return JSON.parse(decrypted);
        return JSON.parse(str);
    },
    serialize: (obj) => {
        // let cipher = crypto.createCipher(algorithm, secret);
        // let crypted = cipher.update(JSON.stringify(obj), inputEncoding, outputEncoding);
        // crypted += cipher.final(outputEncoding);
        // return crypted;
        return JSON.stringify(obj);
    }
};

const db = low(path.join(global.dirs.home, 'db.json'), {format: format});

module.exports = db;
