(ns movertone.main
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [cli]]
            [movertone.api :as api]))

(defn -main [& args]
  (let [[{:keys [port nrepl-port] :as args}
         _args
         _usage]
        (cli args
             ["-p"    "--port" "Port"
              :default 3000 :parse-fn #(Long/parseLong %)]
             ["-np"   "--nrepl-port" "nREPL port"
              :default 3001 :parse-fn #(Long/parseLong %)])
        [task-group task] _args]
    (api/start! port nrepl-port)))

(apply -main *command-line-args*)
