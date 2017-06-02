/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

/**
 * Finite State Machine implementation.
 *
 * @module fsm
 */



/**
 * Creates a new FSM with the given initial state.
 *
 * @constructor
 * @param {initialState} initialState Initial state for the FSM.
 */
function Fsm(initialState) {
  this.current = initialState;
}


/**
 * Triggers the initial state of the FSM..
 */
Fsm.prototype.start = function() {
  if (this.current.in) {
    this.current.in();
  }
};


/**
 * Triggers the transition for the given event.
 *
 * @param {string} evt Event that triggers the transition.
 * @param {Object} data Optional event data.
 */
Fsm.prototype.next = function(evt, data) {
  if (this.current.transitions && this.current.transitions[evt]) {
    if (this.current.out) {
      this.current.out();
    }
    this.current = this.current.transitions[evt];
    if (this.current.in) {
      this.current.in(data);
    }
  }
};


/**
 * Set a transition on FSM.
 *
 * @param {Object} state State before the trasition.
 * @param {string} evt Event that triggers the transition.
 * @param {Object} next State after the trasition.
 */
Fsm.prototype.set_transition = function(state, evt, next) {
  if (!state.transitions) {
    state.transitions = {};
  }
  state.transitions[evt] = next;
};


/**
 * Module's exports.
 */
module.exports = Fsm;

// Usage example
/*
var Fsm = require('fsm');

var initialState = {
  in: function(){console.log('function called when entering initialState state')},
  out: function(){console.log('function called when leaving initialState state')},
  transitions: {}
}

var runningJob = {
  in: function(){console.log('function called when entering runningJob state')},
  out: function(){console.log('function called when leaving runningJob state')},
  transitions: {}
}

var testState = {
}

var fsm = new Fsm(initialState);

fsm.set_transition(initialState, 'start', runningJob);
fsm.set_transition(runningJob, 'stop', testState);
fsm.set_transition(testState, 'test', initialState);

fsm.next('start');
fsm.next('stop');
fsm.next('test');
*/
