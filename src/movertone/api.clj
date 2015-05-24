(ns movertone.api
  (:require [ring.adapter.jetty :as jetty]
            [cider.nrepl :as cider]
            [clojure.tools.nrepl.server :as nrepl]
            [bidi.ring :refer (make-handler)]
            [ring.util.http-response :refer [ok content-type header] :as resp]
            [movertone.print :as p]))

(defn html-response [html]
  (-> html
      resp/ok
      (resp/content-type "text/html; charset=UTF-8")))

(defn index [request]
  (prn "here")
  (html-response "Hello world html not here"))

(defn melakarthas-index [request]
  (-> (p/melakartha-rows)
      p/html-rows
      p/make-html
      html-response))

(def routes ["/" {"" index
                  "index.html" index
                  "melakarthas/" {"" melakarthas-index
                                  [:id "/article.html"] :article}}])

(def handler
  (make-handler routes))

(defn start! [port nrepl-port]
  (nrepl/start-server :port nrepl-port :handler cider/cider-nrepl-handler)
  (jetty/run-jetty handler {:port port}))
