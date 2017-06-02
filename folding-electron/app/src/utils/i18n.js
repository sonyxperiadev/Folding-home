/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const i18n = require('i18n');

i18n.configure({
    //defaultLocale: 'pt',
    directory: dirs.l10n,
    updateFiles: false
});

module.exports = i18n;