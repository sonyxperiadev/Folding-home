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

#ifndef OPENMM_CORE_HH_
#define OPENMM_CORE_HH_

#include "Core.h"
#include <OpenMM.h>
#include <sstream>
#include <iostream>
#include <map>
#include <string>

class OpenMMCore : public Core {
public:
    OpenMMCore(std::string core_key,
               std::map<std::string, std::string> properties = std::map<std::string, std::string>(),
               std::ostream &logStream = std::cout);
    ~OpenMMCore();

    virtual void main();

    virtual void startStream(const std::string &uri,
                             const std::string &donor_token = "",
                             const std::string &target_id = "",
                             const std::string &proxy_string = "");

    void setProgressUpdateInterval(int interval);

    /* set the checkpoint interval */
    void setCheckpointSendInterval(int interval);

    /* flush the stored checkpoint */
    void flushCheckpoint();

#ifdef FAH_CORE
    std::string wu_dir;
#endif

protected:
    /* check the step and determine if we need to 1) write frame/send frame,
    or 2) send a checkpoint */
    void checkFrameWrite();

    /* get time per frame in seconds */
    int timePerFrame(long long steps_completed) const;

    /* get nanoseconds per day of the current simulation */
    float nsPerDay(long long steps_completed) const;

    /* verify the openmm state */
    void checkState(const OpenMM::State &core_state) const;

    /* set the heartbeat interval */
    void setHeartbeatInterval(int interval);

private:
    void setupSystem(OpenMM::System *system, int randomSeed) const;

    void cleanUp();

    std::map<std::string, std::string> properties_;
    int steps_per_frame_;
    int checkpoint_send_interval_;
    int heartbeat_interval_;
    int progress_update_interval_;
    int start_time_;
    long long current_step_;
    long long last_checkpoint_step_;
    //std::string last_checkpoint_;
    OpenMM::Context* ref_context_;
    OpenMM::Context* core_context_;
    OpenMM::Integrator* ref_intg_;
    OpenMM::Integrator* core_intg_;
    OpenMM::State* initial_state_;
    OpenMM::System* shared_system_;

};

#endif
