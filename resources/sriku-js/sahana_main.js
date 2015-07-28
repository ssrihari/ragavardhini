
package.config({
    'srikumarks.phd.vina': {path: 'vina.js', alias: 'vina'},
    'srikumarks.phd.db': {path: 'db.js', alias: 'db'},
    'srikumarks.phd.pasr-graph': {path: 'pasr-graph.js', alias: 'pasr-graph'},
    'com.nishabdam.audio.sample-manager': {path: 'sample-manager.js', alias: 'sample-manager'},
    'srikumarks.phd.raga.sahana.db': {path: 'sahana_db.js', alias: 'sahana_db'},
    'srikumarks.phd.raga.sahana.meta': {path: 'sahana_db_meta.js', alias: 'sahana_db_meta'},
    'org.anclab.steller.scheduler': {path: 'scheduler.js', alias: 'scheduler'}
});

package('main', ['vina', 'db', 'pasr-graph', 'sahana_db', 'sahana_db_meta', '#onload'], 
        function main(Vina, DB, PASRGraph) {
            var stageInterp = 'fret_slide';//'glide';
            var danceInterp = 'skew_sine';

            function e(id) { return document.getElementById(id); }

            function report(stat) {
                e('status').insertAdjacentHTML('beforeend', '\n'+stat);
            }

            function onload() {
                Vina.init({
                    didInitializeVina: onVinaReady,
                    onError: function (e) {
                        console.log(e);
                    }
                });
            }

            function invoke(methodName) {
                return function (obj) {
                    return obj[methodName]();
                };
            }

            function flatten(arr) {
                return Array.prototype.concat.apply([], arr);
            }

            var sch;

            function calcStoppage_secs(pluck, i, plucks) {
                // Put the stoppage information in the first note of the pluck.
                // In the stoppage_secs field.
                var lastNote = pluck[pluck.length - 1];
                var sdAbs = Vina.soasrn(lastNote.duration_secs, lastNote.soasr);
                var sEnd = sdAbs.stage[sdAbs.stage.length - 1];
                var dEnd = sdAbs.dance[sdAbs.dance.length - 1];
                var sMaxStop = 0.25 * (sEnd.sustain + sEnd.release);
                var dMaxStop = 0.25 * (dEnd.sustain + dEnd.release);
                var lim = 0.1;

                pluck[0].stoppage_secs = Math.min(lim, Math.min(sMaxStop, dMaxStop));
            }


            function onVinaReady(Vina, sm, sampleSet, audioContext) {
                var vina1 = Vina.create(-1.38, 0.5);

                var sahana = DB.load('sahana');

                var phrasesSection = e('phrases');
                sahana.performance.forEach(function (section, i) {
                    if (section.plucks) {
                        var phraseHeader = document.createElement('h4');
                        var phraseList = document.createElement('ol');
                        phraseHeader.innerText = section.label 
                            + (section.speed !== 1 ? (" (speed " + section.speed + ")") : "");
                        section.pasr.forEach(function (phrase, j) {
                            if (phrase.plucks) {
                                var li = document.createElement('li');
                                var span = document.createElement('span');
                                span.className = 'code';
                                span.insertAdjacentHTML('beforeend', phrase.presc.join(' '));
                                li.insertAdjacentElement('beforeend', span);
                                phraseList.insertAdjacentElement('beforeend', li);

                                phrase.plucks.forEach(calcStoppage_secs);

                                span.onclick = function (e) {
                                    vina1.playPhrase(phrase.plucks, stageInterp, danceInterp, 0.01);
                                };
                            }
                        });
                        phrasesSection.insertAdjacentElement('beforeend', phraseHeader);
                        phrasesSection.insertAdjacentElement('beforeend', phraseList);
                    }
                });

                var special = e('special');
                sahana.performance.forEach(function (section, i) {
                    if (section.special) {
                        var phraseHeader = document.createElement('h4');
                        var phraseList = document.createElement('ol');
                        phraseHeader.innerText = section.text
                            + (section.speed !== 1 ? (" (speed " + section.speed + ")") : "");
                        section.pasr.forEach(function (phrase, j) {
                            if (phrase.plucks) {
                                var li = document.createElement('li');
                                var span = document.createElement('span');
                                span.className = 'code';
                                span.insertAdjacentHTML('beforeend', phrase.presc.join(' '));
                                li.insertAdjacentElement('beforeend', span);
                                phraseList.insertAdjacentElement('beforeend', li);

                                phrase.plucks.forEach(calcStoppage_secs);

                                span.onclick = function (e) {
                                    vina1.playPhrase(phrase.plucks, stageInterp, danceInterp, 0.01);
                                };
                            }
                        });
                        special.insertAdjacentElement('beforeend', phraseHeader);
                        special.insertAdjacentElement('beforeend', phraseList);
                    }
                });
                /*
                var sel = sahana.performance.filter(function (section) {
                    return section.label.indexOf('pallavi.') === 0;
                });
                var plucks = flatten(sel.map(function (section) { 
                    return flatten(section.pasr.map(function (phrase) { 
                        return phrase.plucks;
                    }));
                }));
                var span = document.createElement('span');
                span.className = 'code';
                span.insertAdjacentHTML('beforeend', 'Pallavi');
                span.onclick = function (e) {
                    vina1.playPhrase(plucks, stageInterp, danceInterp, 0.01);
                };
                special.insertAdjacentElement('beforeend', span);
                */

                report('db length = ' + Object.keys(sahana.pitch_contexts).length);
                //vina1.pluck(Vina.pasre(1.0, db['ma1,ga3,ga3'].phrases[0], 0.0, 'sine'), Vina.now_secs).play();
                // vina1.pluck(Vina.pasre(1.0, [[0.0, 1, 1, 0], [1.0, 2, 1, 0], [0.0, 2, 1, 0], [1.0, 2, 1, 1]], 0.0, 'skew_sine'), Vina.now_secs).play();

                //                        vina1.pluck(Vina.bsasr(1, {b: [[2,0,0,0], [0,2,4,0]], s: [[2,0,2,4], [7,0,2,0]]}, 0.1, 'skew_sine'), Vina.now_secs).play();
                //                        vina1.pluck(Vina.pasr(Vina.pasrn(1.0, [[2, 0,0,0], [0,2,6,0]]), 0.1, 'sine'), Vina.now_secs);

            }

            onload();
        });
