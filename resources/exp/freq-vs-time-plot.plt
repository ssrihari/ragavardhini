set term png
set output filename.'.png'

load 'dark2.pal'

# xyborder.cfg
set style line 101 lc rgb '#808080' lt 1 lw 1
set border 3 front ls 101
set tics nomirror out scale 0.75
set format '%g'

# noborder.cfg
set border 0
set style line 101 lc rgb '#808080' lt 1 lw 1
unset xlabel
unset ylabel
set format x ''
set format y ''
set tics scale 0

plot filename with lines