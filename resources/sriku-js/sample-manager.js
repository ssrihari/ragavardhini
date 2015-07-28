// Copyright (c) 2011 Srikumar K. S. 
// All rights reserved
// Licensed under: New BSD License
//
// The SampleManager provides cached storage of audio
// sample files downloaded from a relative URL location
// called the "sample set url" (SSURL).
//
// SSURL+"/mappings.js" is expected to be a JSON file
// with keys = names of samples (arbitrary) and values 
// being objects with at least two fields - "version"
// and "url". The "version" gives a monotonically increasing 
// version number for the sample and the "url" field
// gives the file name of the sample relative to SSURL.
//
// SampleManager will then cache these samples using
// a local file system organized as follows -
// 1. All sample sets are stored under the "/samples/"
//    folder.
// 2. Each sample set is stored within its own folder.
//    The name of the folder is the "name" parameter
//    to the exported loadSampleSet function.
// 3. The sample files downloaded from the SSURL
//    location are saved to the sample set folder
//    with the same file name.
// 4. The latest mappings.js file is stored in localStorage
//    under the key "com.nishabdam.SampleManager.mappings.SSNAME"
//    as a JSON string.
//
// Every time the app is used, the mappings file is downloaded
// and the version numbers in it are compared with the stored
// version numbers in localStorage. If any version number
// has been bumped up, that file will be downloaded again.
// Otherwise the sample will be loaded from the file system
// instead of the net, thus saving bandwidth and time.
//
// Exported functions -
//
// SampleManager.init(requiredStorage_MB, delegate, options)
//
//      This needs to be called first before any sample set
//      can be loaded. Since we need local storage space to
//      keep samples around, we might need to get the browser to
//      ask the user to permit the app to do that. Hence this
//      additional first step. This can be called more than once
//      if, say, the storage requirement changes.
//
//      The delegate is an object with optionally defined
//      methods that get invoked at the appropriate times.
//
//      didInitialize: function (SampleManager) {...}
//      Gets called once initialization is complete. The one 
//      argument is the sample manager module itself. This
//      delegate is mandatory, or you won't know when initialization
//      has finished.
//
//      initFailed: function (error) {...}
//      Gets called if there was some error in initializing the
//      file system stuff (usually). Optional. If you don't supply
//      this, the didInitialize: function won't get called and 
//      the module will just stay silent about the error.
//
//      The "options" argument, if given, is expected to be an
//      object which provides pre-initialized system resources
//      if they're already available. If you don't give the
//      options argument, then a new audio context will be
//      created and the local file system initialized. If you
//      pass an empty object to options, these fields will be
//      filled in with the created system resource objects.
//
//          options.audioContext :: AudioContext
//              If you already have an initialized webkitAudioContext
//              that can be used for decoding and playing audio,
//              you can pass it in here.
//
//          options.fileSystem :: FileSystem
//              If you already have initialized a local file system
//              with the necessary quota and no new initialization
//              is necessary, you can pass the file system object
//              in this field.
//
// SampleManager.loadSampleSet(name, root_url, delegate)
//
//      "name" is the local name of the sample set you want to
//      load. This name is used to uniquely identify your
//      sample set on the client side. It has no bearing on
//      the server side.
//
//      "root_url" is the URL under which all the samples
//      belonging to the set are located. The location should
//      also have a "mappings.js" JSON file that describes
//      what samples are present, their versions and names.
//      (see above).
//
//      "delegate" is an object with functions that get called
//      on various events. All functions except didFinishLoadingSampleSet
//      are optional. If you don't specify the didFinishLoadingSampleSet,
//      then you'll never know when the sample set finished loading and
//      you'll also not have a reference to the sample set to work with!
//      So make sure you specify that one. The "name" argument to the
//      delegate calls can be used to distinguish between multiple sample
//      sets that may be loading simultaneously.
//      
//          didFetchMappings: function (name, mappings) {...}
//          Called once mappings.js has been fetched for the sample set,
//          before any sample loading begins.
//
//          didFinishLoadingSampleSet: function (name, sampleSet) {...}
//          Gets called once all the samples in the set are loaded
//          and decoded. The passed argument is an object whose keys
//          are the names of the samples (as specified in the mappings.js
//          file) and whose corresponding values are AudioBuffer objects.
//          This is the *LAST* call that you'll get upon success of
//          each loadSampleSet() invocation.
//
//          onError: function (e) {...}
//          Gets called whenever an error occurs.
//
//          beganDownloadingSample: function (name, key, url) {...}
//          Called when download of a particular sample starts.
//          The "key" is the logical name of the sample and the "url"
//          is its file name. Intended for UI purposes.
//
//          didDownloadSample: function (name, key, url) {...}
//          Called once a sample finished downloading. For UI purposes.
//
//          didLoadSample: function (name, key) {...}
//          Called once a sample data has been loaded into memory for decoding. 
//          For UI purposes.
//
//          didDecodeSample: function (name, key, buffer) {...}
//          Called once the specified downloaded sample has finished decoding.
//
//          didExpireSampleFile: function (name, path) {...}
//          Called when a file originally cached on the local system was
//          deleted on the server and so is no longer part of the sample set.
//          This file is removed from the local file system to conserve
//          storage.
//
// SampleManager.play(sampleSetName, key, gain, rate, when, offset, duration)
//      
//      Simple function to play a single sample with the given settings.
//      The sample set with the given name is looked up and the given key is 
//      looked up within that set. This buffer is triggered with the rest
//      of the given settings. 
//
//      "gain" = 0.0-1.0 range
//      "rate" = factor. 1.0 means normal rate, 0.5 means one octave lower, 
//               2.0 means one octave higher.
//      "when" = start time of note relative to now.
//      "offset" = time offset within sample to start. Passing 0 starts at beginning.
//      "duration" = (optional) the amount of audio to play. If left out, the
//                   whole sample will be played.
//
// SampleManager.requireWebAudioAPI()
//
//      A simple orthogonal function that ensures the presence of the web audio
//      API or else throws an exception.
package('com.nishabdam.audio.sample-manager', function () {

var exports = this;
var initsCompleted = {};
var context, fs;
var global = window;

function mkpipe() {
    var storage = [];
    var readIx = 0;
    var readNotification = undefined;
    var closed = false;

    var pipe = {};


    // A pipe read, if succeeds, immediately returns the
    // value. Otherwise it will put the given notify function
    // in a queue and call it with the pipe as the argument
    // when a value is available. In that case, the return
    // value will be undefined. A pipe read will always
    // succeed if the pipe isn't empty. If the pipe is empty
    // and is closed, then the notify function won't be scheduled
    // for a call and the return value will be undefined, but then
    // the pipe.isClosed() call will return true.
    //
    // Supports only one reader.
    pipe.read = function (notify) {
        var value;

        if (readIx < storage.length) {
            // There is material to be read.
            value = storage[readIx];
            ++readIx;

            // Do some simple memory management before
            // calling the notification.
            if (readIx > storage.length / 2) {
                storage.splice(0, readIx);
                readIx = 0;
            }

            return value;
        } else {
            // Clear the storage.
            storage = [];
            readIx = 0;
        }

        if (!closed) {
            readNotification = notify;
        }

        return undefined;
    };

    // Can always write to a pipe, unless it is
    // closed. If closed, then write call will
    // raise an exception.
    pipe.write = function (value) {
        var notify;
        if (closed) {
            throw new Error('Writing to closed pipe!');
        }

        storage.push(value);

        if (readNotification) {
            notify = readNotification;
            readNotification = undefined;
            notify(pipe);
        }
    };

    // Closes the pipe so that no more writing can occur.
    // A reader waiting on a pipe will get triggered. This
    // means the reader can check the closed status of
    // the pipe if read failed immediately and take necessary
    // actions.
    pipe.close = function () {
        closed = true;
        if (readNotification) {
            var notify = readNotification;
            readNotification = undefined;
            notify(pipe);
        }
    };

    // Use to check whether the pipe is closed.
    pipe.isClosed = function () {
        return closed;
    };

    return pipe;
}


var sampleSetCollection = {};
// This is a map from sample *set* names as keys to an object with
// sample names as keys and AudioBuffer as value. i.e. -
//    sampleSetCollection[sampleSetName][sampleKey] = AudioBuffer

function globalCheck() {
    if (initsCompleted.globalCheck) { return; }

    // Check requirements
    if (!global.localStorage 
            || !(global.requestFileSystem || global.webkitRequestFileSystem)
            || !(global.navigator.persistentStorage || global.navigator.webkitPersistentStorage)) {
                alert("Please use the latest version of a modern browser like Chrome.");
                throw false;
            }

    if (!global.AudioContext && !global.webkitAudioContext) {
        alert("Needs latest version of Chrome for the Web Audio API.");
        throw false;
    }

    // Compatibility modifications.
    global.requestFileSystem = global.requestFileSystem || global.webkitRequestFileSystem;
    global.AudioContext = global.AudioContext || global.webkitAudioContext;
    global.navigator.persistentStorage = global.navigator.persistentStorage || global.navigator.webkitPersistentStorage;
    global.URL = global.URL || global.webkitURL;

    initsCompleted.globalCheck = true;
}

function initFS(_fs, delegate) {
    if (fs) { return; }
    fs = _fs;
    console.log("File system request succeeded!");
    if (delegate && delegate.didInitialize) {
        delegate.didInitialize(exports);
    }
}

function failFS(e, delegate) {
    alert("File system request failed!");
    if (delegate && delegate.initFailed) {
        delegate.initFailed(e);
    }
    throw false;
}

// Initialize audio and storage. Can be called
// again if the storage requirement changes.
function init(storageQuota_MB, delegate, options) {
    try {
        globalCheck();
        if (options && options.audioContext) {
            context = options.audioContext;
        }
        if (!context) {
            context = new global.AudioContext();
        }
        if (options) {
            options.audioContext = context;
        }
    } catch (e) {
        if (delegate && delegate.initFailed) {
            delegate.initFailed(e);
        }
        return;
    }

    if (options && options.fileSystem) {
        fs = null;
        initFS(options.fileSystem, delegate);
    } else {
        fs = null;

        var makeFSRequest = function () {
            if (fs) { return; };
            global.requestFileSystem(global.PERSISTENT, storageQuota_MB * 1024 * 1024, 
                    function (_fs) {
                        if (options) {
                            options.fileSystem = _fs;
                        }
                        initFS(_fs, delegate);
                    },
                    function (e) {
                        failFS(e, delegate);
                    });
        };

        var makeQuotaRequest = function () {
            global.navigator.persistentStorage.requestQuota(storageQuota_MB * 1024 * 1024, 
                    function (grantedBytes) {
                        setTimeout(makeFSRequest, 0);
                    },
                    function (e) {
                        failFS(e, delegate);
                    });
        };

        global.navigator.persistentStorage.queryUsageAndQuota(
                function (usage, quota) {
                    if (quota < storageQuota_MB * 1024 * 1024) {
                        setTimeout(makeQuotaRequest, 0);
                    } else {
                        setTimeout(makeFSRequest, 0);
                    }
                },
                function (e) {
                    setTimeout(makeQuotaRequest, 0);
                });

//       setTimeout(makeQuotaRequest, 0);
//        setTimeout(makeFSRequest, 0);
    }
}

var mappingKey = "com.nishabdam.SampleManager.mapping";
var samplesPath = "samples";

// delegate.onError(e)
// delegate.didFinishLoadingSampleSet(name, sampleTable)  // The FINAL call
// delegate.beganDownloadingSample(name, key, url)
// delegate.didDownloadSample(name, key, url, remainingLoadTasks)
// delegate.didDecodeSample(name, key, buffer)
// delegate.didExpireSampleFile(name, path)
function loadSampleSet(name, root_url, delegate) {

    if (name in sampleSetCollection) {
        delegate.didFinishLoadingSampleSet(name, sampleSetCollection[name]);
        return;
    }

    if (!fs || !context) {
        if (delegate && delegate.onError) {
            delegate.onError(new Error("SampleManager not initialized"));
        } else {
            throw "SampleManager not initialized";
        }
    }

    var mappingsURL = root_url + '/mappings.js';
    var request = new global.XMLHttpRequest();
    request.open('GET', mappingsURL, true);
    request.onload = function () {
        var mappings = global.JSON.parse(request.responseText);

        if (delegate && delegate.didFetchMappings) {
            delegate.didFetchMappings(name, mappings);
        }

        downloadSamples(name, root_url, mappings, delegate);
    };
    request.onerror = function () {
        if (delegate && delegate.onError) {
            delegate.onError();
        }
    };
    request.send();
}

function downloadSamples(name, root_url, mappings, delegate) {
    // Compare with the earlier mapping and load only
    // those samples that have changed.
    var loadTasks = samplesToDownload(mappings);
    var loadedSamples = {};
    var downloadFinished = false;

    loadTasks.decode = mkpipe();

    function reportError(e) {
        if (delegate && delegate.onError) {
            delegate.onError(e);
        }
    }

    function removeDeadSamples(pipe) {
        var fileName = pipe.read(removeDeadSamples);
        if (fileName === undefined) {
            if (pipe.isClosed()) {
                // Nothing to do.
            }
            return;
        }

        fs.root.getFile('samples/' + name + '/' + fileName, {create: false},
                function (fileEntry) {
                    fileEntry.remove(function () {
                        if (delegate && delegate.didExpireSampleFile) {
                            delegate.didExpireSampleFile(name, fileName);
                        }
                        removeDeadSamples(pipe);
                    },
                    reportError);
                },
                function () { 
                    console.error("Error deleting file [" + fileName + "]"); 
                });
    }

    function downloadSamples(pipe) {
        var key = pipe.read(downloadSamples);
        if (key === undefined) {
            if (pipe.isClosed()) {
                loadTasks.load.close();
            }
            return;
        }

        var request = new global.XMLHttpRequest();
        var url = root_url + '/' + mappings[key].url;
        if (delegate && delegate.beganDownloadingSample) {
            delegate.beganDownloadingSample(name, key, url);
        }
        request.open('GET', url, true);
        request.responseType = 'arraybuffer';
        request.onload = function () {
            if (delegate && delegate.didDownloadSample) {
                delegate.didDownloadSample(name, key, url, loadTasks.download.length);
            }

            console.log("Downloaded " + key);
            saveFile(request.response, key, mappings[key].url, function () {
                console.log("Saved " + key);
                loadTasks.load.write(key);
                downloadSamples(pipe);
            });
        };
        request.onerror = function () {
            reportError(key);
            downloadSamples(pipe);
        };
        request.send();
    }

    function loadFiles(pipe) {
        var key = pipe.read(loadFiles);
        if (key === undefined) {
            if (pipe.isClosed()) {
                loadTasks.decode.close();
            }
            return;
        }

        loadFile(key, 
                function (data) {
                    console.log("Loaded " + key);
                    if (delegate && delegate.didLoadSample) {
                        delegate.didLoadSample(name, key);
                    }

                    loadTasks.decode.write({key: key, data: data});
                    loadFiles(pipe);
                }, 
                function () {
                    reportError(key);
                    loadFiles(pipe);
                });
    }

    function decodeFiles(pipe) {
        var key = pipe.read(decodeFiles);
        if (key === undefined) {
            if (pipe.isClosed()) {
                // We're done!
                sampleSetCollection[name] = loadedSamples;
                if (delegate && delegate.didFinishLoadingSampleSet) {
                    delegate.didFinishLoadingSampleSet(name, loadedSamples);
                }
            }
            return;
        }

        context.decodeAudioData(key.data,
                function (buffer) {
                    loadedSamples[key.key] = buffer;
                    console.log("Decoded " + key.key);
                    if (delegate && delegate.didDecodeSample) {
                        delegate.didDecodeSample(name, key.key, buffer);
                    }
                    decodeFiles(pipe);
                },
                function () {
                    reportError(key);
                    decodeFiles(pipe);
                });

    }

    function download() {
        removeDeadSamples(loadTasks.remove);
        downloadSamples(loadTasks.download);
        loadFiles(loadTasks.load);
        decodeFiles(loadTasks.decode);
        loadTasks.download.close(); // No more stuff to download.
    }

    function saveFile(data, key, url, onComplete) {

        fs.root.getDirectory("samples", {create: true},
                function (samplesDir) {
                    samplesDir.getDirectory(name, {create: true},
                        function (dirEntry) {
                            dirEntry.getFile(url, {create: true}, 
                                function (f) {
                                    f.createWriter(
                                        function (writer) {
                                            writer.onwriteend = onComplete;
                                            writer.onerror = reportError;

                                            //var bb = new global.BlobBuilder();
                                            //bb.append(data);
                                            writer.write(new Blob([data], {type: 'application/octet-stream'}));//bb.getBlob());
                                        },
                                        reportError);
                                },
                                reportError);
                        },
                        reportError);
                },
                reportError);
    }

    function loadFile(key, onLoad, onError) {
        fs.root.getDirectory("samples", {create: true},
                function (samplesDir) {
                    samplesDir.getDirectory(name, {create: true},
                        function (dirEntry) {
                            dirEntry.getFile(mappings[key].url, {create: false},
                                function (fileEntry) {
                                    fileEntry.file(
                                        function (f) {
                                            var reader = new global.FileReader();

                                            reader.onloadend = function (e) {
                                                onLoad(reader.result);
                                            };

                                            reader.onerror = onError;

                                            reader.readAsArrayBuffer(f);
                                        },
                                        onError);
                                },
                                onError);
                        },
                        onError);
                },
                onError);
    }

    function samplesToDownload(mappings) {
        var prevMappingStr = global.localStorage[mappingKey + '.' + name];
        var prevMapping, key;
        var result = {download: mkpipe(), load: mkpipe(), remove: mkpipe()};

        if (prevMappingStr) {
            prevMapping = global.JSON.parse(prevMappingStr);
            for (key in mappings) {
                if (key in prevMapping) {
                    if (prevMapping[key].version < mappings[key].version) {
                        result.download.write(key);
                    } else {
                        result.load.write(key);
                    }
                } else {
                    result.download.write(key);
                }
            }
        } else {
            // We need to download the whole sample set.
            for (key in mappings) {
                result.download.write(key);
            }
        }

        for (key in prevMapping) {
            if (!(key in mappings)) {
                // A sample has been removed. Note down that
                // it has to be deleted from the file system.
                result.remove.write(prevMapping[key].url);
            }
        }

        global.localStorage[mappingKey + '.' + name] = JSON.stringify(mappings);
        return result;
    }

    download();
}

// A simple note player. Useful for testing, but not much
// beyond that. The duration parameter, if left out, will
// be automatically calculated from the sample. 
function play(sampleSetName, sampleKey, gain, rate, when, offset, duration) {
    var source = context.createBufferSource();
    source.gain.value = gain;
    source.playbackRate.value = rate;
    source.buffer = sampleSetCollection[sampleSetName][sampleKey];
    source.connect(context.destination);

    source.noteGrainOn(
            when, 
            Math.min(Math.max(0.0, offset ? offset : 0.0), source.buffer.duration - 0.001), 
            Math.min(Math.max(0.0, duration === undefined ? source.buffer.duration : duration), source.buffer.duration - 0.001));
}

exports.init = init;
exports.loadSampleSet = loadSampleSet;
exports.play = play;
exports.requireWebAudioAPI = function () {
    if (!(global.AudioContext || global.webkitAudioContext)) {
        alert("SampleManager needs the Web Audio API. Use the latest version of Chrome.");
        throw false;
    } else {
        global.AudioContext = global.AudioContext || global.webkitAudioContext;
    }
};

return exports;
});
