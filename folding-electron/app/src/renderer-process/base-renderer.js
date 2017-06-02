/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const fs = electron.remote.require('fs');
const path = electron.remote.require('path');


global.requireLocalPackage = function (packageName) {
    return require(path.join(global.dirs.source, packageName));
};

const TRANSLATE_REGEX = /\{\{([^}}]+)\}\}/g;
const BIND_PREFIX = 'bind-';

global.dirs = electron.remote.getGlobal('dirs');
global.errorCodes = electron.remote.getGlobal('errorCodes');
global.buildConfig = electron.remote.getGlobal('buildConfig');
global.eventMessages = electron.remote.getGlobal('eventMessages');

global.logger = requireLocalPackage('/utils/logger.js');
global.i18n = requireLocalPackage('/utils/i18n.js');

global.addCss = function (path) {
    let css = document.createElement('link');
    css.rel = 'stylesheet';
    css.href = path;
    document.head.appendChild(css);
};

global.parseNodes = function () {
    translateNode(document);
};

global.inflateHtml = function (params, callback) {
    fs.readFile(global.dirs.views + '/' + params.file, 'utf8', (err, data) => {
        let returnVal = null;
        if (!err) {
            let parentElem = params.parent ? params.parent : document.createElement('div');
            parentElem.innerHTML = data;
            translateNode(parentElem);
            returnVal = params.parent ? params.parent : parentElem.innerHTML;
        }

        if (callback) {
            callback(returnVal);
        }
    });
};

function translateNode(node) {
    //replace text nodes
    if (node) {
        if (node.nodeType == Node.TEXT_NODE) {
            node.textContent = replaceText(node.textContent);
        }
        //replace attributes
        if (node.attributes) {
            for (let i = 0; i < node.attributes.length; i++) {
                var attrib = node.attributes[i];
                if (attrib.specified) {
                    setAttribute(node, attrib);
                }
            }
        }
        //if has children, iterate over them
        if (node.childNodes) {
            for (let i = 0; i < node.childNodes.length; i++) {
                let elem = node.childNodes[i];
                translateNode(elem);
            }
        }
    }
}

function setAttribute(node, attrib) {
    let value = replaceText(attrib.value);
    let index = attrib.name.indexOf(BIND_PREFIX);
    attrib.value = value;
    // if the attribute name starts with BIND_PREFIX (bind-) we add a new attribute without the prefix
    // and the content will be replaced with it's binded value
    // eg.: bind-src='{{var_name}}' becomes src='var value'
    if (index === 0) {
        let realName = attrib.name.substring(BIND_PREFIX.length);
        node.setAttribute(realName, value);
    }
}

function replaceValue(param, operation, string) {
    //ff the operation is translate, just translate the message
    if (operation == 'translate') {
        return global.i18n.__(param);
    }
    //the content can be an expression/statement
    try {
        return eval(param);
    } catch (error) {
        return string;
    }
}

function replacer(match, p1, offset, string) {
    //checks if the text contains a pipe char followed by a operation eg.: {{key_name|translate}}
    let pipeIndex = p1.indexOf('|');
    let operation = pipeIndex > 0 && p1.length > 0 ? p1.substring(pipeIndex + 1, p1.length).trim() : '';
    let param = pipeIndex > 0 ? p1.substring(0, pipeIndex).trim() : p1.trim();
    return replaceValue(param, operation, string);
}

function replaceText(text) {
    return text.replace(TRANSLATE_REGEX, replacer);
}

window.onload = function () {
    parseNodes();
};