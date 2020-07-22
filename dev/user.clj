(ns user
  (:require
   [nrepl.server :as nrepl]))


(println (str "Starting nREPL server at localhost:" 7001))
(nrepl/start-server :port 7001)
(spit ".nrepl-port" "7001")