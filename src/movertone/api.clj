(ns movertone.api
  (:require [ring.adapter.jetty :as jetty]
            [cider.nrepl :as cider]
            [clojure.tools.nrepl.server :as nrepl]
            [bidi.ring :as br]
            [movertone.print :as p]
            [movertone.ragams :as r]))

(defn html-response [html]
  {:status 200
   :body html
   :content-type "text/html"})

(defn index [request]
  {:status 200
   :body "muhaha!"})

(defn melakarthas-index [request]
  (-> (p/melakartha-rows)
      p/html-rows
      p/make-html
      html-response))

(defn all [request]
  (-> (p/html-rows)
      p/make-html
      html-response))

(defn show-ragam [{:keys [params] :as request}]
  (let [ragam-name (keyword (:name params))
        ragam {ragam-name (r/ragams ragam-name)}]
    (->> [(p/row false ragam)]
         p/html-rows
         p/make-html
         html-response)))

(defn search [{:keys [params] :as request}]
  (if-let [search-result (r/search (:query params))]
    (html-response (p/search-result-html search-result))
    "Sorry, no such ragam."))

(def routes ["/" {"" index
                  "all" all
                  "melakarthas/" {"" melakarthas-index
                                  [:name] show-ragam}
                  "ragams/" {"" all
                             [:name] show-ragam}
                  "search/" {[:query] search}}])

(def handler
  (br/make-handler routes))

(defn start! [port nrepl-port]
  (nrepl/start-server :port nrepl-port :handler cider/cider-nrepl-handler)
  (jetty/run-jetty handler {:port port}))
