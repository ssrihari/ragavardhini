package('srikumarks.phd.raga.sahana.meta', 
{
    "adi_kalai2": {
        "type": "tala",
        "structure": [4,2,2],
        "beats_per_count": 2,
        "pulses_per_count": 4,
        "tempo_bpm": 70
    },

    "adi_kalai1": {
        "type": "tala",
        "structure": [4,2,2],
        "beats_per_count": 1,
        "pulses_per_count": 4,
        "tempo_bpm": 70
    },

    "contents": ["pallavi", "anupallavi", "muktayisvaram", 
                "ending_before_caranam",
                "caranam",
                "cittasvaram"],

    "pallavi": {
        "tala": "adi_kalai2",
        "contents": ["line1", "line2"],
        "line1": {
            "lyrics": [["ka:2", "ru:2", "Nim:8", "pa:4"], 
                     ["i:4", "di:4", "man:5", "ci:3"]],
            "presc": [
                ["pa:2", "ma1:2", "ga3:2", "ga3", "ma1", "ri2:2", 
                 "ga3", "ri2", "sa:4"],
                ["ni2-", "sa", "ri2", "ga3", "ri2", "sa", "ni-", 
                 "sa", "da2-", "pa-", "ma1-", "da2-:2", "ni2-", "sa", "ri2"]
                    ]
        },
        "line2": {
            "lyrics": [["ta:7", "ru:7", "Na:5", "mu:3"], ["sa:7", "mi:3"]],
            "presc": [
                ["pa", "ma1", "ga3", "ma1", "ri2", "ga3", "ri2", "sa:3"],
                ["ri2", "ga3", "ma1", "pa", "ma1"],
                ["da2:2", "ni2:2", "sa+", "pa:3"],
                ["ri2", "ga3", "ma1"],
                ["da2", "pa", "pa", "ma1", "ga3", "ma1", "ri2"]
                    ]
        }
    },

    "anupallavi": {
        "tala": "adi_kalai2",
        "contents": ["line1", "line2"],
        "line1": {
            "lyrics": [["pa:3", "ru:2", "la:5"], ["ve:3", "da:3", "le:3", "nu:11"], ["na:2"]],
            "presc": [
                ["da2", "ni2", "da2", "da2", "pa"], 
                ["pa", "da2", "pa", "pa", "ma1"],
                ["da2", "pa", "ma1", "ga3", "ma1", "ri2", "ga3", "ma1", "pa"],
                ["ni2-", "sa", "ri2", "ga3", "ma1"],
                ["pa:2", "ma1:2", "da2", "ni2", "sa+:2"]
                ]
        },
        "line2": {
            "lyrics": [["pa:3", "li:3"], ["sri:3", "ve:4", "Nu:3"], ["go:3", "pa:3", "la:3"], ["de:4", "va:3"]],
            "presc": [
                ["da2", "ni2", "sa+", "ri2+:2", "ri2+"],
                ["ni2", "sa+", "ri2+"],
                ["ga3+", "ma1+", "ri2+:2", "ga3+", "ri2+", "sa+"],
                ["ri2+", "ni2:2", "sa+", "da2:2", "ni2", "da2", "pa"],
                ["da2", "pa", "pa", "ma1", "ga3", "ma1", "ri2"]
                    ]
        }
    },

    "muktayisvaram": {
        "tala": "adi_kalai2",
        "contents": ["part1", "part2"],
        "part1": {
            "presc": [
                ["pa:3"], ["pa", "ma1", "ga3", "ma1:3"], 
                ["ri2", "ga3", "ma1", "pa", "ma1", "ga3", "ma1"],
                ["ri2:3", "ga3", "ri2", "sa", "ni2-", "sa"],
                ["ri2", "ga3", "ma1", "pa", "ma1", "da2", "ni2", "sa+"]
                    ]
        },
        "part2": {
            "presc": [
                ["pa:2"], ["da2", "ni2", "sa+", "ni2", "sa+", "ri2+"],
                ["ga3+", "ma1+", "ri2+:2", "ga3+", "ri2", "sa+"],
                ["ni2", "sa+", "ri2+", "ni2", "sa+:2"],
                ["sa+", "pa", "da2", "ma1", "pa:2"],
                ["ni2-", "sa", "ri2", "ga3", "ma1"]
                    ]
        }
    },

    "ending_before_caranam": {
        "tala": "adi_kalai2",
        "contents": ["ending"],
        "ending": {
            "lyrics": [["ka:2", "ru:2", "Nim:8", "pa:4"]],
            "presc": [
                ["pa:2", "ma1:2", "ga3:2", "ga3", "ma1", "ri2:2", "ga3", "ri2", "sa:4"]
            ]
        }
    },

    "caranam": {
        "tala": "adi_kalai1",
        "contents": ["line1", "line2"],
        "line1": {
            "lyrics": [["kr:2", "pa:4"], ["ju:6", "du:2", "mi:6"]],
            "presc": [
                ["da2:2", "da2:4", "ni2:4", "da2:4", 
                 "ni2", "da2", "pa:6"]
                ]
        },
        "line2": {
            "lyrics": [["i:3"], ["ve:3", "la:4"]],
            "presc": [
                ["da2", "pa", "ma1", "ga3", "ma1", "ri2", 
                 "ga3", "ma1", "pa", "ma1"]
                ]
        }
    },

    "cittasvaram": {
        "tala": "adi_kalai1",
        "contents": ["c1", "c2", "c3", "c4"],
        "c1": {
            "presc": [
                ["pa:6", "ma1:4"], 
                ["da2:2", "pa:2", "ma1:2"], 
                ["ga3:3", "ma1:3", "ri2:3"],
                ["ga3:2", "ma1:2", "pa:2", "ma1"]
                    ]
        },
        "c2": {
            "presc": [
                ["da2:2", "pa", "ma1", "ga3", "ma1", "ri2:2"],
                ["ga3", "ma1", "pa", "ni2-", "sa", "ri2", "ga3", "ma1"],
                ["pa:2", "ma1", "ga3:2", "ma1", "ri2:2"],
                ["ga3", "ri2", "sa", "ri2", "ga3", "ma1", "pa", "ma1"]
                    ]
        },
        "c3": {
            "presc": [
                ["pa", "da2", "ma1", "pa", "ga3", "ma1"], 
                ["ri2", "ga3", "ma1", "pa"], 
                ["ma1", "ga3", "ma1", "ri2", "ga3", "ri2"],
                ["sa", "ri2", "ni2-", "sa", "da2-", "ni2-", "sa", "ri2", "ga3", "ma1"], 
                ["sa", "ri2", "ga3", "ma1", "pa", "da2", "pa", "ma1"],
                ["da2", "ni2", "sa+", "ri2+", "sa+", "ri2+", "ni2+", "sa+"],
                ["ri2+", "ga3+", "ma1+", "ri2+", "ga3+", "ri2+", "sa+", "ri2+"],
                ["ni2", "ri2+", "sa+"], 
                ["da2", "ni2", "sa+"],
                ["da2", "ni2", "da2"],
                ["pa", "da2", "ma1", "pa", "ma1"]
                ]
        },
        "c4": {
            "presc": [
                ["ni2:3", "sa+", "ri2+", "sa+"],
                ["ni2", "ri2+", "sa+", "ni2", "sa+", "ni2"],
                ["da2:2", "ni2", "da2"],
                ["pa:3", "ri2:3"],
                ["ga3", "ma1", "pa", "ma1", "ga3", "ma1", "ri2", "ga3", "ri2", "sa"],
                ["ri2", "ga3", "ma1", "pa:2", "pa"],
                ["ma1", "pa", "ma1", "da2:2", "da2"],
                ["pa", "da2", "ni2", "da2"],
                ["pa", "da2", "ma1", "pa", "ga3", "ma1", "ri2", "ga3", "ri2", "sa:2"],
                ["ri2", "ga3", "ma1", "pa:2"],
                ["pa", "ma1", "ga3", "ma1", "ri2:2"],
                ["da2", "pa", "ma1", "ga3", "ma1", "ri2:2"],
                ["ga3", "ma1", "pa"],
                ["ni2-", "sa:2", "ri2", "ga3", "ma1", "pa", "ma1"],
                ["da2:2", "ni2", "sa+", "ri2+:2", "ri2+:2"],
                ["ga3+", "ma1+", "ri2+", "ga3+", "ri2+", "sa+"],
                ["ni2", "ri2+", "sa+", "ni2", "sa+", "da2+", "pa", "ma1", "da2", "ni2"],
                ["sa+", "sa+:2"],
                ["pa", "pa:2"],
                ["ri2", "ri2:2"],
                ["ni2-", "sa", "ri2", "ga3", "ma1", "pa", "ma1"]
                ]
        }
    }
});
