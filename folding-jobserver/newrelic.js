'use strict'

/**
 * This file includes all of the configuration variables used by the Node.js
 * module. If there's a configurable element of the module and it's not
 * described in here, there's been a terrible mistake.
 */
exports.config = {
  /**
   * Array of application names.
   *
   * @env NEW_RELIC_APP_NAME
   */
  app_name: ['Folding@Home'],
  /**
   * The user's license key. Must be set by per-app configuration file.
   *
   * @env NEW_RELIC_LICENSE_KEY
   */
  license_key: '<PLACE_YOUR_NEWRELIC_LICENSE_KEY_HERE>',
  logging: {
    /**
     * Verbosity of the module's logging. This module uses bunyan
     * (https://github.com/trentm/node-bunyan) for its logging, and as such the
     * valid logging levels are 'fatal', 'error', 'warn', 'info', 'debug' and
     * 'trace'. Logging at levels 'info' and higher is very terse. For support
     * requests, attaching logs captured at 'trace' level are extremely helpful
     * in chasing down bugs.
     *
     * @env NEW_RELIC_LOG_LEVEL
     */
    level: 'trace',
  }
}
