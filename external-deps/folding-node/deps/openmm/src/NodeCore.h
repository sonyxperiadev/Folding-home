#ifndef NODE_CORE_H_
#define NODE_CORE_H_

#include <string>
#include <map>
#include <ostream>
#include <sstream>
#include <iostream>

#include "picojson.h"

#include <uv.h>
#include <node.h>
#include <v8.h>
#include <node_buffer.h>
#include "openmm/serialization/XmlSerializer.h"

using namespace v8;
using namespace node;

/**
 * A baton that provides the required information to start the simulation.
 */
struct WorkerThreadBaton {
    /* The libuv request object. This is used for thread callbacks. */
    uv_work_t request;
    /* A JSON Encoded Options */
    std::string *options;
    /* A JSON Encoded Files */
    std::map<std::string, std::string> *files;
    /* The Event Callback Functions */
    std::map<std::string, CopyablePersistentTraits<Function>::CopyablePersistent > *event_callbacks;
};

/**
 *  A Baton provides the properties to send an event to the nodejs interface
 */
struct EmitEventBaton {
    /* The name of the event that originated this baton */
    std::string eventName;
    /* The event's argument (Optional). For multiple arguments, use JSON-Encoding */
    std::string argument;
};

/**
 * A Core provides the basic interface for talking to the Siegetank Backend.
 *
 * The core contains basic functionality such as starting a stream, stopping a
 * stream, sending frames, checkpoints, and heartbeats.
 *
 */

class Core {
public:
    /* Core's constructor. Current implementation will ignore core_key and
       will use log for any logging purposes
     */
    Core(std::string core_key, std::ostream& log = std::cout);

    ~Core();

    /* Initialize the core with the given options and files */
    void setSettings(std::string *options, std::map<std::string, std::string> *files);

    /* Main MD loop */
    virtual void main();

    std::ostream &logStream;

    /* Disengage the core from the stream and destroy the session */
    void stopStream(std::string error_msg = "");

    /* Pauses the integration */
    void pauseIntegration();

    /* Continues the integration from the paused state */
    void continueIntegration();

    /* Checks if the pause flag is on */
    bool isPaused();

protected:

    /* Empty function block. It will later be overriden by one of its subclasses */
    virtual void startStream(const std::string &cc_uri,
                             const std::string &donor_token = "",
                             const std::string &target_id = "",
                             const std::string &proxy_string = "");

    /* Send frame files to the WS.  This method will ignore the third
       parameter and will send a JSON-Enconded value of its files and
       frame_count to the nodejs layer.
    */
    void sendFrame(const std::map<std::string, std::string> &files,
                   int frame_count=1, bool gzip=false) const;

    /* Send checkpoint files to the WS. This method will ignore the second
       parameter and will send a JSON-Enconded value to the nodejs layer.
    */
    void sendCheckpoint(const std::map<std::string, std::string> &files,
                        double frames, bool gzip=false) const;

    /* Send a heartbeat */
    void sendHeartbeat() const;

    /* get a specific option */
    template<typename T>
    T getOption(const std::string &key) const {
        std::stringstream ss(options_);
        picojson::value value; ss >> value;
        picojson::value::object &object = value.get<picojson::object>();
        return object[key].get<T>();
    }

    /* The map of the files received on /core/assign */
    std::map<std::string, std::string> files_;

    /* The target id. This is currently not being used */
    std::string target_id_;

    /* The stream id. This is currently not being used */
    std::string stream_id_;

    /* The paused flag */
    bool isPaused_;

private:
    /* The options received on /core/assign */
    std::string options_;
};

#endif
