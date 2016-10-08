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

# Remove the wav file
# rm "$i.wav"

# Get frequencies data from pitch
echo "Extracting frequency data.."
tail -n +12 $"$i.wav.pitch" | grep -A6 "frame" | grep -ve "--" | grep "frequency" | grep -ve "--" | grep -oE "[0-9\.]+" > "$i.wav.pitch.frequencies"

# Plot freq vs time
echo "Plotting t vs f.."
awk -v dx="$dx" '{x = NR * dx; print x, $0}' "$i.wav.pitch.frequencies" | grep -vE " 0$" > "$i.wav.pitch.frequencies.time-freq-data"
gnuplot -e "filename='$(echo "$i.wav.pitch.frequencies.time-freq-data")'" plot.plt


# Plot freq vs count
echo "Plotting f vs c.."
cat "$i.wav.pitch.frequencies" | grep -oE "^[0-9]+" | egrep -v "^0" | sort -nrk1 | uniq -c | awk '{print $2, $1}' > "$i.wav.pitch.frequencies.freq-count-data"
gnuplot -e "filename='$(echo "$i.wav.pitch.frequencies.freq-count-data")'" freq-plot.plt
