package.config({
    'org.anclab.steller.scheduler': {path: 'scheduler.js'}
});

package('srikumarks.phd.vina', ['com.nishabdam.audio.sample-manager', 'org.anclab.steller.scheduler'],
        function (SampleManager, Scheduler) {

var exports = this;

var Vina = exports;

SampleManager.requireWebAudioAPI();

function startTanpura(audioContext, tanpura, tuning_st, destination) {
    var sched = new Scheduler(audioContext);
    sched.running = true;
    var factor = Math.pow(2, tuning_st/12);
    var t = sched.loop(sched.track(sched.fire(function () {
        var source = audioContext.createBufferSource();
        var sourceGain = audioContext.createGain();
        sourceGain.gain.value = 0.25;
        sourceGain.connect(destination);
        source.buffer = tanpura;
        source.playbackRate.value = factor;
        source.connect(sourceGain);
        source.start(0);
    }), sched.delay(factor * 6)));
    sched.play(t);
}

exports.init = function (delegate) {
    var options = {};

    // Initialize the sample manager with 10MB of storage space
    // for the vina samples.
    SampleManager.init(10, {
        didInitialize: function (sm) {
            console.log("SampleManager initialized.");
            loadVinaSamples(sm);
        },

        initFailed: function (error) {
        }
    }, options);

    function loadVinaSamples(sm) {
        sm.loadSampleSet("vina", "vina", {
            didFinishLoadingSampleSet: function (name, sampleSet) {
                console.log("Vina samples loaded.");
                setupExports(sm, sampleSet, options.audioContext);
                delegate.didInitializeVina(exports, sm, sampleSet, options.audioContext);
            },

            onError: function (e) {
                console.log(e);
                delegate.onError(e);
            }
        });
    }
}


function semitones(st) {
    return Math.pow(2.0, st / 12);
}

function setupExports(sm, sampleSet, audioContext) {
    var pitchseq = ['p7', 'p8', 'p9', 'p10', 'p11', 'p12', 'p13', 'p14', 'p15', 'p16', 'p17', 'p18', 'p19', 'p20', 'p21', 'p22', 'p23', 'p24'];
    var pitchseq_zero = 5;
    var kTimeEpsilon = 1/audioContext.sampleRate;

    exports.audioContext = audioContext;
    exports.__defineGetter__('now_secs', function () {
        return audioContext.currentTime;
    });

    var destination = audioContext.destination;

    function prepareDestination() {
        var main = audioContext.createGain();
        var dry = audioContext.createGain();
        var wet = audioContext.createGain();
        var conv = audioContext.createConvolver();
        conv.buffer = sampleSet['matrix-reverb'];
        dry.gain.value = 1;
        wet.gain.value = 1;
        main.gain.value = 0.25;
        main.connect(dry);
        main.connect(conv);
        conv.connect(wet);
        dry.connect(audioContext.destination);
        wet.connect(audioContext.destination);
        destination = main;
    }

    prepareDestination();

    function Instrument(tuning_st, gain) {
        startTanpura(audioContext, sampleSet['tanpura'], tuning_st, destination);
        var dt = 1/100;

        gain = gain || 0.5;

        // The pitchCurve is expected to be a function of time where
        // the time interval runs from 0 to duration_secs. The return
        // value is expected to be the pitch in semitones where 0 stands
        // for "C".
        //
        // If duration_secs and stoppage are omitted, they're expected to
        // be object members of pitchCurve and will be retrieved as
        // pitchCurve.duration_secs and pitchCurve.stoppage.
        this.pluck = function (pitchCurve, time_secs, duration_secs, stoppage) {
            var sample, source, stopBegin, stopEnd, t;

            if (!pitchCurve) {
                return undefined;
            }

            duration_secs = (duration_secs === undefined ? pitchCurve.duration_secs : duration_secs);
            stoppage    = (stoppage === undefined ? pitchCurve.stoppage : stoppage);
            sample      = nearestSample(pitchCurve.base_pitch + tuning_st);
            source      = audioContext.createBufferSource();
            stopBegin   = duration_secs - (stoppage === false ? 0.0 : stoppage);
            stopEnd     = Math.min(duration_secs, stopBegin + 0.05);

            source.buffer = sample.buffer;

            var sourceGain = audioContext.createGain();
            sourceGain.gain.value = gain;
            sourceGain.connect(destination);
            source.connect(sourceGain);

            // The linear and exponential ramps don't work.
            // The setValues... method also doesn't work.
            for (t = 0; t < duration_secs; t += dt) {
                source.playbackRate.setValueAtTime(semitones(pitchCurve(t) + sample.pitch_rel - pitchCurve.base_pitch), time_secs + t);
            }

            // Compute the gain envelope. This implements a crude stopping technique.
            if (stopEnd > stopBegin) {
                for (t = stopBegin; t <= stopEnd; t += dt) {
                    t = Math.min(stopEnd, t);
                    sourceGain.gain.linearRampToValueAtTime(gain * (stopEnd - t) / (stopEnd - stopBegin), time_secs + t);
                }
                sourceGain.gain.setValueAtTime(0, time_secs + duration_secs);
            } else if (stoppage !== false) {
                // If stoppage is false, it means the tone is a sustained tone.
                sourceGain.gain.setValueAtTime(0, time_secs + duration_secs);
            }

            return {
                play: function () { source.start(time_secs); },
                time_secs: time_secs,
                end_secs: time_secs + duration_secs,
                duration_secs: duration_secs,
                pitch_curve: pitchCurve
            };
        };

        // plucks[i][j] = note j of pluck i
        // note.soasr gives the {stage:oscil:} form of the note.
        // *Interp = linear|sine|skew_sine|fret_slide|glide
        this.playPhrase = function (plucks, stageInterp, danceInterp) {
            var t = audioContext.currentTime;
            var instr = this;
            var pluckObjs = plucks.map(function (p) {
                var totalDuration_secs = 0;
                var notes = p.map(function (n) {
                    totalDuration_secs += n.duration_secs;
                    return exports.soasrn(n.duration_secs, n.soasr);
                });
                var pluckObj = instr.pluck(Vina.soasr(totalDuration_secs,
                                        Vina.concat_soasrns(notes),
                                        p[0].stoppage_secs || 0.01,  // The first note of a pluck contains stoppage information.
                                        danceInterp,
                                        stageInterp), t);
                t += pluckObj.duration_secs;
                return pluckObj;
            });
            pluckObjs.forEach(function (p) { p.play(); });
            return pluckObjs;
        };
    }

    function nearestSample(pitch_st) {
        var fret = Math.min(12, Math.max(-pitchseq_zero, Math.floor(pitch_st + 0.5)));
        var offset = pitch_st - fret;
        return {fret: fret, pitch_rel: offset, buffer: sampleSet[pitchseq[Math.floor(0.5 + pitchseq_zero + fret)]]};
    }


    exports.create = function (tuning_st, gain) {
        return new Instrument(tuning_st, gain);
    };


    // A variety of interpolation methods.
    // The functions all have signature
    // function (t, skewPoint, p1, p2) -> frac
    // where the result value is expected to be the fraction
    // of interpolation between the two given pitches.
    // Functions surely need t, but may ignore other
    // arguments.
    var interpolation = (function () {
        // A skewed interpolation makes use of the same interpolation
        // curve (interp) for the portions before and after the skewPoint,
        // (range 0-1) but it makes sure that at the skew point the
        // time derivative is continuous.
        function skew(t, skewPoint, interp) {
            if (t <= 0.0) {
                return 0.0;
            } else if (t >= 1.0) {
                return 1.0;
            } else if (t < skewPoint) {
                return 2.0 * skewPoint * interp(0.5 * t / skewPoint);
            } else {
                return 1.0 - 2.0 * (1.0 - skewPoint) * interp(0.5 * (1.0 - t) / (1.0 - skewPoint));
            }
        }

        // This interpolation behaves as though you were to slide
        // on a stringed instrument. The movement is linear in
        // space - i.e. linear in the vibrating length of the string.
        // which is inversely related to the vibrating frequency.
        //
        // The return value is the actual interpolated pitch.
        function stringPosInterp(t, skewPoint, p1, p2) {
            var l1, l2, frac, p;
            if (p2 === p1) {
                return p1;
            }

            l1 = Math.pow(2, - p1 / 12);
            l2 = Math.pow(2, - p2 / 12);
            frac = sine(t);//skewSine(t, skewPoint);
            p = (-12) * Math.log(l1 + (l2 - l1) * frac) / Math.log(2);
            return p;
        }

        function linear(t) {
            return t;
        }

        // One period of a sine.
        function sine(t) {
            return 0.5 * (1.0 + Math.sin(Math.PI * (t - 0.5)));
        }

        // Sine interpolation taking a "skewPoint" into account.
        // The skew point is a value in the [0,1] range (which is
        // the range of t) at which the derivative is continuous,
        // but the portion before and after the skew point are
        // 1/4 period sine curves.
        function skewSine(t, skewPoint) {
            return skew(t, skewPoint, sine);
        }

        // Interpolation as though you were sliding on a fretted
        // string instrument (like veena, guitar). The movement is
        // a bit too discrete to be sonically accurate, but is
        // reasonable.
        function fretSlide(t, skewPoint, p1, p2) {
            if (p2 === p1) {
                return 0;
            }

            var p = stringPosInterp(t, skewPoint, p1, p2);
            p = Math.min(Math.max(p1, p2), Math.ceil(p - 0.1));
            return (p - p1) / (p2 - p1);
        }

        // Smooth glide on a stringed instrument.
        function glide(t, skewPoint, p1, p2) {
            if (p2 === p1) {
                return 0;
            }

            var p = stringPosInterp(t, skewPoint, p1, p2);
            return (p - p1) / (p2 - p1);
        }

        // Collect everything into an object.
        return {
            linear: linear,
            sine: sine,
            skew_sine: skewSine,
            fret_slide: fretSlide,
            glide: glide
        };
    }());

    // pasr is an arrary of {pitch:, attack:, sustain:, release:} objects.
    // stoppage is stopping duration in seconds.
    // interpName is the name of an interpolation function - expected to be
    // one of 'linear', 'sine', or 'skew_sine'.
    //
    // Return value is a function that you can pass to pluck() above
    // without having to specify the duration and stoppage explicitly.
    exports.pasr = function (pasr, stoppage, interpName) {
        var duration_secs, i, N, durAcc, interp, holdAtStart, holdAtEnd;
        var curve, seg;

        if (!pasr || pasr.length === 0) {
            return undefined;
        }

        interp = interpolation[(interpName in interpolation) ? interpName : "sine"];

        for (i = 0, N = pasr.length, duration_secs = 0.0, durAcc = [0.0]; i < N; ++i) {
            seg = pasr[i];
            durAcc.push(durAcc[durAcc.length - 1] + seg.attack + seg.sustain + seg.release);
        }

        duration_secs   = durAcc[durAcc.length - 1];
        holdAtStart     = durAcc[0] + pasr[0].attack + pasr[0].sustain;
        holdAtEnd       = duration_secs - pasr[pasr.length - 1].sustain - pasr[pasr.length - 1].release;

        // Calculates pitch when the segment in which the given time
        // falls is known.
        function curve_seg(t, i) {
            if (i + 1 >= pasr.length || pasr[i].release + pasr[i+1].attack < 0.001) {
                return pasr[i].pitch;
            } else {
                return pasr[i].pitch + (pasr[i+1].pitch - pasr[i].pitch) * interp(
                        (t - durAcc[i] - pasr[i].attack - pasr[i].sustain) / (pasr[i].release + pasr[i+1].attack),
                        (pasr[i].release / (pasr[i].release + pasr[i+1].attack)),
                        pasr[i].pitch,
                        pasr[i+1].pitch
                        );
            }
        }

        curve = function (t) {
            var i, j, k;
            if (t <= holdAtStart + kTimeEpsilon) {
                return pasr[0].pitch;
            } else if (t >= holdAtEnd - kTimeEpsilon) {
                return pasr[pasr.length - 1].pitch;
            } else {
                for (i = 0, j = pasr.length - 1; i < j; ++i) {
                    if (t >= durAcc[i] + pasr[i].attack + pasr[i].sustain - kTimeEpsilon) {
                        // In the release period.
                        if (t < durAcc[i+1] + pasr[i+1].attack + kTimeEpsilon) {
                            return pasr[i].pitch + (pasr[i+1].pitch - pasr[i].pitch) * interp(
                                    (t - durAcc[i] - pasr[i].attack - pasr[i].sustain) / (pasr[i].release + pasr[i+1].attack),
                                    (pasr[i].release / (pasr[i].release + pasr[i+1].attack)),
                                    pasr[i].pitch,
                                    pasr[i+1].pitch
                                    );
                        } else {
                            // Step to the next one.
                        }
                    } else if (t >= durAcc[i] + pasr[i].attack - kTimeEpsilon) {
                        // In the sustain period.
                        return pasr[i].pitch;
                    } else {
                        // In the attack period.
                    }
                }

                throw "Invalid case!";
            }
        };

        curve.duration_secs = duration_secs;
        curve.stoppage      = stoppage;
        curve.base_pitch    = Math.min.apply(null, pasr.map(function (p) { return p.pitch; }));

        return curve;
    };

    // Attempt at a bend+slide model of the movement in the pitch dimension.
    // This turns a 1D movement into a 2D model, which seems like complicating
    // things, but I think it may have some significant thing to say about
    // whether the music itself can lend itself to modeling in this form.
    //
    // A movement or "gamaka" is represented as two components, expressed as an
    // object with two fields 'dance' and 'stage' standing for relative
    // oscillations about a "stage" pitch. The 'dance' component is represented
    // and behaves as a regular pasr, but the 'stage' component is interpolated
    // using a stair step interpolation in the and 'slide' respectively. 1/L
    // domain instead of logarithmic (to match the veena). I don't think the
    // interpolation strategy itself is significant, but the important question
    // here is that does this "two dimensionalization" of the pitch movement
    // help with modeling across the board? In particular, if we also use sine
    // interpolation for the 'stage' part, we get similar to vocal movements. The
    // whole pitch curve is then considered to be the sum of these two
    // movements.
    exports.soasr = function (duration_secs, soasrn, stoppage, interpname, stageInterp) {

//        var oasrn = exports.pasrn(duration_secs, soasrn.dance);
//        var sasrn = exports.pasrn(duration_secs, soasrn.stage);
        var oasrCurve = exports.pasr(soasrn.dance, stoppage, interpname);
        var sasrCurve = exports.pasr(soasrn.stage, stoppage, stageInterp || 'fret_slide');

        function curve(t) {
            return oasrCurve(t) + sasrCurve(t);
        }

        curve.duration_secs = duration_secs;
        curve.stoppage = stoppage;
        curve.base_pitch = oasrCurve.base_pitch + sasrCurve.base_pitch;

        return curve;
    };

    exports.soasrn = function (duration_secs, soasrn) {
        return {
            stage: exports.pasrn(duration_secs, soasrn.stage),
            dance: exports.pasrn(duration_secs, soasrn.dance)
        };
    };

    exports.concat_soasrns = function (soasrns) {
        return {
            stage: [].concat.apply([], soasrns.map(function (so) { return so.stage; })),
            dance: [].concat.apply([], soasrns.map(function (so) { return so.dance; }))
        };
    };

    function pasrPitch(pasr) {
        if (pasr[0] instanceof Array) {
            return pasr[0];
        } else {
            return [pasr[0], 0];
        }
    }

    // Converts a duration normalized pasr expressed in array form,
    // where, for each entry 'pasr', pasr[0] = pitch,
    // pasr[1] = attack, pasr[2] = sustain and pasr[3] = release.
    exports.pasrn = function (duration_secs, pasrn) {
        var duration, i, N, scale;

        if (!pasrn || pasrn.length === 0) {
            return [];
        }

        for (i = 0, N = pasrn.length, duration = 0; i < N; ++i) {
            duration += pasrn[i][1] + pasrn[i][2] + pasrn[i][3];
        }

        scale = duration_secs / duration;

        return pasrn.map(function (pasr) {
            var p = pasrPitch(pasr);
            return {
                pitch: p[0] + p[1],
               logical_pitch: p[0],
               attack: pasr[1] * scale,
               sustain: pasr[2] * scale,
               release: pasr[3] * scale
            };
        });
    };

    // Directly accepts a normalized PASR form and yields a function
    // that can be passed to pluck().
    exports.pasre = function (duration_secs, pasrn, stoppage, interpName) {
        return exports.pasr(exports.pasrn(duration_secs, pasrn), stoppage, interpName);
    };

}

return exports;
});
