package('srikumarks.phd.pasr-graph', function () {

var exports = this;

var nextFrame = (window.requestAnimationFrame 
        || window.webkitRequestAnimationFrame 
        || window.mozRequestAnimationFrame
        || (function (f) { return setTimeout(f, 1000/60); }));

function non_null(thing, descr) {
    if (thing === null || thing === undefined) {
        throw new Error('Bad ' + descr);
    }

    return thing;
}

function pasrDurAcc(pasr, t) {
    var acc = [{start: t, pitch: pasr[0].pitch, attack: t, sustain: t, release: t}];
    var i, N, p;

    for (i = 0, N = pasr.length; i < N; ++i) {
        t = acc[acc.length - 1].release;
        p = pasr[i];
        acc.push({
            start: t,
            pitch: p.pitch,
            attack: t + p.attack,
            sustain: t + p.attack + p.sustain,
            release: t + p.attack + p.sustain + p.release
        });
    }

    acc.startTime = acc[0].start;
    acc.endTime = acc[acc.length - 1].release;
    acc.duration = acc.endTime - acc.startTime;
    return acc;
}

function setup(canvasID, plucks, tempo_bpm, style) {
    var canvas = non_null(document.getElementById(canvasID), "canvas ID");
    var ctxt = canvas.getContext('2d');
    var tref = plucks[0].time_secs;
    var timeScale = 60 / tempo_bpm;

    var view = { 
        left:   { x: 0.05, t: 0},
        right:  { x: 0.95, t: 2 },
        bottom: { y: 0.95, p: -5 },
        top:    { y: 0.05, p: 17 },
        xscale: 1,
        yscale: 1,

        update: function () {
            this.xscale = (this.right.x - this.left.x) / (this.right.t - this.left.t);
            this.yscale = (this.top.y - this.bottom.y) / (this.top.p - this.bottom.p);
            return this;
        },

        t2x: function (t) {
            return (canvas.width - 1) * (this.left.x + (t - this.left.t) * this.xscale);
        },

        x2t: function (x) {
            return this.left.t + ((x / (canvas.width - 1)) - this.left.x) / this.xscale;
        },

        p2y: function (p) {
            return (canvas.height - 1) * (this.bottom.y + (p - this.bottom.p) * this.yscale);
        },

        y2p: function (y) {
            return this.bottom.p + ((y / (canvas.height - 1)) - this.bottom.y) / this.yscale;
        }
    };

    var pasrDur = plucks.reduce(function (acc, p) { return acc + p.duration_secs; }, 0.0);
    view.right.t = pasrDur;
    view.update();

    var kPitchNames = ['S', 'r', 'R', 'g', 'G', 'm', 'M', 'P', 'd', 'D', 'n', 'N'];

    function clearCanvas(ctxt) {
        ctxt.save();
        ctxt.fillStyle = 'white';
        ctxt.clearRect(0, 0, canvas.width, canvas.height);
        ctxt.restore();
    }

    function drawGrid(ctxt) {
        ctxt.save();
        ctxt.lineWidth = 0.5;
        ctxt.strokeStyle = 'rgb(192,192,192)';
        ctxt.globalAlpha = 0.5;

        var p, t;
        ctxt.beginPath();
        for (p = Math.ceil(view.bottom.p); p < view.top.p; p += 1.0) {
            ctxt.moveTo(view.t2x(view.left.t), view.p2y(p));
            ctxt.lineTo(view.t2x(view.right.t), view.p2y(p));
        }
        ctxt.stroke();

        for (p = Math.ceil(view.bottom.p); p < view.top.p; p += 1.0) {
            ctxt.fillText(kPitchNames[((p % 12) + 12) % 12], view.t2x(view.left.t) - 16, view.p2y(p)+3);
        }

        ctxt.beginPath();
        for (t = Math.ceil(view.left.t * timeScale * 4) / (4 * timeScale); t < view.right.t; t += 0.25 * timeScale) {
            ctxt.moveTo(view.t2x(t), view.p2y(view.bottom.p));
            ctxt.lineTo(view.t2x(t), view.p2y(view.top.p));
        }
        ctxt.stroke();

        ctxt.lineWidth = 2.0;
        ctxt.beginPath();
        for (t = Math.ceil(view.left.t * timeScale * 4) / (4 * timeScale); t < view.right.t; t += 1.0 * timeScale) {
            ctxt.moveTo(view.t2x(t), view.p2y(view.bottom.p));
            ctxt.lineTo(view.t2x(t), view.p2y(view.top.p));
        }
        ctxt.stroke();

        ctxt.restore();
    }

    function drawPluck(pluck) {
        var t;
        var tpref = pluck.time_secs - tref;
        var t0 = Math.max(pluck.time_secs - tref, Math.min(view.left.t, pluck.end_secs - tref));
        var t1 = Math.max(pluck.time_secs - tref, Math.min(view.right.t, pluck.end_secs - tref));
        var dt = 1.0 / ((canvas.width - 1) * view.xscale);
        var i, N;
        var fatness = 0.25;

        var lineWidth = (canvas.height - 1) * view.yscale / 10;
        var curve = pluck.pitch_curve;

        var x, y, p, pPlus, pMinus, pvU, pvL, dpdt, path = [];
        for (t = t0, i = 1; t < t1;) {
            x = view.t2x(t);
            p = curve(t - tpref);
            pPlus = curve(t + 0.5 * dt - tpref);
            pMinus = curve(t - 0.5 * dt - tpref);
            if (Math.abs(pPlus - pMinus) < 0.4) {
                dpdt = (pPlus - pMinus) / dt;
                pvU = p + fatness * (1/120) * dpdt;
                pvL = p - fatness * (1/120) * dpdt;
            } else {
                debugger;
                // Ensure we handle discontinuities nicely.
                pvU = pvL = p;
            }
            path.push({
                x: x,
                y_above: view.p2y(pvU),
                y_below: view.p2y(pvL)
            });
            t += dt;
        }


        ctxt.save();
        ctxt.fillStyle = 'black';
        ctxt.lineWidth = lineWidth;

        ctxt.beginPath();
        ctxt.arc(view.t2x(t0), view.p2y(curve(t0 - tpref)), 4, 0, 2 * Math.PI);
        ctxt.fill();

        ctxt.beginPath(); 
        ctxt.moveTo(view.t2x(t0), view.p2y(curve(t0 - tpref)));
        for (i = 0, N = path.length; i < N; ++i) {
            ctxt.lineTo(path[i].x, path[i].y_below);
        }
        for (i = path.length - 1; i >= 0; --i) {
            ctxt.lineTo(path[i].x, path[i].y_above);
        }
        ctxt.stroke();
        ctxt.fill();
        ctxt.restore();
    }

    var pendingDraws = 0;

    function drawOnce() {
        clearCanvas(ctxt);
        drawGrid(ctxt);
        plucks.forEach(drawPluck);
    }

    function drawAsync() {
        if (pendingDraws > 0) {
            drawOnce();
            pendingDraws = 0;
        }
    }

    function draw() {
        pendingDraws++;
        nextFrame(drawAsync);
    }

    var pasrGraph = {
        get view() {
            return {left: view.left, right: view.right, bottom: view.bottom, top: view.top};
        },

        set view(v) {
            view.left.x = v.left ? v.left.x : view.left.x;
            view.left.t = v.left ? v.left.t : view.left.t;
            view.right.x = v.right ? v.right.x : view.right.x;
            view.right.t = v.right ? v.right.t : view.right.t;
            view.bottom.y = v.bottom ? v.bottom.y : view.bottom.y;
            view.bottom.p = v.bottom ? v.bottom.p : view.bottom.p;
            view.top.y = v.top ? v.top.y : view.top.y;
            view.top.p = v.top ? v.top.p : view.top.p;

            view.update();
            draw();
            return v;
        },

        pitch_time: function (x, y) {
            return {pitch: view.y2p(y), time: view.x2t(x)};
        },

        draw: draw
    };

    canvas.pasr_graph = pasrGraph;

    return canvas;
};

exports.setup = setup;

return exports;
});
