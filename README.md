# Ragavardhini

Not sure what this is meant to do yet.

## Usage

```clojure

  (:ragavardhini d/melakarthas)
   > {:arohanam [:s :r3 :g3 :m1 :p :d1 :n2 :s.], :avarohanam (:s. :n2 :d1 :p :m1 :g3 :r3 :s)}

  (play-arohanam-and-avarohanam (:hanumatodi d/melakarthas))

  (play-arohanam-and-avarohanam (:vasanta d/janyas))

  (play-phrase (phrase [:s :r2 :g3 :p :m1 :g3 :r2 :s]
                       [ 1   1  1  1   1   1   2   4]
                       1))

  (play-phrase
   (phrase (:mechakalyani d/melakarthas)
           [:m :d :n :g :m :d :r :g :m  :g :m :d :n :s.]
           [ 1  1  2  1  1  2  1  1  4   1  1  1  1  4]
           2))

  (play-string (:bilahari d/janyas)
               "s,,r g,p, d,s., n,d, p,dp mgrs rs .n .d s,,,
                s,,r g,p, m,,g p,d, r.,,s. n,d, p,,m g,r,")

  (play-file (:mohana d/janyas)
             "mohana-varnam.txt")
```

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
