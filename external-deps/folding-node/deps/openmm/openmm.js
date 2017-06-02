var openmm_native = process.binding('openmm_native');

exports.simulate = function(callback) {
  console.log(openmm_native);
  openmm_native.simulation(callback);
}

exports.simulateFromXml = function(options, files) {
  openmm_native.simulateFromXml(options, files);
}

exports.on = function(eventName, callback) {
  openmm_native.on(eventName, callback);
}

exports.pause = function() {
  openmm_native.pause();
}

exports.continue = function() {
  openmm_native.continue();
}

exports.stop = function() {
  openmm_native.stop();
}

exports.getVersionName = function() {
  return openmm_native.getVersionName();
}
