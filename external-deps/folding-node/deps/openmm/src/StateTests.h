// Authors: Yutong Zhao <proteneer@gmail.com>
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License. You may obtain
// a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.

#ifndef STATE_TESTS_H_
#define STATE_TESTS_H_

#include <OpenMM.h>

// Single Precision Tolerances
static double const DEFAULT_FORCE_TOL_KJ_PER_MOL_PER_NM = 5;
static double const DEFAULT_ENERGY_TOL_KJ_PER_MOL = 10.0;

//static double const DEFAULT_FORCE_TOL_KJ_PER_MOL_PER_NM = 0.3;
//static double const DEFAULT_ENERGY_TOL_KJ_PER_MOL = 3.0;

// Double Precision Tolerances
//static double const DEFAULT_FORCE_TOL_KJ_PER_MOL_PER_NM = 0.251; // 0.06 kcal/mol/nm
//static double const DEFAULT_ENERGY_TOL_KJ_PER_MOL = 0.418; // 0.1 kcal/mol/nm
// Usage lets us compare the state internals of two platforms to test their differences.
namespace StateTests {

void checkForNans(const OpenMM::State& a);
void checkForDiscrepancies(const OpenMM::State &a);
void compareEnergies(const OpenMM::State& a, const OpenMM::State& b, double tolerance = DEFAULT_ENERGY_TOL_KJ_PER_MOL);
void compareForces(const OpenMM::State& a, const OpenMM::State& b, double tolerance = DEFAULT_FORCE_TOL_KJ_PER_MOL_PER_NM);
void compareForcesAndEnergies(const OpenMM::State& a, const OpenMM::State &b, 
    double forceTolerance = DEFAULT_FORCE_TOL_KJ_PER_MOL_PER_NM, double energyTolerance = DEFAULT_ENERGY_TOL_KJ_PER_MOL);

}

#endif