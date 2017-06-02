var openmm = require('openmm');

console.log('>> openmm.simulate =============================================================');

openmm.simulateFromXml('./DHFR_SYSTEM_EXPLICIT.xml',
                       './DHFR_STATE_EXPLICIT.xml',
                       './DHFR_INTEGRATOR_EXPLICIT.xml',
                       function (err, data) {
                        console.log('>> callback =============================================================');
                        console.log(err);
                        console.log(data);
                        console.log('<< callback =============================================================');
                       });

console.log('<< openmm.simulate =============================================================');