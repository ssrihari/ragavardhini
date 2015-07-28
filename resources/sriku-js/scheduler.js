package.declare([
        'org.anclab.steller.scheduler',
        'org.anclab.steller.timer',
        'org.anclab.steller.clock'
        ]);

/**
 * org.anclab.steller.scheduler
 * ----------------------------
 *
 * This is a simple scheduler for "models" .. which are functions
 *  of the form --
 *       function (sched, t1, t2, next) {
 *           // ... do something
 *           next(sched, t1, t2, sched.stop); // Go to the next one if you wish.
 *       }
 *   where --
 * 
 *   'sched' is the scheduler object.
 *   't1-t2' is the interval for which the call is being made.
 *   'next' is the model that is supposed to follow this one in time.
 * 
 *   To use the scheduler, you first make an instance using "new".
 *
 *      var sh = new Scheduler;
 *
 *   Then you start it running by setting the 'running' property to true.
 *
 *      sh.running = true;
 *
 *   Then you can play models already. Here is something that will keep
 *   outputting 'fizz', 'buzz' alternately every 2 seconds.
 *
 *      var p = Parameterize({});
 *      p.params.define({name: 'dur', min: 0.01, max: 60, value: 2});
 *      var fizzbuzz = sh.loop(sh.track([
 *          sh.log('fizz'), sh.delay(p.dur), 
 *          sh.log('buzz'), sh.delay(p.dur)
 *      ]));
 *      sh.play(fizzbuzz);
 * 
 *  Now try changing the value of the duration parameter p.dur like below
 *  while the fizzes and buzzes are being printed out --
 *      
 *      p.dur.value = 1
 */
package('org.anclab.steller.scheduler', ['#global', '.timer', '.clock'], function (window, Timer, Clock) {

    function Scheduler(audioContext) {
        // Make sure we don't clobber the global namespace accidentally.
        var self = (this === window ? {} : this);

        var time_secs = (function () {
            if (!audioContext) {
                return function () {
                    return Date.now() * 0.001;
                };
            } else if (audioContext.constructor.name === 'AudioContext') {
                return function () {
                    return audioContext.currentTime;
                };
            } else {
                throw new Error("Scheduler: Argument is not an audio context");
            }
        }());


        var timer, running = false;

        // To start the scheduler, set "scheduler.running = true"
        // To stop it, set it to false.
        self.__defineGetter__('running', function () { return running; });
        self.__defineSetter__('running', function (state) {
            if (state) {
                if (!running) {
                    running = true;
                    clock.jumpTo(time_secs());
                    timer.start();
                }
            } else {
                running = false;
                timer.stop();
            }
        });


        // A pair of arrays used as an event tick queue.
        // Models placed in queue are processed and the resultant
        // models scheduled go into the requeue. Then after one
        // such cycle, the variables are swapped.
        var queue = [];
        var requeue = [];

        function flush() {
            queue.splice(0, queue.length);
            requeue.splice(0, requeue.length);
        }

        // Keep track of time.
        var clockDt = 0.02; // Use a 50Hz time step.
        var clockBigDt = clockDt * 5; // A larger 10Hz time step.
        var clock = new Clock(time_secs(), 0, clockDt, 1.0);

        function scheduleTick() {
            // Main scheduling work happens here.
            var i, N, tmpQ;
            var now_secs = time_secs() + clockDt;

            // If lagging behind, advance time before processing models.
            while (now_secs - clock.t1 > clockBigDt) {
                clock.advance(clockBigDt);
            }

            while (clock.t1 < now_secs) {
                tmpQ = queue;
                queue = requeue;

                // Process the scheduled tickers. The tickers
                // will know to schedule themselves and for that
                // we pass them the scheduler itself.
                for (i = 0, N = tmpQ.length; i < N; ++i) {
                    tmpQ[i](self, clock, cont);
                }

                tmpQ.splice(0, tmpQ.length);
                requeue = tmpQ;
                clock.tick();
            }
        }

        // scheduleTick needs to be called with good solid regularity.
        // If we're running the scheduler under node.js, it can only
        // be because MIDI is needed, which needs high precision,
        // indicated by 0.
        timer = new Timer(scheduleTick, 0);

        // Schedules the model by placing it into the processing queue.
        function schedule(model) {
            if (model) {
                queue.push(model);
            }
        }

        // Wraps the concept of "performing" a model so that
        // the representation of the model as a continuation 
        // is not strewn all over the place. Note that the
        // "current scheduler" is used by perform via "this",
        // so perform *must* be called like a method on the 
        // scheduler.
        function perform(model, clock, next) {
            model(this, clock, next);
        }

        // Having constructed a model, you use play() to play it.
        // The playing starts immediately. See 'delay' below if you want
        // the model to start playing some time in the future.
        function play(model) {
            this.perform(model, clock.copy(), stop);
        }

        // This "model" says "stop right here, nothing more to do."
        // This is the "zero" of the algebra. No model placed after a stop
        // in a sequence will get to run.
        function stop(sched, clock, next) {
        }

        // This "model" just says "continue on with whatever's next".
        // This is the "one" of the algebra. Placing it anywhere in
        // a sequence has no consequence on it.
        function cont(sched, clock, next) {
            next && sched.perform(next, clock, stop);
        }

        // Gives a model that introduces the given amount of delay in a
        // sequence. Notice that the "valueOf" protocol on dt is used. This
        // lets us introduce fixed as well as variable delays. This is the
        // absolute *core* of the "scheduler" since it is the only function
        // which actually does something about invoking stuff at a specified
        // time! Any optimization of the scheduling loop will also involve this
        // function and, likely, this one *only*.
        //
        // Example:
        //
        //      sh.play(sh.track(sh.delay(1), model))
        //
        // Will cause the model to play after 1 second.
        //
        // If callback is provided, it will be called throughout the wait
        // period with the arguments (t1, t2, startTime, endTime) giving the
        // interval for which it is being called. Continuous parameter
        // animations can be handled using the callback, for example.
        function delay(dt, callback) {
            return function (sched, clock, next) {
                var startTime = clock.t1r;
                (function tick(sched, clock) {
                    var endTime = startTime + dt.valueOf();
                    if (clock.t2r < endTime) {
                        if (callback) {
                            callback(clock, clock.t1r, clock.t2r, startTime, endTime);
                        }
                        schedule(function (sched, clockT) {
                            sched.perform(tick, clock.tick());
                        });
                    } else {
                        if (callback && endTime > clock.t1r) {
                            callback(clock, clock.t1r, endTime, startTime, endTime);
                        }
                        if (clock.t2r > clock.t1r) {
                            sched.perform(next, clock.nudgeToRel(endTime), sched.stop);
                        } else {
                            sched.perform(next, clock, sched.stop);
                        }
                    }
                }(sched, clock));
            };
        }

        // The two given models will be performed in sequence.
        // When the first model ends, it will transfer control
        // to the second model. 
        //
        // Note: This is an internal combinator exposed via the
        // more convenient "track".
        function seq(model1, model2) {
            return function (sched, clock, next) {
                sched.perform(model1, clock, seq(model2, next));
            };
        }

        // Here is a model that will never end. The given model
        // will be looped forever. You better have a delay in 
        // there or you'll get an infinite loop or blow the stack
        // or something like that.
        function loop(model) {
            return function looper(sched, clock, next) {
                sched.perform(model, clock, looper);
            };
        }

        // The models in the given array are spawned off simultanously.
        // When all the models finish their work, the fork will
        // continue on with whatever comes next.
        //
        //  Ex:     sh.play(sh.track(sh.fork([drumpat1, drumpat2]), drumpat3));
        //  That will cause pat1 to be played simultaneously with pat2
        //  and when both finish, pat3 will play.
        //
        //  Support both fork(a, b, c, ..) and fork([a, b, c, ..]) forms.
        //
        function fork(models) {
            if (models && models.constructor === Function) {
                // We're given the models as arguments instead of an array.
                models = Array.prototype.slice.call(arguments, 0);
            } else {
                models = models.slice(0);
            }
            return function (sched, clock, next) {
                var syncCount = 0;
                function join(sched, clockJ) {
                    syncCount++;
                    if (syncCount === models.length) {
                        // All models have finished.
                        sched.perform(next, clock.jumpTo(clockJ.t1), sched.stop);
                    }
                };

                // Start off all models.
                models.forEach(function (model) {
                    sched.perform(model, clock.copy(), join);
                });
            };
        }

        // Similar to fork, except that the spawn will immediately
        // continue on with whatever is next, as though its duration
        // is zero.
        //
        // Support both spawn(a, b, c, ..) and spawn([a, b, c, ..]) forms.
        function spawn(models) {
            if (models && models.constructor === Function) {
                // We're given the models as arguments instead of an array.
                models = Array.prototype.slice.call(arguments, 0);
            } else {
                models = models.slice(0);
            }
            return function (sched, clock, next) {
                models.forEach(function (model) {
                    sched.perform(model, clock.copy(), sched.stop);
                });
                sched.perform(next, clock, sched.stop);
            };
        }

        // A generic 'dynamic model', which determines the
        // model to use at any given time according to some
        // rule. 'dyn' is a function (t1, t2) and is expected
        // to return a model, which is then scheduled. You can
        // use this, for example, to do random choices, conditional
        // choices, etc.
        function dynamic(dyn) {
            return function (sched, clock, next) {
                sched.perform(dyn(clock), clock, next);
            };
        }

        // Produces a model that consists of a sequence of
        // the given models (given as an array of models).
        // 
        // track([a,b,c,d]) is just short hand for
        // seq(a, seq(b, seq(c, d)))
        //
        // Support both track(a, b, c, ..) and track([a, b, c, ..]) forms.
        //
        // Note that the intermediate continuations are one-shot
        // and are not reusable for the sake of performance. If you
        // need reusable continuations, use trackR instead of track.
        function track(models) {
            if (models && models.constructor === Function) {
                // We're given the models as arguments instead of an array.
                models = Array.prototype.slice.call(arguments, 0);
            }

            if (!models || models.constructor !== Array || models.length === 0) {
                return cont;
            }

            if (models.length === 1) {
                return models[0];
            }

            return function (sched, clock, next) {
                var i = 0;
                sched.perform(function iter(sched, clock, _) {
                    if (i < models.length) {
                        sched.perform(models[i++], clock, iter);
                    } else {
                        sched.perform(next, clock, stop);
                    }
                }, clock, next);
            };
        }

        function trackR_iter(models, i, next) {
            if (i < models.length) {
                return function (sched, clock, _) {
                    sched.perform(models[i], clock, trackR_iter(models, i + 1, next));
                };
            } else {
                return next;
            }
        }

        // Functionally identical to track, but generates reusable
        // continuations on the fly. Not usually needed and track
        // is more memory efficient at doing what it does, but 
        // this could be useful for some interesting effects such as
        // canonization operators, or when you need to store away
        // a continuation and revisit it later. The "R" in the name
        // stands for "reusable continuations".
        function trackR(models) {
            if (models && models.constructor === Function) {
                // We're given the models as arguments instead of an array.
                models = Array.prototype.slice.call(arguments, 0);
            }

            if (!models || models.constructor !== Array || models.length === 0) {
                return cont;
            }

            if (models.length === 1) {
                return models[0];
            }


            return function (sched, clock, next) {
                sched.perform(trackR_iter(models, 0, next), clock, stop);
            };
        }

        // A model that simply fires the given call when it happens, takes
        // zero duration itself and moves on.
        function fire(callback) {
            return function (sched, clock, next) {
                callback(clock);
                sched.perform(next, clock, stop);
            };
        };

        // Useful logging utility.
        function log(msg) {
            return fire(function () {
                console.log(msg);
            });
        }

        // Parameter animation curves.
        //
        // anim(param, dur, func):
        // func is expected to be a function (t) where t is 
        // in the range [0,1]. The given parameter will be assigned
        // the value of the function over the given duration.
        //
        // anim(param, dur, v1, v2):
        // The parameter will be linearly interpolated over the given duration
        // starting with value v1 and ending with v2.
        //
        // anim(param, dur, v1, v2, interp):
        // The parameter will be interpolated from value v1 to value v2
        // over the given duration using the given interpolation function
        // interp(t) whose domain and range are both [0,1].
        //
        // Note that animation curves have a duration so that you
        // can sequence different curves using track().
        // If you want a bunch of parameters to animate simultaneously,
        // you need to use spawn() or fork().
        //
        // Also remember that the "dur" parameter can be anything
        // that supports "valueOf" protocol. That said, be aware that
        // varying the duration can result in the parameter jumping
        // values due to large changes in the fractional time. Sometimes,
        // that might be exactly what you want and at other times that
        // may not be what you want.
        function anim(param, dur) {
            var v1, v2, func, afunc;
            switch (arguments.length) {
                case 3: // Third argument must be a function.
                    afunc = arguments[2];
                    break;
                case 4: // Third and fourth arguments are starting
                        // and ending values over the duration.
                    v1 = arguments[2];
                    v2 = arguments[3];
                    afunc = function (f) { return v1 + f * (v2 - v1); };
                    break;
                case 5: // Third and fourth are v1, and v2 and fifth is
                        // a function(fractionalTime) whose return value is
                        // in the range [0,1] which is remapped to [v1,v2].
                        // i.e. the function is an interpolation function.
                    v1 = arguments[2];
                    v2 = arguments[3];
                    func = arguments[4];
                    afunc = function (f) { return v1 + func(f) * (v2 - v1); };
                    break;
                default:
                    throw new Error("Invalid arguments to anim()");
            }

            
            if (param.constructor.name === 'AudioParam') {
                // Use linear ramp for audio parameters.
                return delay(dur, function (clock, t1, t2, startTime, endTime) {
                    var dt = endTime - startTime;
                    if (t1 <= startTime) {
                        param.setValueAtTime(afunc((t1 - startTime) / dt), t1);
                    }
                    param.linearRampToValueAtTime(afunc((t2 - startTime) / dt), t2);
                });
            } else {
                return delay(dur, function (clock, t1, t2, startTime, endTime) {
                    // Account for the fact that we're generating only one
                    // value for the parameter per call. This means the first
                    // call should have a fractional time of 0 and the last 
                    // one should have a fractional time of 1. We can make
                    // that happen if we assume that t2-t1 stays constant.
                    //
                    // The ideal behaviour would be to generate two values
                    // for each call and have the audio engine interpolate
                    // between them. The technique below serves as a stop-gap
                    // arrangement until then.
                    var dt = endTime - startTime - (t2 - t1);
                    if (dt > 0.001) {
                        param.value = afunc((t1 - startTime) / dt);
                    } else {
                        // If we're generating only one value because the
                        // animation duration is very short, make it 
                        // the final value.
                        param.value = afunc(1);
                    }
                });
            }
        }

        // Changes the rate of progress of time through delays.  The given rate
        // "r" can be anything that supports the valueOf() protocol. The rate
        // value/parameter will flow along with the clock object that arrives
        // at this point - meaning it will affect all events that occur
        // sequenced with the rate control action. Note that fork() and spawn()
        // copy the clock before propagating. This means that each track within
        // a spawn/fork can have its own rate setting and that won't interfere
        // with the others. However, if you set a rate in the track that
        // contains the fork/spawn (and before them), the rate setting will
        // propagate to all the forked tracks by virtue of the clock copy.
        //
        // You need to be aware of whether the rate is being propagated "by
        // reference" or "by value". If the rate is a parameter, it gets
        // propagated by reference - i.e. changing the *value* of the rate
        // parameter in one track (clock.rate.value = num) affects the rate of
        // all the tracks that share the rate parameter. If it is a simple
        // number, then it gets propagated by value - i.e. "clock.rate = num" in
        // one track won't change the rate for the other tracks.
        function rate(r) {
            return function (sched, clock, next) {
                clock.rate = r;
                sched.perform(next, clock, sched.stop);
            };
        }

        self.audioContext = audioContext;
        self.perform    = perform;
        self.flush      = flush;
        self.play       = play;
        self.stop       = stop;
        self.cont       = cont;
        self.delay      = delay;
        self.loop       = loop;
        self.fork       = fork;
        self.spawn      = spawn;
        self.dynamic    = dynamic;
        self.track      = track;
        self.trackR     = trackR;
        self.fire       = fire;
        self.log        = log;
        self.anim       = anim;
        self.rate       = rate;

        return self;
    }

    return Scheduler;

});

/**
 * org.anclab.steller.timer
 * ------------------------
 *
 * A simple class with two methods - start() and stop().
 * The given callback is called periodically. I wrote
 * this class because setInterval() has **significantly* better
 * callback regularity than setTimeout in node.js. You can,
 * however, use this in a browser as well as in node.js as it
 * can check for a browser environment and adapt the precision
 * to the necessary level. Note that the *minimum* precision
 * in a browser environment will be that of requestAnimationFrame
 * if that API exists. Otherwise the callback will be called
 * at least once every 33 ms.
 *
 * Here are a couple of measurements (in ms) for N callbacks 
 * with dt interval for setInterval under node.js -
 *      {"N":1500,"dt":10,"mean":0.13,"min":-1,"max":1,"deviation":0.34}
 *      {"N":1500,"dt":10,"mean":-0.84,"min":-2,"max":0,"deviation":0.37}
 * Here are two measurements for setTimeout under node.js -
 *      {"N":1500,"dt":10,"mean":-850.31,"min":-1680,"max":-3,"deviation":486.16}
 *      {"N":1500,"dt":10,"mean":-833.59,"min":-1676,"max":0,"deviation":479.3}
 *
 * There is no such difference between the two in the browser, so
 * we always latch on to requestAnimationFrame if found. Here is 
 * a measurement of setInterval in the browser (Chrome) - 
 *      {"N":1500,"dt":10,"mean":-687.63,"min":-1381,"max":-1,"deviation":402.51}
 */
package('org.anclab.steller.timer', ['#global'], function (window) {

    function PeriodicTimer(callback, precision_ms) {
        var requestAnimationFrame = (window.requestAnimationFrame 
            || window.mozRequestAnimationFrame 
            || window.webkitRequestAnimationFrame 
            || window.msRequestAnimationFrame);

        var self = this;
        var running = false;
        var intervalID;

        if (precision_ms === undefined) {
            precision_ms = 15; // Default to about 60fps just like requestAnimationFrame.
        } else {
            // If we're in a browser environment, no point trying to use
            // setInterval based code because the performance is as bad
            // as with setTimeout anyway -
            //      {"N":1500,"dt":10,"mean":-687.63,"min":-1381,"max":-1,"deviation":402.51}
            precision_ms = Math.min(Math.max(window.document ? 15 : 1, precision_ms), 33);
        }

        if (requestAnimationFrame && precision_ms >= 12) {
            self.start = function () {
                if (!running) {
                    running = true;
                    requestAnimationFrame(function () {
                        if (running) {
                            callback();
                            requestAnimationFrame(arguments.callee);
                        }
                    });
                }
            };

            self.stop = function () {
                running = false;
            };
        } else {
            self.start = function () {
                if (!running) {
                    running = true;
                    intervalID = setInterval(callback, 1);
                }
            };

            self.stop = function () {
                if (running) {
                    running = false;
                    clearInterval(intervalID);
                    intervalID = undefined;
                }
            };
        }

        self.__defineGetter__('running', function () { return running; });
        self.__defineSetter__('running', function (state) {
            if (state) {
                self.start();
            } else {
                self.stop();
            }
            return running;
        });

        if (precision_ms <= 5) {
            console.error("WARNING: High precision timing used. May impact performance.");
        }
    }

    return PeriodicTimer;
});

/**
 * A simple clock type that can keep track of absolute time
 * as well as a rate-integrated relative time.
 *
 * [t1,t2] is the absolute time interval,
 * [t1r,t2r] is the rate integrated time interval,
 * dt is the absolute time step for scheduler tick. 'dt' is
 * expected to remain a constant.
 *
 * The 'rate' property can be anything that supports
 * the 'valueOf()' protocol.
 */
package('org.anclab.steller.clock', function () {
    function Clock(t, tr, dt, rate) {
        this.dt = dt;
        this.t1 = t;
        this.t2 = t + dt;
        this.t1r = tr;
        this.t2r = tr + rate.valueOf() * dt;
        this.rate = rate;

        this.data = null; 
        // Arbitrary data slot for use by scheduler tasks.  Each "track"
        // inherits this "data" field from the track that spawned/forked it.
        // The field is copied via prototypal inheritance using
        // Object.create(), so each track can treat "data" as though it owns it
        // and add and change properties. However, note that the "virtual copy"
        // isn't a deep copy, so modifying an object held in the data object
        // (ex: data.arr[3]) is likely to affect all tracks that can access
        // that object. You can override how data is copied by overriding a
        // clock's copy() method.
        
        return this;
    }

    // A function for rounding time in seconds up to millisecond precision.
    function ms(t) {
        return Math.round(t * 1000) / 1000;
    }

    // Convenience method to show the state of a clock object.
    Clock.prototype.toString = function () {
        return JSON.stringify([this.t1r, this.t2r - this.t1r, this.t1, this.t2 - this.t1].map(ms));
    };

    // Makes a copy such that the absolute and rate-integrated
    // times both match and the "data" field is "inherited".
    Clock.prototype.copy = function () {
        var c = new Clock(this.t1, this.t1r, this.dt, this.rate);
        if (this.data) {
            c.data = Object.create(this.data);
        }
        return c;
    };

    // Advances the absolute time interval by dt and the rate-integrated
    // one by dt * rate.
    Clock.prototype.advance = function (dt) {
        var dtr = dt * this.rate.valueOf();
        this.t1 += dt;
        this.t2 += dt;
        this.t1r += dtr;
        this.t2r += dtr;
        return this;
    };

    // Makes one scheduler time step. This just means that t1 takes
    // on the value of t2 and t2 correspondingly increments by a
    // tick interval. Similarly for the rate-integrated interval.
    Clock.prototype.tick = function () {
        this.t1 = this.t2;
        this.t2 += this.dt;
        this.t1r = this.t2r;
        this.t2r += this.dt * this.rate.valueOf();
        return this;
    };

    // Jumps the absolute time to the given time and adjusts
    // the rate-integrated value according to the jump difference.
    Clock.prototype.jumpTo = function (t) {
        var step_dt = t - this.t1;
        var step_dtr = step_dt * this.rate.valueOf();
        this.t1 += step_dt;
        this.t2 += step_dt;
        this.t1r += step_dtr;
        this.t2r += step_dtr;
        return this;
    };

    // Nudges the rate-integrated "relative" time to the given value.
    // The absolute start time is also adjusted proportionally.
    //
    // WARNING: This needs t2r > t1r to hold.
    Clock.prototype.nudgeToRel = function (tr) {
        tr = Math.max(this.t1r, tr);
        if (this.t2r > this.t1r) {
            this.t1 += (tr - this.t1r) * (this.t2 - this.t1) / (this.t2r - this.t1r);
        }
        this.t1r = tr;
        return this;
    };

    return Clock;
});
