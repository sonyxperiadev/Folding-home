#include <fstream>
#include <string>
#include <streambuf>
#include <sstream>
#include <stdexcept>
#include <algorithm>
#include <locale>
#include <cstdlib>
#include <cstdio>
#ifdef ARM
#include <unistd.h>
#endif
#include <ctime>

#include "NodeCore.h"
#include "openmm/Platform.h"

using namespace std;
using namespace OpenMM;

void EmitEvent(EmitEventBaton*);
void CallEventSync(EmitEventBaton*);

Core::Core(std::string core_key, std::ostream& log) :
    logStream(log), isPaused_(false) {
    logStream << "\n\nconstructing base core\n\n" << endl;
}

Core::~Core() {
}


void Core::setSettings(string *options, map<string, string> *files) {
    options_ = *options;
    files_ = *files;
}

void Core::startStream(const string &cc_uri,
                       const string &donor_token,
                       const string &target_id,
                       const string &proxy_string) {
    // Empty... the options should already be set by calling setSettings.
}

void Core::sendFrame(const map<string, string> &files,
    int frame_count, bool gzip) const {

    EmitEventBaton* emitEventBaton = new EmitEventBaton();
    emitEventBaton->eventName = "sendFrame";

    stringstream frame_count_str;
    frame_count_str << frame_count;
    string message;
    message += "{";
    message += "\"frames\":"+frame_count_str.str()+",";
    message += "\"files\":{";
    for(map<string, string>::const_iterator it=files.begin();
        it != files.end(); it++) {
        string filename = it->first;
        string filedata = it->second;

        // Encode to JSON filedata
        picojson::value json_value(filedata);
        filedata = json_value.serialize();


        if(it != files.begin())
            message += ",";
        message += "\""+filename+"\"";
        message += ":";
        message += filedata;
    }
    message += "}}";

    emitEventBaton->argument = message;

    EmitEvent(emitEventBaton);
}

void Core::sendCheckpoint(const map<string, string> &files, double frames,
    bool gzip) const {

    EmitEventBaton* emitEventBaton = new EmitEventBaton();
    emitEventBaton->eventName = "sendCheckpoint";

    string message;
    message += "{\"files\":{";
    for(map<string, string>::const_iterator it=files.begin();
        it != files.end(); it++) {
        string filename = it->first;
        string filedata = it->second;

        // Encode to JSON filedata
        picojson::value json_value(filedata);
        filedata = json_value.serialize();


        if(it != files.begin())
            message += ",";
        message += "\""+filename+"\"";
        message += ":";
        message += filedata;
    }
    message += "},";
    message += "\"frames\":";
    stringstream frames_string;
    frames_string << frames;
    message += frames_string.str();
    message += "}";

    emitEventBaton->argument = message;

    EmitEvent(emitEventBaton);
}

void Core::pauseIntegration() {
    logStream << "Pause Core..." << endl;
    isPaused_ = true;
}

void Core::continueIntegration() {
    logStream << "Continue Core..." << endl;
    isPaused_ = false;
}

bool Core::isPaused() {
    return isPaused_;
}

void Core::stopStream(string err_msg) {
    EmitEventBaton* emitEventBaton = new EmitEventBaton();
    emitEventBaton->eventName = "stopStream";
    emitEventBaton->argument = err_msg;
    CallEventSync(emitEventBaton);
}

void Core::sendHeartbeat() const {
    EmitEventBaton* emitEventBaton = new EmitEventBaton();
    emitEventBaton->eventName = "sendHeartbeat";
    EmitEvent(emitEventBaton);
}

void Core::main() {
}
