# Folding@Home
![Folding logo](https://s3-eu-west-1.amazonaws.com/folding-page/imgs/ic_launcher.png)

Folding@Home is a project that aims to harness the spare computing power of mobile devices to help with computing-intensive research.

# Build and Run folding-android
Folding@Home can be executed on Android 19 (4.4 - KitKat) or higher.

The project has native parts (compiled with NDK for armv7) but the libraries are bundled with the source code, so it is normally not necessary to install the NDK to run Folding@Home from Android Studio.
## Android Studio
1. Import the project into Android Studio.
2. Run the app with the “run” button. (alt+shift+f10)

## Command line
1. Run the following command:
```
gradlew assembleDebug
```
2. The generated APK is located at: *“...\app\build\outputs\apk”*

### Gradle tasks
You can see all the build tasks by running:
```
gradlew tasks
```

# How it works
The Android app creates a Service which runs a Thread that executes the node process and responds its events.

## Understanding the code
### SummaryActivity
*(com/sonymobile/androidapp/gridcomputing/activities/SummaryActivity.java)*

This Activity represents the main point of interaction between the user and the Folding@Home app. It handles user input (Ex: Enable/Disable Folding@Home) and handle the information presented to the user (Ex: Conditions that must be met to keep Folding@Home executing, current contribution information).

### ComputeService
*(com/sonymobile/androidapp/gridcomputing/service/ComputeService.java)*

We don’t want to stop our contribution when the user leaves the app, that’s where the ComputeService enters. The ComputeService is responsible to create the ComputeEnvironment which runs the Folding process in a separate thread while storing and processing all the necessary data from the current execution.

### ComputeEnvironment
*(com/sonymobile/androidapp/gridcomputing/service/ComputeEnvironment.java)*

The ComputeEnvironment is responsible to execute the Node.js process. All communication between the Android App and the Node process happens here.

This service encapsulates the native part of the client indirectly, because it spawns the Node.js subprocess, and communicates with it.

### Native Components
The following native components are part of the software:

* A version of Node.js, ported to Android and modified to be safer against certain classes of attacks (e.g. filesystem manipulation APIs are restricted so a rogue script that is downloaded and executed cannot escape easily its sandbox).
* A version of the OpenMM library. Even though Folding@Home is a generic distributed processing platform, the Stanford’s Folding@Home jobs depend on this particular library. Other projects might employ pure Javascript-based jobs, or WebGL-based jobs.
* Javascript bindings for the OpenMM library. The Stanford’s Folding@Home jobs are basically scripts that intermediate the network communication between the remote PM server and the native OpenMM library.
* A version of the FFTW library. This is a dependency from OpenMM. The currently bundled version (3.2.2) is non-GPL, a license has been bought to allow closed-source distribution of this software.

Currently, the native components have been compiled to the target armv7 (ARM 32 bits). Ideally, the targets Intel and ARM 64-bit should be supported as well. They are not currently supported because compiling FFTW in armv8 needs an update to 3.3.x, that would cost another license. If Folding@Home is made open-source in the future, in a GPL-compatible way, the GPL version of FFTW could be used instead.

Native components are build using the build scripts. Following the steps below.

1. Build OpenMM.
```
$ ./apply_patches
$ ./build_openmm
```
2. Copy .so files to the node project
```
$ cp *.so <folding-node>/deps/openmm/libraries
```
3. Go the <folding-node> directory and run.
```
$ ./apply_patches
```
4. Compile node.
```
$ cd folding-nome/node
$ ./android-configure {path-ndk}
$ make -j5
```

### ConditionsHandler
*(com/sonymobile/androidapp/gridcomputing/conditions/ConditionsHandler.java)*

This class is responsible to watch the necessary conditions to run Folding@Home.
They are: Battery level, Wi-Fi state, Charging state, Play/Pause switch.
When something changes the ConditionsHandler will inform the Service/UI what happened via EventBus.


### Integration with Google Game Services (GGS)

The source code, and the currently published version, make use of the GGS and its APIs.

The GGS is an ancilliary component employed to engage users, it is not a primary component, and the distributed processing does not depend on it.

It is noteworthy that connection with GGS has a high degree of integration with Google Play. The publisher of this software must be able to access the Developer Console (to get the API Key, add/edit game achievements etc.). The publisher must also be able to sign the APK with the identity certificate associated with the app. Otherwise, GGS features will not function.

For example, the current application ID of this app is com.sonymobile.androidapp.grid. A third-party developer can build and run the software, but GGS features will not work because the developer won’t be able to sign the APK, because he does not have the Sony Mobile’s identity certificate. Either the developer:

- Changes the application ID, so he can add this new app ID in Developer Console, and associate it with an identity certificate that he controls (which also implies in a new API key), or
- The developer has the com.sonymobile.androidapp.grid ownership transferred from Sony Mobile to him.

------------------------------------------------------------------------------------------------------

## Server Setup folding-jobserver

### AWS Structure:
1. VPC (Virtual Private Cloud)

2. Route 53
  2. Control project dns

3. ELB (Elastic Load Balancing) / Auto scale
 	3. Elastic Load Balancing automatically distributes incoming application traffic across   multiple folding server instances (ec2).

  	3. The auto scale is configured to run a new instance when the processor load is greater than 80%

4. Private subnet
  4. All server instances run inside a private subnet with access only allowed by load balance on port 80

  4.  To update code on the ec2 instances we have a Bastion Host only this instance has permission to access the server instance inside private subnet.

  4. To run the Bastion Host we need access aws admin portal to start the instance and access the instance by SSH with key.pem. Inside the machine we have a file.pem to access the production instances.

5. S3
    5. On S3 we have two files
	  5. *“version.mapping.json”* with the mapping of each android version with the specific file *“FAHjobscript”*
	  5. *“FAHjobscript-[VERSION]”* File downloaded by client to start the stanford server communication

6. Dynamo DB
  6. Save the user id

![Folding Architecture](http://i.imgur.com/1dKfJES.png)

  ------------------------------------------------------------------------------------------------------

## Electron Setup folding-electron

1. HOW TO BUILD

  1. Make sure Node.js and NPM are installed
  1. Run 'npm install' from the project root dir
  1. To execute the app without generating the build pacakges run 'npm start'
  1. To build the packages run 'npm run dist' in the root folder


2. NOTES

  2. You must build the package from within the target operating system.
  2. The package will be saved into the 'dist' folder.
  2. To build the app from Ubuntu you'll need to install the 'rpm' package
  2. To build the app from Fedora you'll need to install the 'fedora-packager' package

# The Grid Compute Architecture

## Overview

This is an implementation of the GComp architecture. It's composed of three parts:

* The Project Management Server
* The Job Queue Server
* The Job Client

These three components form a powerful architecture for management of execution tasks that are to be handled in a distributed cloud environment.
Below follows an introduction to these components and an overview of how jobs are created and sent out to clients.

## Project Management Server

The project management server interacts with the job server to upload job requests to a queue.
In turn, the job server is contacted by clients who pop elements from the queue and execute the
request by downloading a script from the supplied server in the request. The script is passed via
stdin to a Node.js instance.

### Certificates

Four certificate sets are used in the project management server to communicate with the job server and the client:

* jobserver-cert.pem : Used as a CA to identify the job server. (To be replaced by a recognized CA)

* jobserver-pmca-cert.pem : A CA certificate used by clients to identify the project management
server.

* pmclient-cert.pem & pmclient-key.pem : A certificate (and key) sent to the job server which
is passed to a client and authorizes it to connect to the project management server and download scripts/data.

* pmserver-cert.pem & pmserver-key.pem : A certificate (and key) signed by the job server to identify  and authorize a project management server to upload jobs and connect to clients.

## Job Client

The job client is a simple job request implementation which starts an auxiliary Node.js process that  executes a job task on the local machine provided from stdin. The auxiliary Node.js process is locked  down so that forking and free filesystem access is not available from the script environment.

### Certificates

Three certificates are utilized from within the client:

* client-cert.pem & client-key.pem : An optional certificate with an encrypted key which is used to
validate the client. The passphrase for the encrypted key must be stored within a secure element and passed securely to the client. The secure element must be erased if the client or its environment has been compromised or is otherwise accessible to any external party. If this certificate is passed to the job server when requesting a job, the job server is ensured that the client environment cannot access or manipulate the job data nor its results.

* jobserver-cert.pem : A CA certificate which validates the job server. (To be replaced by a
recognized CA)

* jobserver-pmca-cert.pem : A CA certificate which identifies and authorizes project management  servers that the job client attempts to communicate with.


## Job Queue Server

The job queue server is contacted by project management servers to receive jobs, then in turn
by clients to execute jobs.


### Certificates

Three certificates are utilized from within the job server:

* jobserver-cert.pem & jobserver-key.pem : A certificate which identifies this server as a valid
job server that may send and receive jobs from project management servers and job clients.

* jobserver-clientca-cert.pem : A CA certificate for clients to the job server that verifies whether
a client is tamper-proof or not, allowing for secure jobs to be sent out to the clients and giving
the project management servers a way to get reliable results.

* jobserver-pmca-cert.pem : A CA certificate which identifies and authorizes project management
servers that wish to upload jobs to the job server.


### Job Format

Job scripts are NodeJS scripts that will be downloaded from the project management server on an HTTPS URI specified by the job request.

### Job Request


To start a job, a JSON object is sent via HTTPS POST to the server as plaintext:
```
name: "name": REQUIRED
desc: "desc": REQUIRED
server: "server-address": REQUIRED
port: server-port: REQUIRED
path: "/script-path": REQUIRED
client_key: pmclient-key.pem: REQUIRED
client_cert: pmclient-cert.pem : REQUIRED
job_count: number of jobs to start : REQUIRED
current_job: [if job_count==0, this is a list of jobs to start]: REQUIRED
secure: Whether we require that a client is tamper proof or not : REQUIRED
```


pmserver-cert.pem and pmserver-key.pem are used in the request to authorize the connection. Similarly,  jobserver-cert.pem is used as a CA to ensure the job server is secure.

The server returns a timeout string specifying in seconds when it believes the jobs will have had time  to start. If this timeout is passed, the job is to be retried.


### Job Execution

When a job starts, it will send a HTTPS GET request to the supplied server, port, and path, using the client key and cert to authorize itself. jobserver-pmca-cert.pem is used as a CA to ensure the project  management server is secure. The request must return a valid Node.js script that is then passed over  stdin to a new Node.js instance.

After startup, a new timeout should be initiated on the project management server. This timeout
indicates a time limit for the job in case it is stopped on the client without being given a chance to notify the project management server.

When a job ends, it may if necessary contact the project management server to supply any results.
If a job is stopped by the client, it may be given a chance to finish the current job. If so, it sends
a SIGTERM to the job and gives it a [TBD] second timeout to finish any current task. This SIGTERM may be trapped in the job to upload any intermediate results to the project management server before ending the task. If the timeout passes, a SIGKILL is sent to the process to force the current task to end.


# Place your keys

## Android app
| File                                                    | Variable                                                                           | Description                                                           |
| ------------------------------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| `app/src/main/Manifest.xml`                             | `android:authorities in provider com.facebook.FacebookContentProvider`             | Content provider used to share on Facebook                            |
| `app/src/main/res/values/strings_nontranslatable.xml`   | `fb_app_id`                                                                        | Facebook app id                                                       |
| `app/src/main/res/values/ids.xml`                       | `app_id`                                                                           | Google games app id                                                   |
| `app/src/main/res/values/ids.xml`                       | `achievement_share_on_facebook`                                                    | 'Share on facebook' achievement id                                    |
| `app/src/main/res/values/ids.xml`                       | `achievement_start_folding`                                                        | 'Start folding' achievement id                                        |
| `app/src/main/res/values/ids.xml`                       | `achievement_aminoacid`                                                            | 'Aminoacid' achievement id                                            |
| `app/src/main/res/values/ids.xml`                       | `achievement_peptide`                                                              | 'Peptide' achievement id                                              |
| `app/src/main/res/values/ids.xml`                       | `achievement_polypeptide`                                                          | 'Polypeptide' achievement id                                          |
| `app/src/main/res/values/ids.xml`                       | `achievement_protein`                                                              | 'Protein' achievement id                                              |
| `app/src/main/res/values/ids.xml`                       | `achievement_enzyme`                                                               | 'Enzyme' achievement id                                               |
| `app/src/main/res/values/ids.xml`                       | `achievement_open_the_breast_cancer_details`                                       | 'Open breast cancer details' achievement id                           |
| `app/src/main/res/values/ids.xml`                       | `achievement_6_hours_straight_breast_cancer`                                       | 'Contribute 6 straight for breast cancer' achievement id              |
| `app/src/main/res/values/ids.xml`                       | `achievement_12_hours_straight_breast_cancer`                                      | 'Contribute 12 straight for breast cancer' achievement id             |
| `app/src/main/res/values/ids.xml`                       | `achievement_18_hours_straight_breast_cancer`                                      | 'Contribute 18 straight for breast cancer' achievement id             |
| `app/src/main/res/values/ids.xml`                       | `achievement_24_hours_straight_breast_cancer`                                      | 'Contribute 24 straight for breast cancer' achievement id             |
| `app/src/main/res/values/ids.xml`                       | `leaderboard_leaderboards`                                                         | 'All contributors' leaderboard id                                     |
| `app/src/main/assets/environment.js`                    | `JOB_SERVER_ADDRESS`                                                               | Job server address                                                    |
| `androidapp/gridcomputing/gamification/Scores.java`     | `ID_BREAST_CANCER_RESEARCH`                                                        | 'Open breast cancer details' achievement id                           |
| `androidapp/gridcomputing/utils/FacebookUtils.java`     | `POST_OBJECT_PATH`                                                                 | Facebook open graph type used to share the contributed time           |
| `androidapp/gridcomputing/utils/FacebookUtils.java`     | `GRAPH_OBJECT`                                                                     | Facebook open graph object                                            |
| `androidapp/gridcomputing/utils/FacebookUtils.java`     | `POST_ACTION_PATH`                                                                 | Facebook open graph path                                              |
| `androidapp/gridcomputing/utils/FacebookUtils.java`     | `POST_ACTION_IMAGE_URL`                                                            | Image url that will be used when posting to facebook                  |


## Electron app
| File                                                    | Variable                                                                           | Description                                                           |
| ------------------------------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| `app/src/social/facebook-share.js`                      | `APP_ID`                                                                           | Facebook app id                                                       |
| `app/src/social/facebook-share.js`                      | `CLIENT_SECRET`                                                                    | Facebook client secret                                                |
| `app/src/social/facebook-share.js`                      | `REDIRECT_SUCCESS`                                                                 | Facebook OAUTH redirect url                                           |
| `app/src/social/facebook-share.js`                      | `FB_OG_TYPE`                                                                       | Facebook open graph type used to share the contributed time           |
| `app/src/social/facebook-share.js`                      | `FB_OG_OBJECT`                                                                     | Facebook open graph object                                            |
| `app/src/social/facebook-share.js`                      | `FB_OG_PATH`                                                                       | Facebook open graph path                                              |
| `app/src/social/facebook-share.js`                      | `FB_OG_URL`                                                                        | Url that will be shared when posting to facebook                      |
| `app/src/social/facebook-share.js`                      | `FB_OG_IMAGE`                                                                      | Image url that will be used when posting to facebook                  |
| `app/src/social/facebook-share.js`                      | `FB_OG_TITLE`                                                                      | Title used to post on facebook                                        |
| `app/src/social/facebook-share.js`                      | `FB_SHARE_URL`                                                                     | Url to be included in the message body when posting to facebook       |
| `app/src/gamification/google-apis.js`                   | `transporters.prototype.USER_AGENT`                                                | Google games app id                                                   |
| `app/src/gamification/google-apis.js`                   | `CLIENT_SECRET`                                                                    | Google client secret                                                  |
| `app/src/gamification/google-apis.js`                   | `REDIRECT_URL`                                                                     | Google OAUTH redirect url when login                                  |
| `app/src/utils/global-config.js`                        | `gameIds.CLIENT_ID`                                                                | 'Share on facebook' achievement id                                    |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_SHARE_ON_FACEBOOK`                                                    | 'Share on facebook' achievement id                                    |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_START_FOLDING`                                                        | 'Start folding' achievement id                                        |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_AMINOACID`                                                            | 'Aminoacid' achievement id                                            |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_PEPTIDE`                                                              | 'Peptide' achievement id                                              |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_POLYPEPTIDE`                                                          | 'Polypeptide' achievement id                                          |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_PROTEIN`                                                              | 'Protein' achievement id                                              |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_ENZYME`                                                               | 'Enzyme' achievement id                                               |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_OPEN_THE_BREAST_CANCER_DETAILS`                                       | 'Open breast cancer details' achievement id                           |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_6_HOURS_STRAIGHT_BREAST_CANCER`                                       | 'Contribute 6 straight for breast cancer' achievement id              |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_12_HOURS_STRAIGHT_BREAST_CANCER`                                      | 'Contribute 12 straight for breast cancer' achievement id             |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_18_HOURS_STRAIGHT_BREAST_CANCER`                                      | 'Contribute 18 straight for breast cancer' achievement id             |
| `app/src/utils/global-config.js`                        | `gameIds.ACH_24_HOURS_STRAIGHT_BREAST_CANCER`                                      | 'Contribute 24 straight for breast cancer' achievement id             |
| `app/src/utils/global-config.js`                        | `gameIds.LEAD_LEADERBOARDS`                                                        | 'All contributors' leaderboard id                                     |
| `app/src/main/assets/environment.js`                    | `JOB_SERVER_ADDRESS`                                                               | Job server address                                                    |




## Jobserver
| File                                                    | Variable                                                                           | Description                                                                             |
| ------------------------------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| `scripts/monitor.js`                                    | `logglyConfig`                                                                     | Loggly JSON config file path                                                            |
| `src/server.js`                                         | `defaultJobDesc`                                                                   | Path to a JSON file containing the config of a S3 bucket where the job files are stored |
| `src/aws-credentials-analytics.json`                    | `accessKeyId`                                                                      | AWS analytics access key id                                                             |
| `src/aws-credentials-analytics.json`                    | `secretAccessKey`                                                                  | AWS analytics secret access key                                                         |
| `src/aws-credentials-analytics.json`                    | `region`                                                                           | AWS analytics region                                                                    |
