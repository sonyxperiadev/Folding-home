#ifndef OPENMM_CUSTOMNONBONDEDFORCE_H_
#define OPENMM_CUSTOMNONBONDEDFORCE_H_

/* -------------------------------------------------------------------------- *
 *                                   OpenMM                                   *
 * -------------------------------------------------------------------------- *
 * This is part of the OpenMM molecular simulation toolkit originating from   *
 * Simbios, the NIH National Center for Physics-Based Simulation of           *
 * Biological Structures at Stanford, funded under the NIH Roadmap for        *
 * Medical Research, grant U54 GM072970. See https://simtk.org.               *
 *                                                                            *
 * Portions copyright (c) 2008-2013 Stanford University and the Authors.      *
 * Authors: Peter Eastman                                                     *
 * Contributors:                                                              *
 *                                                                            *
 * Permission is hereby granted, free of charge, to any person obtaining a    *
 * copy of this software and associated documentation files (the "Software"), *
 * to deal in the Software without restriction, including without limitation  *
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,   *
 * and/or sell copies of the Software, and to permit persons to whom the      *
 * Software is furnished to do so, subject to the following conditions:       *
 *                                                                            *
 * The above copyright notice and this permission notice shall be included in *
 * all copies or substantial portions of the Software.                        *
 *                                                                            *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,   *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL    *
 * THE AUTHORS, CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,    *
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR      *
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE  *
 * USE OR OTHER DEALINGS IN THE SOFTWARE.                                     *
 * -------------------------------------------------------------------------- */

#include "Force.h"
#include "Vec3.h"
#include <map>
#include <set>
#include <utility>
#include <vector>
#include "internal/windowsExport.h"

namespace OpenMM {

/**
 * This class implements nonbonded interactions between particles.  Unlike NonbondedForce, the functional form
 * of the interaction is completely customizable, and may involve arbitrary algebraic expressions and tabulated
 * functions.  It may depend on the distance between particles, as well as on arbitrary global and
 * per-particle parameters.  It also optionally supports periodic boundary conditions and cutoffs for long range interactions.
 *
 * To use this class, create a CustomNonbondedForce object, passing an algebraic expression to the constructor
 * that defines the interaction energy between each pair of particles.  The expression may depend on r, the distance
 * between the particles, as well as on any parameters you choose.  Then call addPerParticleParameter() to define per-particle
 * parameters, and addGlobalParameter() to define global parameters.  The values of per-particle parameters are specified as
 * part of the system definition, while values of global parameters may be modified during a simulation by calling Context::setParameter().
 * 
 * Next, call addParticle() once for each particle in the System to set the values of its per-particle parameters.
 * The number of particles for which you set parameters must be exactly equal to the number of particles in the
 * System, or else an exception will be thrown when you try to create a Context.  After a particle has been added,
 * you can modify its parameters by calling setParticleParameters().  This will have no effect on Contexts that already exist
 * unless you call updateParametersInContext().
 *
 * CustomNonbondedForce also lets you specify "exclusions", particular pairs of particles whose interactions should be
 * omitted from force and energy calculations.  This is most often used for particles that are bonded to each other.
 *
 * As an example, the following code creates a CustomNonbondedForce that implements a 12-6 Lennard-Jones potential:
 *
 * <tt>CustomNonbondedForce* force = new CustomNonbondedForce("4*epsilon*((sigma/r)^12-(sigma/r)^6); sigma=0.5*(sigma1+sigma2); epsilon=sqrt(epsilon1*epsilon2)");</tt>
 *
 * This force depends on two parameters: sigma and epsilon.  The following code defines these as per-particle parameters:
 *
 * <tt><pre>
 * force->addPerParticleParameter("sigma");
 * force->addPerParticleParameter("epsilon");
 * </pre></tt>
 *
 * The expression <i>must</i> be symmetric with respect to the two particles.  It typically will only be evaluated once
 * for each pair of particles, and no guarantee is made about which particle will be identified as "particle 1".  In the
 * above example, the energy only depends on the products sigma1*sigma2 and epsilon1*epsilon2, both of which are unchanged
 * if the labels 1 and 2 are reversed.  In contrast, if it depended on the difference sigma1-sigma2, the results would
 * be undefined, because reversing the labels 1 and 2 would change the energy.
 * 
 * CustomNonbondedForce can operate in two modes.  By default, it computes the interaction of every particle in the System
 * with every other particle.  Alternatively, you can restrict it to only a subset of particle pairs.  To do this, specify
 * one or more "interaction groups".  An interaction group consists of two sets of particles that should interact with
 * each other.  Every particle in the first set interacts with every particle in the second set.  For example, you might use
 * this feature to compute a solute-solvent interaction energy, while omitting all interactions between two solute atoms
 * or two solvent atoms.
 * 
 * To create an interaction group, call addInteractionGroup().  You may add as many interaction groups as you want.
 * Be aware of the following:
 * 
 * <ul>
 * <li>Exclusions are still taken into account, so the interactions between excluded pairs are omitted.</li>
 * <li>Likewise, a particle will never interact with itself, even if it appears in both sets of an interaction group.</li>
 * <li>If a particle pair appears in two different interaction groups, its interaction will be computed twice.  This is
 * sometimes useful, but be aware of it so you do not accidentally create unwanted duplicate interactions.</li>
 * <li>If you do not add any interaction groups to a CustomNonbondedForce, it operates in the default mode where every
 * particle interacts with every other particle.</li>
 * </ul>
 * 
 * When using a cutoff, by default the interaction is sharply truncated at the cutoff distance.
 * Optionally you can instead use a switching function to make the interaction smoothly go to zero over a finite
 * distance range.  To enable this, call setUseSwitchingFunction().  You must also call setSwitchingDistance()
 * to specify the distance at which the interaction should begin to decrease.  The switching distance must be
 * less than the cutoff distance.  Of course, you could also incorporate the switching function directly into your
 * energy expression, but there are several advantages to keeping it separate.  It makes your energy expression simpler
 * to write and understand.  It allows you to use the same energy expression with or without a cutoff.  Also, when using
 * a long range correction (see below), separating out the switching function allows the correction to be calculated
 * more accurately.
 * 
 * Another optional feature of this class is to add a contribution to the energy which approximates the effect of all
 * interactions beyond the cutoff in a periodic system.  When running a simulation at constant pressure, this can improve
 * the quality of the result.  Call setUseLongRangeCorrection() to enable it.
 * 
 * Computing the long range correction takes negligible work in each time step, but it does require an expensive precomputation
 * at the start of the simulation.  Furthermore, that precomputation must be repeated every time a global parameter changes
 * (or when you modify per-particle parameters by calling updateParametersInContext()).  This means that if parameters change
 * frequently, the long range correction can be very slow.  For this reason, it is disabled by default.
 * 
 * Expressions may involve the operators + (add), - (subtract), * (multiply), / (divide), and ^ (power), and the following
 * functions: sqrt, exp, log, sin, cos, sec, csc, tan, cot, asin, acos, atan, sinh, cosh, tanh, erf, erfc, min, max, abs, step, delta.  All trigonometric functions
 * are defined in radians, and log is the natural logarithm.  step(x) = 0 if x is less than 0, 1 otherwise.  delta(x) = 1 if x is 0, 0 otherwise.  The names of per-particle parameters
 * have the suffix "1" or "2" appended to them to indicate the values for the two interacting particles.  As seen in the above example,
 * the expression may also involve intermediate quantities that are defined following the main expression, using ";" as a separator.
 *
 * In addition, you can call addFunction() to define a new function based on tabulated values.  You specify a vector of
 * values, and a natural spline is created from them.  That function can then appear in the expression.
 */

class OPENMM_EXPORT CustomNonbondedForce : public Force {
public:
    /**
     * This is an enumeration of the different methods that may be used for handling long range nonbonded forces.
     */
    enum NonbondedMethod {
        /**
         * No cutoff is applied to nonbonded interactions.  The full set of N^2 interactions is computed exactly.
         * This necessarily means that periodic boundary conditions cannot be used.  This is the default.
         */
        NoCutoff = 0,
        /**
         * Interactions beyond the cutoff distance are ignored.
         */
        CutoffNonPeriodic = 1,
        /**
         * Periodic boundary conditions are used, so that each particle interacts only with the nearest periodic copy of
         * each other particle.  Interactions beyond the cutoff distance are ignored.
         */
        CutoffPeriodic = 2,
    };
    /**
     * Create a CustomNonbondedForce.
     *
     * @param energy    an algebraic expression giving the interaction energy between two particles as a function
     *                  of r, the distance between them, as well as any global and per-particle parameters
     */
    explicit CustomNonbondedForce(const std::string& energy);
    /**
     * Get the number of particles for which force field parameters have been defined.
     */
    int getNumParticles() const {
        return particles.size();
    }
    /**
     * Get the number of particle pairs whose interactions should be excluded.
     */
    int getNumExclusions() const {
        return exclusions.size();
    }
    /**
     * Get the number of per-particle parameters that the interaction depends on.
     */
    int getNumPerParticleParameters() const {
        return parameters.size();
    }
    /**
     * Get the number of global parameters that the interaction depends on.
     */
    int getNumGlobalParameters() const {
        return globalParameters.size();
    }
    /**
     * Get the number of tabulated functions that have been defined.
     */
    int getNumFunctions() const {
        return functions.size();
    }
    /**
     * Get the number of interaction groups that have been defined.
     */
    int getNumInteractionGroups() const {
        return interactionGroups.size();
    }
    /**
     * Get the algebraic expression that gives the interaction energy between two particles
     */
    const std::string& getEnergyFunction() const;
    /**
     * Set the algebraic expression that gives the interaction energy between two particles
     */
    void setEnergyFunction(const std::string& energy);
    /**
     * Get the method used for handling long range nonbonded interactions.
     */
    NonbondedMethod getNonbondedMethod() const;
    /**
     * Set the method used for handling long range nonbonded interactions.
     */
    void setNonbondedMethod(NonbondedMethod method);
    /**
     * Get the cutoff distance (in nm) being used for nonbonded interactions.  If the NonbondedMethod in use
     * is NoCutoff, this value will have no effect.
     *
     * @return the cutoff distance, measured in nm
     */
    double getCutoffDistance() const;
    /**
     * Set the cutoff distance (in nm) being used for nonbonded interactions.  If the NonbondedMethod in use
     * is NoCutoff, this value will have no effect.
     *
     * @param distance    the cutoff distance, measured in nm
     */
    void setCutoffDistance(double distance);
    /**
     * Get whether a switching function is applied to the interaction.  If the nonbonded method is set
     * to NoCutoff, this option is ignored.
     */
    bool getUseSwitchingFunction() const;
    /**
     * Set whether a switching function is applied to the interaction.  If the nonbonded method is set
     * to NoCutoff, this option is ignored.
     */
    void setUseSwitchingFunction(bool use);
    /**
     * Get the distance at which the switching function begins to reduce the interaction.  This must be
     * less than the cutoff distance.
     */
    double getSwitchingDistance() const;
    /**
     * Set the distance at which the switching function begins to reduce the interaction.  This must be
     * less than the cutoff distance.
     */
    void setSwitchingDistance(double distance);
    /**
     * Get whether to add a correction to the energy to compensate for the cutoff and switching function.
     * This has no effect if periodic boundary conditions are not used.
     */
    bool getUseLongRangeCorrection() const;
    /**
     * Set whether to add a correction to the energy to compensate for the cutoff and switching function.
     * This has no effect if periodic boundary conditions are not used.
     */
    void setUseLongRangeCorrection(bool use);
    /**
     * Add a new per-particle parameter that the interaction may depend on.
     *
     * @param name     the name of the parameter
     * @return the index of the parameter that was added
     */
    int addPerParticleParameter(const std::string& name);
    /**
     * Get the name of a per-particle parameter.
     *
     * @param index     the index of the parameter for which to get the name
     * @return the parameter name
     */
    const std::string& getPerParticleParameterName(int index) const;
    /**
     * Set the name of a per-particle parameter.
     *
     * @param index          the index of the parameter for which to set the name
     * @param name           the name of the parameter
     */
    void setPerParticleParameterName(int index, const std::string& name);
    /**
     * Add a new global parameter that the interaction may depend on.
     *
     * @param name             the name of the parameter
     * @param defaultValue     the default value of the parameter
     * @return the index of the parameter that was added
     */
    int addGlobalParameter(const std::string& name, double defaultValue);
    /**
     * Get the name of a global parameter.
     *
     * @param index     the index of the parameter for which to get the name
     * @return the parameter name
     */
    const std::string& getGlobalParameterName(int index) const;
    /**
     * Set the name of a global parameter.
     *
     * @param index          the index of the parameter for which to set the name
     * @param name           the name of the parameter
     */
    void setGlobalParameterName(int index, const std::string& name);
    /**
     * Get the default value of a global parameter.
     *
     * @param index     the index of the parameter for which to get the default value
     * @return the parameter default value
     */
    double getGlobalParameterDefaultValue(int index) const;
    /**
     * Set the default value of a global parameter.
     *
     * @param index          the index of the parameter for which to set the default value
     * @param name           the default value of the parameter
     */
    void setGlobalParameterDefaultValue(int index, double defaultValue);
    /**
     * Add the nonbonded force parameters for a particle.  This should be called once for each particle
     * in the System.  When it is called for the i'th time, it specifies the parameters for the i'th particle.
     *
     * @param parameters    the list of parameters for the new particle
     * @return the index of the particle that was added
     */
    int addParticle(const std::vector<double>& parameters);
    /**
     * Get the nonbonded force parameters for a particle.
     *
     * @param index       the index of the particle for which to get parameters
     * @param parameters  the list of parameters for the specified particle
     */
    void getParticleParameters(int index, std::vector<double>& parameters) const;
    /**
     * Set the nonbonded force parameters for a particle.
     *
     * @param index       the index of the particle for which to set parameters
     * @param parameters  the list of parameters for the specified particle
     */
    void setParticleParameters(int index, const std::vector<double>& parameters);
    /**
     * Add a particle pair to the list of interactions that should be excluded.
     *
     * @param particle1  the index of the first particle in the pair
     * @param particle2  the index of the second particle in the pair
     * @return the index of the exclusion that was added
     */
    int addExclusion(int particle1, int particle2);
    /**
     * Get the particles in a pair whose interaction should be excluded.
     *
     * @param index      the index of the exclusion for which to get particle indices
     * @param particle1  the index of the first particle in the pair
     * @param particle2  the index of the second particle in the pair
     */
    void getExclusionParticles(int index, int& particle1, int& particle2) const;
    /**
     * Set the particles in a pair whose interaction should be excluded.
     *
     * @param index      the index of the exclusion for which to set particle indices
     * @param particle1  the index of the first particle in the pair
     * @param particle2  the index of the second particle in the pair
     */
    void setExclusionParticles(int index, int particle1, int particle2);
    /**
     * Add a tabulated function that may appear in the energy expression.
     *
     * @param name           the name of the function as it appears in expressions
     * @param values         the tabulated values of the function f(x) at uniformly spaced values of x between min and max.
     *                       The function is assumed to be zero for x &lt; min or x &gt; max.
     * @param min            the value of the independent variable corresponding to the first element of values
     * @param max            the value of the independent variable corresponding to the last element of values
     * @return the index of the function that was added
     */
    int addFunction(const std::string& name, const std::vector<double>& values, double min, double max);
    /**
     * Get the parameters for a tabulated function that may appear in the energy expression.
     *
     * @param index          the index of the function for which to get parameters
     * @param name           the name of the function as it appears in expressions
     * @param values         the tabulated values of the function f(x) at uniformly spaced values of x between min and max.
     *                       The function is assumed to be zero for x &lt; min or x &gt; max.
     * @param min            the value of the independent variable corresponding to the first element of values
     * @param max            the value of the independent variable corresponding to the last element of values
     */
    void getFunctionParameters(int index, std::string& name, std::vector<double>& values, double& min, double& max) const;
    /**
     * Set the parameters for a tabulated function that may appear in algebraic expressions.
     *
     * @param index          the index of the function for which to set parameters
     * @param name           the name of the function as it appears in expressions
     * @param values         the tabulated values of the function f(x) at uniformly spaced values of x between min and max.
     *                       The function is assumed to be zero for x &lt; min or x &gt; max.
     * @param min            the value of the independent variable corresponding to the first element of values
     * @param max            the value of the independent variable corresponding to the last element of values
     */
    void setFunctionParameters(int index, const std::string& name, const std::vector<double>& values, double min, double max);
    /**
     * Add an interaction group.  An interaction will be computed between every particle in set1 and every particle in set2.
     * 
     * @param set1    the first set of particles forming the interaction group
     * @param set2    the second set of particles forming the interaction group
     * @return the index of the interaction group that was added
     */
    int addInteractionGroup(const std::set<int>& set1, const std::set<int>& set2);
    /**
     * Get the parameters for an interaction group.
     * 
     * @param index   the index of the interaction group for which to get parameters
     * @param set1    the first set of particles forming the interaction group
     * @param set2    the second set of particles forming the interaction group
     */
    void getInteractionGroupParameters(int index, std::set<int>& set1, std::set<int>& set2) const;
    /**
     * Set the parameters for an interaction group.
     * 
     * @param index   the index of the interaction group for which to set parameters
     * @param set1    the first set of particles forming the interaction group
     * @param set2    the second set of particles forming the interaction group
     */
    void setInteractionGroupParameters(int index, const std::set<int>& set1, const std::set<int>& set2);
    /**
     * Update the per-particle parameters in a Context to match those stored in this Force object.  This method provides
     * an efficient method to update certain parameters in an existing Context without needing to reinitialize it.
     * Simply call setParticleParameters() to modify this object's parameters, then call updateParametersInContext()
     * to copy them over to the Context.
     * 
     * This method has several limitations.  The only information it updates is the values of per-particle parameters.
     * All other aspects of the Force (the energy function, nonbonded method, cutoff distance, etc.) are unaffected and can
     * only be changed by reinitializing the Context.  Also, this method cannot be used to add new particles, only to change
     * the parameters of existing ones.
     */
    void updateParametersInContext(Context& context);
protected:
    ForceImpl* createImpl() const;
private:
    class ParticleInfo;
    class PerParticleParameterInfo;
    class GlobalParameterInfo;
    class ExclusionInfo;
    class FunctionInfo;
    class InteractionGroupInfo;
    NonbondedMethod nonbondedMethod;
    double cutoffDistance, switchingDistance;
    bool useSwitchingFunction, useLongRangeCorrection;
    std::string energyExpression;
    std::vector<PerParticleParameterInfo> parameters;
    std::vector<GlobalParameterInfo> globalParameters;
    std::vector<ParticleInfo> particles;
    std::vector<ExclusionInfo> exclusions;
    std::vector<FunctionInfo> functions;
    std::vector<InteractionGroupInfo> interactionGroups;
};

/**
 * This is an internal class used to record information about a particle.
 * @private
 */
class CustomNonbondedForce::ParticleInfo {
public:
    std::vector<double> parameters;
    ParticleInfo() {
    }
    ParticleInfo(const std::vector<double>& parameters) : parameters(parameters) {
    }
};

/**
 * This is an internal class used to record information about a per-particle parameter.
 * @private
 */
class CustomNonbondedForce::PerParticleParameterInfo {
public:
    std::string name;
    PerParticleParameterInfo() {
    }
    PerParticleParameterInfo(const std::string& name) : name(name) {
    }
};

/**
 * This is an internal class used to record information about a global parameter.
 * @private
 */
class CustomNonbondedForce::GlobalParameterInfo {
public:
    std::string name;
    double defaultValue;
    GlobalParameterInfo() {
    }
    GlobalParameterInfo(const std::string& name, double defaultValue) : name(name), defaultValue(defaultValue) {
    }
};

/**
 * This is an internal class used to record information about an exclusion.
 * @private
 */
class CustomNonbondedForce::ExclusionInfo {
public:
    int particle1, particle2;
    ExclusionInfo() {
        particle1 = particle2 = -1;
    }
    ExclusionInfo(int particle1, int particle2) :
        particle1(particle1), particle2(particle2) {
    }
};

/**
 * This is an internal class used to record information about a tabulated function.
 * @private
 */
class CustomNonbondedForce::FunctionInfo {
public:
    std::string name;
    std::vector<double> values;
    double min, max;
    FunctionInfo() {
    }
    FunctionInfo(const std::string& name, const std::vector<double>& values, double min, double max) :
        name(name), values(values), min(min), max(max) {
    }
};

/**
 * This is an internal class used to record information about an interaction group.
 * @private
 */
class CustomNonbondedForce::InteractionGroupInfo {
public:
    std::set<int> set1, set2;
    InteractionGroupInfo() {
    }
    InteractionGroupInfo(const std::set<int>& set1, const std::set<int>& set2) :
        set1(set1), set2(set2) {
    }
};

} // namespace OpenMM

#endif /*OPENMM_CUSTOMNONBONDEDFORCE_H_*/
