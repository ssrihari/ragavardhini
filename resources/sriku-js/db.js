package('srikumarks.phd.db', function () {

var exports = this;

// function that takes in a db (data structure like in sahana_db.js)
// and annotates the objects within it with all relevant connections
// so that you can go from any part of the db to all other related
// parts using links. 
//
// We don't care about memory usage.
function annotate(db, meta) {

    var pitch_contexts = {};

    // Keep a reference to the metadata.
    // This duplicates the 'info' field.
    db.meta = meta;

    // Reify the metadata field of each performance section.
    //
    // meta: Each one gets an additional "meta" field that points
    //       directly into the metadata object. 
    // part: Points to the part object with info about tala and 
    //       sections.
    // tala: It also gets a direct "tala" lookup.
    // plucks: The phrases are regrouped according to plucks in order
    //         to enable easy playback.
    //
    // The whole db gets -
    //
    // pitch_contexts: An object with keys that are pitch contet
    //                 id strings and values that are arrays of
    //                 notes that are in that pitch context.
    // pitch_context_ids: An array of string ids of the various 
    //                 pitch contexts in the db.
    //
    // Annotation for phrases -
    //
    // section: Points to the section that contains the phrase.
    // next: The phrase that immediately follows this one (or null).
    // prev: The phrase that immediately precedes this one (or null).
    // plucks: An array of plucks where each pluck is an
    //         array of notes in the phrase.
    //
    // Annotation for notes -
    //
    // phrase: Each note links back to the phrase it belongs to.
    // prev: Each note links to its previous note, or null if it is 
    //       the first note of a phrase.
    // next: Each note links to its next note, or null if it is 
    //       the last note of a phrase.
    // prescr: Each note's textual prescription is parsed and turned into
    //       an object stored in this field. See prescribedNote().
    // duration_secs: Gives the absolute duration of the note in the performance.
    // pitch_context: Object with "id" giving the context string and
    //       "peers" property giving an array of other notes that have
    //       the same context id.
    //
    db.performance.forEach(function (p) {
        var validPhrases = 0;
        p.label = p.meta; // Remember the full meta name as the part's "label".
        p.part = lookup(meta, p.meta.split('.')[0]); // Get a reference to the part (ex: 'pallavi').
        p.meta = lookup(meta, p.meta); // reify as an object
        p.tala = lookup(meta, p.part.tala); // ex: 'adi_kalai2', but reified as an object.
        p.pasr.forEach(function (phrase, i) {
            var validNotes = 0;
            phrase.section = p;
            phrase.prev = (i === 0 ? null : p.pasr[i-1]);
            phrase.next = (i + 1 === p.pasr.length ? null : p.pasr[i+1]);
            phrase.presc = phrase.map(function (n) { return n[0]; }); // p.meta.presc[i];

            // Annotate the notes in each phrase.
            phrase.forEach(function (note, j) {
                if (note[3] !== undefined) {
                    // Each "note" is a 4-element array where -
                    // note[0] = pitch prescription
                    // note[1] = "stage" of the note's movement (which is itself in pasr form).
                    // note[2] = "dance" of the note's movement to be applied atop the "stage".
                    // note[3] = the combined "pasr" movement.
                    // These are also reified as object fields of note so you don't have
                    // to use the index number in code.
                    note.prev = (j === 0 ? null : phrase[j-1]);
                    note.next = (j + 1 === phrase.length ? null : phrase[j+1]);
                    note.phrase = phrase;

                    note.prescr = prescribedNote(note[0]);
                    note.duration_secs = (note.prescr.duration / p.tala.pulses_per_count) * (p.tala.beats_per_count * 60 / p.tala.tempo_bpm) / p.speed;
                    note.pasr = note[3];
                    if (note[2].length === 0) {
                        console.error("WARNING: Raw pasr found in database. " + note[0] + ' in ' + JSON.stringify(note.phrase.presc) + ' in ' + note.phrase.section.label );
                        note.soasr = {stage: [[0, 0, 1, 0]], dance: note[3]};
                    } else {
                        note.soasr = {stage: note[1], dance: note[2]};
                    }
                    
                    recordDanceAmplitudes(note);
                    recordSimplifiedStageRepresentation(note);
                    recordDanceShapeRepresentation(note);

                    ++validNotes;
                }
            });

            // Collect together the contexts of all the notes,
            // stripping out the duration information in it.
            if (validNotes > 0) {
                phrase.forEach(function (note) {
                    var ctxt = pitchContext(note.prev, note, note.next);

                    // Make an empty array of contexts if this is a new one.
                    // This is a shared array, so the note will itself feature
                    // in its "peers" list.
                    if (!(ctxt in pitch_contexts)) {
                        pitch_contexts[ctxt] = [];
                    }

                    // Add the note to this context.
                    // Keep a reference to the array of notes with same context.
                    note.pitch_context = {
                        id: ctxt,
                        peers: pitch_contexts[ctxt]
                    };
                    note.pitch_context.peers.push(note);
                });

                // Keep info about plucks on a phrase-by-phrase basis as well.
                phrase.plucks = groupNotesIntoPlucks(phrase);
                ++validPhrases;
            }
        });

        if (validPhrases > 0) {
            // Group notes into plucks so that they can be played together more easily.
            p.plucks = groupNotesIntoPlucks([].concat.apply([], p.pasr));
        }
    });

    db.pitch_contexts = pitch_contexts;
    db.pitch_context_ids = Object.keys(pitch_contexts);
    db.pitch_context_vecs = db.pitch_context_ids.map(contextAsVector);
    return db;
}

// Finds all dance amplitudes associated with each stage pitch and records them
// in the array note.soasr.stage.amplitudes. The array has one entry object
// for each stage focal pitch.
//
// The object has two fields - .stats which gives the full stats for
// each amplitude found and .all which is a simple array of all amplitudes
// associated with the particular stage focal pitch.
function recordDanceAmplitudes(note) {
    var stage = timeStampedPASR(normalizePASR(note.soasr.stage, note.prescr.duration));
    var dance = timeStampedPASR(normalizePASR(note.soasr.dance, note.prescr.duration));

    var i, M, j, N, diff, amps = [], ps, pd, stageStart, stageEnd, s, d;
    for (i = 0, M = stage.length; i < M; ++i) {
        s = stage[i];
        ps = pasrRefPitch(s);
        stageEnd = s[3] > s[2] ? s[3] : s[4];
        stageStart = s[3] > s[2] ? s[2] : s[1];
        amps[i] = amps[i] || {stats: {}};
        for (j = 0, N = dance.length; j < N; ++j) {
            d = dance[j];
            diff = Math.min(stageEnd, d[4]) - Math.max(stageStart, d[1]);
            pd = pasrPitchValue(d); // FIXME: (NEXT) Was pasrRefPitch(d)
            if (pd !== 0 && diff > 0) {
                if (!(pd in amps[i].stats)) {
                    amps[i].stats[pd] = 0;
                }

                amps[i].stats[pd] += diff / (d[4] - d[1]);                
            }
        }
        amps[i].all = Object.keys(amps[i].stats).map(function (n) { return +n; });
    }

    console.assert(amps.length === stage.length);
    note.soasr.stage.amplitudes = amps;
}

// The simplified stage representation stores only the stage pitch
// and the associated dance amplitudes. This is stored as an array
// with one entry per stage focal pitch, in note.soasr.stage.simplified.
function recordSimplifiedStageRepresentation(note) {
    // Need recordDanceAmplitudes to be called before this one.
    console.assert(note.soasr.stage.amplitudes);

    note.soasr.stage.simplified = note.soasr.stage.amplitudes.map(function (amp, i) {
        return [pasrRefPitch(note.soasr.stage[i]), amp.all];
    });
        
    note.soasr.stage.simplified_partial = note.soasr.stage.amplitudes.map(function (amp, i) {
        return pasrRefPitch(note.soasr.stage[i]);
    });

}

// The dance shape representation consists of storing, for each
// dance focal pitch, one of three pitches (+1, 0, -1) and a timing 
// category number for the focal pitch - which is one of (0, 1, 2).
// 
// The three dance pitch categories are because the actual amplitude
// will be stored associated with the stage focal pitches. The duration
// categories come from the fact that the distribution of the sustain-to-transient
// timing ratio for focal pitches is trimodal (with a negligible fourth mode).
//
// Timing categories -
//  0 => Transient
//  1 => Neutral
//  2 => Sustained
function recordDanceShapeRepresentation(note) {
    note.soasr.dance.shape = note.soasr.dance.map(function (fp) {
        return [categorizeFocalPitch(fp), categorizeFocalPitchDuration(fp).category];
    });

    note.soasr.dance.shape_partial = note.soasr.dance.map(categorizeFocalPitch);
    note.soasr.dance.shape_semicat = note.soasr.dance.map(function (fp) { 
        return [categorizeFocalPitch(fp), Math.min(1, categorizeFocalPitchDuration(fp).category)]; 
    });
}

function categorizeFocalPitch(pasr) {
    var p = pasrPitchValue(pasr);
    return p > 0.05 ? 1 : (p < -0.05 ? -1 : p); // FIXME: Changed.
//    return p > 0.9 ? 1 : (p < -0.9 ? -1 : p);
}

function categorizeFocalPitchDuration(fp, threshold) {
    // For why the threshold is chosen such, see the
    // trimodal distribution in the _duration_distrib_dance_sahana.json file.
    threshold = threshold || 4;

    // Returns 0 for transient, 1 for intermediate and 2 for sustainable.
    var s = fp[2];
    var ar = fp[1] + fp[3];
    if (s + ar < 0.001) {
        return {slot: -6, category: 0, tilt: 0}; // Definitely a transient.
    }

    var category = Math.round(mew((s - ar) / (s + ar), 64));
    var tilt = Math.round(mew((fp[1] - fp[3]) / (s + ar), 64));

    if (category < -threshold) {
        return {slot: category, category: 0, tilt: tilt};
    } else if (category > threshold) {
        return {slot: category, category: 2, tilt: tilt};
    } else {
        return {slot: category, category: 1, tilt: tilt};
    }
}

function mew(x, k) {
    x *= k;
    if (x > 1) {
        return Math.log(1 + x) / Math.LN2;
    } else if (x < -1) {
        return - Math.log(1 - x) / Math.LN2;
    } else {
        return x;
    }
}

// Turns a context string into a vector of three numbers,
// with the first giving the main pitch, the second giving
// the change from the preceding one to the this one and the
// third giving the change from this one to the following one.
//
// If prev or next pitch is '-', the change is considered to
// be 0.
function contextAsVector(contextStr) {
    var parts = contextStr.split('|').map(function (p) {
        if (p === '-') {
            return null;
        } else {
            return pitchValue(p);
        }
    });

    if (parts[0] === null) {
        parts[0] = parts[1];
    }

    if (parts[2] === null) {
        parts[2] = parts[1];
    }

    return [parts[1], parts[1] - parts[0], parts[2] - parts[1]];
}

var canonicalizeOctave = (function () {
    var lowerSuffix = '----';
    var upperSuffix = '++++';
    var octOps = { "+": 1, "-": -1 };

    // ma1++-++-- => ma1+
    function canonical(pitch) {
        var oct = 0;
        var i, N;
        var pc = pitch.split(/[-+]+/)[0];
        for (i = pc.length, N = pitch.length; i < N; ++i) {
            oct += octOps[pitch.charAt(i)];
        }

        if (Math.abs(oct) > 4) {
            throw "Octave out of range";
        }

        if (oct >= 0) {
            return pc + upperSuffix.substr(0, oct);
        } else {
            return pc + lowerSuffix.substr(0, -oct);
        }
    }

    return canonical;
}());

// Takes a pitch context string id and returns the id of
// the context that is octave shifted by the given number.
// +ve values for shift mean shift octave up and -ve
// values mean shift down.
function octaveShift(contextStr, shift) {
    if (shift < -4 || shift > 4) {
        throw "Octave out of range";
    }

    var suffix = (shift >= 0 ? '++++' : '----').substr(0, Math.abs(shift));

    return contextStr.split('|').map(function (p) { return canonicalizeOctave(p + suffix); }).join('|');
}

function groupNotesIntoPlucks(phrase) {
    var result = [];
    phrase.forEach(function (note, i) {
        if (note.prescr.emph || result.length === 0) {
            // Start of new pluck.
            note.stoppage_secs = 0.03; // Put in some default value here for now.
            result.push([]);
        }

        result[result.length - 1].push(note);
    });

    return result;    
}

function lookup(meta, key) {
    function lookupField(obj, field) {
        return obj[field];
    }

    return key.split('.').reduce(lookupField, meta);
}

function prescribedNote(token) {
    var pitchAndDur, pitch, pauses;
    pauses = token.match(/^,+$/);
    if (pauses) {
        return {
            pause: true,
            pitch: '-',
            pitch_class: null,
            pitch_value: null,
            duration: pauses[0].length,
            emph: false
        };
    } else {
        pitchAndDur = token.split(':');
        if (pitchAndDur[0].charAt(0) === '^') {
            pitch = pitchAndDur[0].substr(1);
            return {
                pause: false,
                pitch: pitch,
                pitch_class: pitchClass(pitch),
                pitch_value: pitchValue(pitch),
                duration: (pitchAndDur[1] && parseFloat(pitchAndDur[1])) || 1,
                emph: true
            };
        } else {
            return {
                pause: false,
                pitch: pitchAndDur[0],
                pitch_class: pitchClass(pitchAndDur[0]),
                pitch_value: pitchValue(pitchAndDur[0]),
                duration: (pitchAndDur[1] && parseFloat(pitchAndDur[1])) || 1,
                emph: false
            };
        }
    }
}


var kPitchValue = {
    'sa':0, 'ri1':1, 'ri2':2, 'ri3':3, 'ga1':2, 'ga2':3, 'ga3':4, 'ma1':5, 'ma2':6,
    'pa':7, 'da1':8, 'da2':9, 'da3':10, 'ni1':9, 'ni2':10, 'ni3':11
};

// ga3+ -> ga3
function pitchClass(pitchStr) {
    var suffix;

    if (pitchStr in kPitchValue) {
        return pitchStr;
    }

    if (pitchStr.length > 0) {
        suffix = pitchStr.charAt(pitchStr.length - 1);

        if (suffix === '+' || suffix === '-') {
            return pitchClass(pitchStr.substr(0, pitchStr.length - 1));
        }
    }

    throw new Error("Invalid pitch string " + pitchStr);
}

// ga3+ -> 16
function pitchValue(pitchStr) {
    var suffix;

    if (pitchStr in kPitchValue) {
        return kPitchValue[pitchStr];
    }

    if (pitchStr.length > 0) {
        suffix = pitchStr.charAt(pitchStr.length - 1);

        if (suffix === '+' || suffix === '-') {
            return pitchValue(pitchStr.substr(0, pitchStr.length - 1)) + (suffix === '+' ? 12 : -12);
        }
    }

    throw new Error("Invalid pitch string " + pitchStr);
}

// Gives the string id corresponding to the given sequence
// of three prescribed notes. Each prescrN is expected to be
// an object as returned by prescribedNote(), or null.
// prescr2 CANNOT be null.
function pitchContext(prescr1, prescr2, prescr3) {
    var lcontext = prescr1 ? prescr1.prescr.pitch : '-';
    var rcontext = prescr3 ? prescr3.prescr.pitch : '-';
    return lcontext + '|' + prescr2.prescr.pitch + '|' + rcontext;
}

function loadDB(ragaName) {
    var db = package('srikumarks.phd.raga.' + ragaName + '.db');
    var meta = package('srikumarks.phd.raga.' + ragaName + '.meta');
    return annotate(db, meta);
}

function pasrPitch(pasr) {
    if (pasr[0] instanceof Array) {
        return pasr[0];
    } else {
        return [pasr[0], 0];
    }
}

function pasrRefPitch(pasr) {
    if (pasr[0] instanceof Array) {
        return pasr[0][0];
    } else {
        return pasr[0];
    }
}

function pasrPitchValue(pasr) {
    if (pasr[0] instanceof Array) {
        return pasr[0][0] + pasr[0][1];
    } else {
        return pasr[0];
    }
}

// Transforms array of [p,a,s,r] to array of [p, t, t+a, t+a+s, t+a+s+r] 
// where the t accumulates from previous pasrs. In other words, the
// various parts of the PASR form get "time stamps" instead of durations.
// The "duration" property of the return value gives the total duration.
function timeStampedPASR(pasr) {
    if (pasr[0].length === 5) {
        // Already time stamped form.
        return pasr;
    }

    var t = 0, i, N, result = [], fp, start, attackEnd, sustainEnd, releaseEnd;
    for (i = 0, N = pasr.length; i < N; ++i) {
        fp = pasr[i];
        start = t;
        attackEnd = start + fp[1];
        sustainEnd = attackEnd + fp[2];
        releaseEnd = sustainEnd + fp[3];
        t = releaseEnd;
        result[i] = [fp[0], start, attackEnd, sustainEnd, releaseEnd];

    }
    result.duration = t;
    return result;
}

// Given a time stamped PASR, turns it into an interval based PASR.
function intervalPASR(tspasr) {
    var i, N, fp, result = [];
    for (i = 0, N = tspasr.length; i < N; ++i) {
        fp = tspasr[i];
        result[i] = [fp[0], fp[2] - fp[1], fp[3] - fp[2], fp[4] - fp[3]];
    }
    result.duration = tspasr.duration;
    return result;
}

// Converts a PASR so that it only has sustain parts
// and all attack/release parts are merged into the sustain.
function sustainedPASR(pasr) {
    var i, N, result = [], fp;
    for (i = 0, N = pasr.length; i < N; ++i) {
        fp = pasr[i];
        result[i] = [fp[0], 0, fp[1] + fp[2] + fp[3], 0];
    }
    return result;
}

// Gives a Euclidean pitch based distance measure
// between the two given PASRs, computed by resampling
// them to the same duration and finding the rms difference
// between the two resulting vectors.
function pasrPitchDistance(p1, p2, Ns) {
    var p1n, p2n, d, dsum, i, N;

    Ns = Ns || 240;
    p1n = resamplePASR(p1, Ns);
    p2n = resamplePASR(p2, Ns);
    for (i = 0, N = p1n.length, dsum = 0; i < N; ++i) {
        d = p1n[i] - p2n[i];
        dsum += d * d;
    }

    return Math.sqrt(dsum / N);
}

// Computes the distance between the two PASRs given in 
// "time stamped" form. See timeStampedPASR.
function pasrDistance12(tspasr1, tspasr2) {
    console.assert(tspasr1[0].length === 5);
    console.assert(tspasr2[0].length === 5);

    var dur1 = tspasr1.duration;
    var dur2 = tspasr2.duration;
    var i, j, M, N;
    var dist = 0, dmin, pmin, p1, p2, p1_dur, p2_dur, dt, dt_lo, dt_hi, d;
    var n1 = 1 / dur1;
    var n2 = 1 / dur2;
    var kdur = Math.LN2 * 4; 
    for (i = 0, M = tspasr1.length; i < M; ++i) {
        p1 = pasrPitchValue(tspasr1[i]);
        p1_dur = n1 * (tspasr1[i][4] - tspasr1[i][1]);
        dmin = 1e6;
        pmin = p1 + 36;
        for (j = 0, N = tspasr2.length; j < N; ++j) {
            p2 = pasrPitchValue(tspasr2[j]);
            p2_dur = n2 * (tspasr2[j][4] - tspasr2[j][1]);
            if (p2_dur > 0) {
                dt_lo = Math.max(n1 * tspasr1[i][1], n2 * tspasr2[j][1]);
                dt_hi = Math.min(n1 * tspasr1[i][4], n2 * tspasr2[j][4]);
                dt = Math.max(n2 * tspasr2[j][4], n1 * tspasr1[i][4]) - Math.min(n2 * tspasr2[j][1], n1 * tspasr1[i][1]);
                d = 1 + (dt_lo - dt_hi) / dt;
                if (d < dmin) {
                    dmin = d;
                    pmin = p2;
                }
            }
        }
        dist += Math.abs(pmin - p1) + 36 * dmin;
    }
    return dist;
}

// Evaluates a "shape" curve for the given time stamped pasr 
// focal pitch.
function tpasrShape(fp, t) {
    if (t < fp[1]) {
        return 0;
    } else if (t < fp[2]) {
        return (t - fp[1]) / (fp[2] - fp[1]);
    } else if (t < fp[3]) {
        return 1;
    } else if (t < fp[4]) {
        return (fp[4] - t) / (fp[4] - fp[3]);
    } else {
        return 0;
    }
}

// p1 and p2 are in quantized and time stamped PASR form.
// This means that all time values are integers and 
// each focal pitch has 4 times associated with it
// that give the absolute time of the movement knee points
// from the start of the pasr form.
function qpasrDot(p1, p2) {
    var n1 = p1[4] - p1[1];
    var n2 = p2[4] - p2[1];
    if (n1 == 0 || n2 === 0) {
        return -1;
    }

    var t, tmin, tmax, p1sum = 0, p2sum = 0, p1sqsum = 0, p2sqsum = 0, p12sum = 0, p1val, p2val;
    tmin = Math.min(p1[1], p2[1]);
    tmax = Math.max(p1[4], p2[4]);

    for (t = tmin; t < tmax; ++t) {
        p1val = tpasrShape(p1, t);
        p2val = tpasrShape(p2, t);

        p1sum += p1val;
        p2sum += p2val;
        p1sqsum += p1val * p1val;
        p2sqsum += p2val * p2val;
        p12sum += p1val * p2val;
    }

    var n = Math.min(tmax - tmin, n1 + n2);
    // Note that the result actually depends on what we
    // choose for N. In this case, we choose N such that
    // when considering two focal pitches that don't overlap,
    // it is irrelevant how far apart they are, and when
    // they do overlap, that is taken into account.

    if (n === 0) {
        return tmax > tmin ? -1 : 1;
    }
    p1sum /= n;
    p2sum /= n;
    p12sum /= n;
    p1sqsum /= n;
    p2sqsum /= n;

    var dot = (p12sum - p1sum * p2sum) / (p1sum * p2sum);
    /**
     * The above dot product works better than the one below for the case of PASR.
     * The idea is for the dot product to be exactly -1 whenever two pasrs have
     * absolutely no overlap. The softer formula below is better behaved and does
     * give substantially negative values below -0.5 usually, but it can be much
     * harder, since we don't have the strict constraint that such a "correlation"
     * feature must be in the range [-1,1].
     *
     * var dot1 = (p12sum - p1sum * p2sum) / Math.sqrt((p1sqsum - p1sum * p1sum) * (p2sqsum - p2sum * p2sum));
     */
    if (isNaN(dot)) {
        debugger;
    }
    console.assert(isNaN(dot) === false);
    return dot;
}

function pasrDistance(tspasr1, tspasr2, N) {
    var qpasr1 = quantizePASR(timeStampedPASR(tspasr1), N);
    var qpasr2 = quantizePASR(timeStampedPASR(tspasr2), N);

    var i, j, imax, jmax, d = 0, p1, p2, dp, kexp = Math.LN2 * 5, kzero = Math.exp(-kexp), w = 0, dw = 0;
    for (i = 0, imax = qpasr1.length; i < imax; ++i) {
        p1 = pasrPitchValue(qpasr1[i]);
        for (j = 0, jmax = qpasr2.length; j < jmax; ++j) {
            p2 = pasrPitchValue(qpasr2[j]);
            dp = Math.min(1, Math.abs(p2 - p1));
            // Using an exponential weight based on the dot product allows
            // us to ignore non-overlapping focal pitches entirely while
            // quickly scaling up to include partial and complete ovelaps.
            //
            // The Math.max is so that we avoid the value becoming negative in
            // the denormalized floating point zone.
            dw = Math.max(0, Math.exp(kexp * qpasrDot(focalPitch(qpasr1, i), focalPitch(qpasr2, j))) - kzero);
            w += dw;
            d += dp * dp * dw;
        }
    }

    return Math.sqrt(d * qpasr1.length * qpasr2.length / w);
}

function focalPitch(tspasr, i, extend) {
    if (extend) {
        // Extended focal pitch, which extends the attack
        // and release time to cover the adjacent ones.
        var fp = tspasr[i];
        if (fp.length < 5) {
            console.error("WARNING[focalPitch]: Pass in time stamped PASR for efficiency. Forcing conversion.");
            tspasr = timeStampedPASR(tspasr);
            fp = tspasr[i];
        }
        return [ fp[0]
               , i > 0 ? tspasr[i-1][3] : fp[1]
               , fp[2]
               , fp[3]
               , i + 1 < tspasr.length ? tspasr[i+1][2] : fp[4]
               ];
    } else {
        // This is the simple version.
        // Works for pasr and time stamped pasr.
        return tspasr[i];
    }
}

function normalizePASR(pasr, N) {
    var total = pasr.reduce(function (acc, f) {
        return acc + f[1] + f[2] + f[3];
    }, 0);

    var norm = N / total;
    return pasr.map(function (f) {
        return [f[0], f[1] * norm, f[2] * norm, f[3] * norm];
    });
}

// Turns the given PASR form into N samples at regular intervals
// over its duration. The return value is an array of N pitch
// values. The array also has two fields "start" and "end"
// which mark the interval for which interpolation is valid.
function resamplePASR(pasr, N) {
    var np = quantizePASR(pasr, N);
    var t = 0;
    var sp = [];
    var lastPitch = pasrPitchValue(np[0]);
    np.forEach(function (p, i) {
        var t1, t2, thisPitch;
        thisPitch = pasrPitchValue(p);
        if (i === 0) {
            // Merge attack and sustain.
            t2 = t + p[1] + p[2];
            sp.start = t + p[1];
        } else {
            // Take care of sustain only.
            t2 = t + p[2];
        }

        for (t1 = t; t1 < t2; t1 += 1) {
            sp[t1] = thisPitch;
        }
        t = t1;

        if (i + 1 === np.length) {
            // Last one. Process release.
            sp.end = t;
            for (t1 = t, t2 = t + p[3]; t1 < t2; t1 += 1) {
                sp[t1] = thisPitch;
            }
        } else {
            // Process release up to end of next attack.
            lastPitch = thisPitch;
            thisPitch = pasrPitchValue(np[i+1]);
            for (t1 = t, t2 = t + p[3] + np[i+1][1]; t1 < t2; t1 += 1) {
                // Linear interpolation
                sp[t1] = lastPitch + (thisPitch - lastPitch) * (t1 - t) / (t2 - t);
            }
        }

        t = t1;
    });

    console.assert(sp.length === N);
    return sp;
}


function quantizePASR(pasr, N) {
    console.assert(pasr && pasr.length > 0);
    if (pasr[0].length === 4) {
        // Normal pasr.
        return intervalPASR(quantizeTimeStampedPASR(timeStampedPASR(pasr), N));
    } else {
        // Time stamped PASR
        return quantizeTimeStampedPASR(pasr, N);
    }
}

function quantizeTimeStampedPASR(tpasr, N) {
    console.assert(tpasr && tpasr.length > 0 && tpasr[0].length === 5);
    console.assert(N > 0);

    var normFactor = N / tpasr.duration;

    function qnorm(t) { 
        return Math.round(t * normFactor); 
    }

    var i, N, result = [], fp, ts;
    for (i = 0, N = tpasr.length; i < N; ++i) {
        fp = tpasr[i];
        ts = fp.slice(1).map(qnorm);
        ts.unshift(fp[0]);
        result[i] = ts;
    }

    result.duration = N;
    return result;
}

function canonicalPASR(pasr) {
    var result = [], i, N, fp;
    if (pasr[0].length < 5) {
        // Normal PASR.
        for (i = 0, N = pasr.length; i < N; ++i) {
            if (result.length > 0) {
                fp = result[result.length - 1];
                if (pasrPitchValue(fp) !== pasrPitchValue(pasr[i])) {
                    result.push(pasr[i]);
                } else {
                    fp[2] += fp[3] + pasr[i][1] + pasr[i][2];
                    fp[3] = pasr[i][3];
                }
            } else {
                result.push(pasr[i]);
            }
        }
    } else {
        // Time stamped form.
        for (i = 0, N = pasr.length; i < N; ++i) {
            if (result.length > 0) {
                fp = result[result.length - 1];
                if (pasrPitchValue(fp) !== pasrPitchValue(pasr[i])) {
                    result.push(pasr[i]);
                } else {
                    fp[3] = pasr[i][3];
                    fp[4] = pasr[i][4];
                }
             } else {
                result.push(pasr[i]);
            }
        }
    }
    return result;
}

function selectLabels(db, regexp) {
    return db.performance.filter(function (part) {
        return regexp.test(part.label);
    });
}

exports.load                = loadDB;
exports.pitch_context       = pitchContext;
exports.prescribed_note     = prescribedNote;
exports.context_as_vector   = contextAsVector;
exports.octave_shift        = octaveShift;
exports.pasr_pitch          = pasrPitch;
exports.pasr_pitch_value    = pasrPitchValue;
exports.pasr_pitch_distance = pasrPitchDistance;
exports.pasr_normalize      = normalizePASR;
exports.pasr_resample       = resamplePASR;
exports.pasr_quantize       = quantizePASR;
exports.pasr_interval2ts    = timeStampedPASR;
exports.pasr_ts2interval    = intervalPASR;
exports.pasr_distance       = pasrDistance;
exports.pasr_sustained      = sustainedPASR;
exports.pasr_canonical      = canonicalPASR;
exports.select_labels       = selectLabels;
exports.pasr_pretty_print   = function (pasr) {
    return canonicalPASR(pasr).map(JSON.stringify);
};

return exports;
});
