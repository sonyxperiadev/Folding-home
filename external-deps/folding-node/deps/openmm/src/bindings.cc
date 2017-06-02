#ifndef BINDINGS_CC
#define BINDINGS_CC

/**
 * bindings.cc
 * This class provides a base initialization point when the native library is imported from nodejs' side.
 */

#include <node.h>
#include <v8.h>
#include <node_buffer.h>
#include <stdio.h>
#include <stdlib.h>
#include <openmm/serialization/XmlSerializer.h>
#include <node_object_wrap.h>
#include <istream>
#include <cstdio>
#include <iostream>
#include <map>
#include <nan.h>

#include "OpenMM.h"
#include "OpenMMCore.h"
#include "ExitSignal.h"
#include "v8-util.h"

#ifdef _WIN32
#include <io.h>
#else
#include <unistd.h>
#endif

using namespace v8;
using namespace node;
using namespace std;

/**
 * The simulation thread baton. This will be used to send data to the thread that simulation will occur.
 */
static WorkerThreadBaton* baton;

/**
 * A UV Library async_t object. This is used to create and managed the uv threads.
 */
static uv_async_t async_task;

/**
 * A map containing all the event listeners registered on RegisterEvent function.
 * These functions are called when its corresponding event is emitted.
 */
static map<string, CopyablePersistentTraits<Function>::CopyablePersistent> event_callbacks;

/**
 * The OpenMMCore. This object is used to perform simulations.
 */
static OpenMMCore* core;


/**
 * This function initializes and performs the OpenMM simulation.
 * @param request A libuv request object.
 */
void doSimulateFromXmlAsync(uv_work_t* request) {
    WorkerThreadBaton* worker = static_cast<WorkerThreadBaton*>(request->data);

    baton = worker;

    map<string, string> contextProperties;
    // We don't have any context properties for now...

    try {
        core = new OpenMMCore("", contextProperties, cout);

        core->setSettings(baton->options, baton->files);
        // After settings are set, we can delete our pointers
        delete baton->files;
        delete baton->options;

        // Later, we could change this funtion in OpenMMCore
        core->startStream("", "", "", "");

        core->main(); // loop
    } catch (std::runtime_error &e) {
        cerr << "Native Runtime Exception" << e.what() << endl;
        exit(1);
    } catch (std::exception &e) {
        cerr << "Native Exception" << e.what() << endl;
        exit(1);
    } catch (...) {
        cerr << "Unhandled Native Exception" << endl;
        exit(1);
    }
}

/**
 * This function is executed right after doSimulateFromXmlAsync is done.
 * @param request A libuv request object.
 * @param status The status code which the doSimulateFromXmlAsync thread exited.
 */
void doSimulateFromXmlAfterAsync(uv_work_t* request, int status) {
    // Send a stopStream event
    core->stopStream();

    cout << "Cleaning up..." << endl;
    //Cleanup functions
    for ( map<string,CopyablePersistentTraits<Function>::CopyablePersistent >::iterator it = event_callbacks.begin(); it!= event_callbacks.end(); ++it ) {
        if (!it->second.IsEmpty()) {
            it->second.Reset();
        }
    }
    event_callbacks.clear();
    delete baton;

    cout << "OpenMM execution finished." << endl;
}

/**
 * This function executes a event listener function specified on the given EmitEventBaton.
 * @param event_baton The baton to extract the emitted event information.
 */
void CallEventSync(EmitEventBaton* event_baton) {
    if (baton->event_callbacks->find(event_baton->eventName) != baton->event_callbacks->end()) {
        Nan::HandleScope scope;
        Local<Value>* localArguments;
        int argumentCount;

        if (event_baton->argument.empty()) {
            localArguments = new Local<Value>[0];
            localArguments[0] = Nan::Undefined();
            argumentCount = 0;
        } else {
            localArguments = new Local<Value>[1];
            argumentCount  = 1;
            localArguments[0] = Nan::CopyBuffer((char*)event_baton->argument.data(), event_baton->argument.size()).ToLocalChecked();
            cout << "CallEventSync 1" << endl;
        }

        Isolate* isolate = Isolate::GetCurrent();
        Local<Function> function = Local<Function>::New(isolate, baton->event_callbacks->at(event_baton->eventName));
        Nan::Callback callback(function);
        callback.Call(argumentCount, localArguments);
        cout << "CallEventSync 2" << endl;

        delete event_baton;
        delete[] localArguments;
    }
}

/**
 * This functions is called by libuv to emit an event.
 */
void EmitEventAsync(uv_async_t *handle) {
    EmitEventBaton* event_baton = static_cast<EmitEventBaton*>(handle->data);
    CallEventSync(event_baton);
}

/**
 * This function is called by the NodeJS layer to start the simulation.
 * It creates a WorkerThreadBaton and sends it to libuv to execute the simulation in another thread.
 * @param args The NodeJS arguments. We currently accept only 2 parameters, the first being a String and the second a NodeJS Object.
 */
void SimulateFromXml(const Nan::FunctionCallbackInfo<Value>& args) {
    Nan::EscapableHandleScope scope;

    if (args.Length() != 2 || !(args[0]->IsString() && args[1]->IsObject())) { // Assert arguments
        cout << "SimulateFromXml: Invalid Arguments... Exiting..." << endl;
        scope.Escape(Nan::Undefined());
        return;
    }

    // Creates a baton and puts all the data that the worker thread will need
    baton = new WorkerThreadBaton();
    baton->request.data = baton;
    baton->event_callbacks = &event_callbacks;
    baton->options = new string(*Nan::Utf8String(args[0]->ToString()));
    baton->files = new map<string, string>();

    Local<Object> files = args[1]->ToObject();
    Local<Array> props = files->GetPropertyNames();
    for(int i = 0; i < props->Length(); i++) {
        if (props->Get(i)->IsString()) {
            Local<String> key = props->Get(i)->ToString();
            if (files->Get(key)->IsString()) {
                Nan::Utf8String val(files->Get(key)->ToString());
                (*(baton->files))[string(*Nan::Utf8String(key))] = string(*val);
            }
        }
    }

    uv_loop_t *loop;

    loop = uv_default_loop();

    // Defines the starting point of tasks sent to async_task to be the EmitEventAsync function
    uv_async_init(loop, &async_task, EmitEventAsync);

    // Starts the worker thread
    uv_queue_work(loop,
                  &baton->request,
                  doSimulateFromXmlAsync,
                  doSimulateFromXmlAfterAsync);

    scope.Escape(Nan::Undefined());
}

/**
 * Register a event.
 * @param args The NodeJS arguments. We currently require the first argument to be a string (the event to register) and the second to be a NodeJS Function (a event listener).
 */
void RegisterEvent(const Nan::FunctionCallbackInfo<Value>& args) {
    Nan::EscapableHandleScope scope;
    if (!args[0]->IsString() ||
        !args[1]->IsFunction()) {
        Nan::ThrowTypeError("Wrong arguments");
        scope.Escape(Nan::Undefined());
        return;
    }

    string eventName = *Nan::Utf8String(args[0]->ToString());

    if (event_callbacks.find(eventName) != event_callbacks.end()) {
        event_callbacks[eventName].Reset();
    }

    Isolate* isolate = args.GetIsolate();
    Local<Function> callback = Local<Function>::Cast(args[1]);
    event_callbacks[eventName].Reset(isolate, callback);

    scope.Escape(Nan::Undefined());
}

/**
 * Pauses the integration. This causes the simulation loop to hang in a while loop until continue or stop is called.
 * @param args The NodeJS Arguments. We currently do not use any arguments.
 */
void Pause(const Nan::FunctionCallbackInfo<Value>& args) {
    Nan::EscapableHandleScope scope;
    if (core != NULL) {
        core->pauseIntegration();
    } else {
      cout << "Could not pause... Core has not been initialized..." << endl;
    }
    scope.Escape(Nan::Undefined());
}

/**
 * Continues the integration. This causes the simulation loop to exit the paused state.
 * @param args The NodeJS Arguments. We currently do not use any arguments.
 */
void Continue(const Nan::FunctionCallbackInfo<Value>& args) {
    Nan::EscapableHandleScope scope;
    if (core != NULL) {
        core->continueIntegration();
    } else {
      cout << "Could not continue... Core has not been initialized..." << endl;
    }
    scope.Escape(Nan::Undefined());
}

/**
 * Stops the integration. This causes the simulation loop to terminate.
 * @param args The NodeJS Arguments. We currently do not use any arguments.
 */
void Stop(const Nan::FunctionCallbackInfo<Value>& args) {
    Nan::EscapableHandleScope scope;
    ExitSignal::forceExit();
    scope.Escape(Nan::Undefined());
}

/**
 * Sends the given baton to be executed in a new thread. The function called by the new thread is EmitEventAsync.
 * This is defined in SimulateFromXml.
 * @param baton The baton containing information regarding the event to be emitted.
 */
void EmitEvent(EmitEventBaton* baton) {
    async_task.data = baton;
    uv_async_send(&async_task);
}

/**
 * Gets the OpenMM version name.
 * @param args The NodeJS Arguments. We currently do not use any arguments.
 */
void GetVersionName(const Nan::FunctionCallbackInfo<v8::Value>& info) {
    string version = OpenMM::Platform::getOpenMMVersion();
    info.GetReturnValue().Set(Nan::New(version).ToLocalChecked());
}

/**
 * Initializes the exports to be used by the NodeJS layer.
 * This registers all native functions that can be called.
 */
void initOpenMM(Local<Object> exports,
              Local<Value> unused,
              Local<Context> context,
              void* priv) {
    Nan::Set(exports,
        Nan::New<String>("simulateFromXml").ToLocalChecked(),
        Nan::New<FunctionTemplate>(SimulateFromXml)->GetFunction());
    Nan::Set(exports,
        Nan::New<String>("on").ToLocalChecked(),
        Nan::New<FunctionTemplate>(RegisterEvent)->GetFunction());
    Nan::Set(exports,
        Nan::New<String>("continue").ToLocalChecked(),
        Nan::New<FunctionTemplate>(Continue)->GetFunction());
    Nan::Set(exports,
        Nan::New<String>("pause").ToLocalChecked(),
        Nan::New<FunctionTemplate>(Pause)->GetFunction());
    Nan::Set(exports,
        Nan::New<String>("stop").ToLocalChecked(),
        Nan::New<FunctionTemplate>(Stop)->GetFunction());
    Nan::Set(exports,
        Nan::New<String>("getVersionName").ToLocalChecked(),
        Nan::New<FunctionTemplate>(GetVersionName)->GetFunction());
}

NODE_MODULE_CONTEXT_AWARE_BUILTIN(openmm_native, initOpenMM)

#endif /* BINDINGS_CC */
