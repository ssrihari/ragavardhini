#!/bin/bash
export PRAAT_EXEC="/Applications/Praat.app/Contents/MacOS/Praat"
export PRAAT_SCRIPT="ps"
export dx=.01
#from docs: dx = time step = frame length = frame duration, in seconds.

i=$1

echo Processing $i
# Convert mp3 to wav
echo "Converting to wav.."
ffmpeg -i $i "$i.wav" -loglevel 8

# Get pitch info from wav file
echo "Converting to pitch.."
$PRAAT_EXEC --run $PRAAT_SCRIPT "$i.wav" "$i.wav.pitch"

rm "$i.wav"

# Get frequencies data from pitch
echo "Extracting frequency data.."
tail -n +12 $"$i.wav.pitch" | grep -A6 "frame" | grep -ve "--" | grep "frequency" | grep -ve "--" | grep -oE "[0-9\.]+" > "$i.wav.pitch.frequencies"
