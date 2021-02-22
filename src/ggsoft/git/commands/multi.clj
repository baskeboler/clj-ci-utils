(ns ggsoft.git.commands.multi)
  ;; (:require [clojure.tools.logging :refer [info error warn]]))
(defmulti  perform-command
  (fn [command-spec]
    (:command command-spec)))
  
(defmethod perform-command
  :default
  [_]
  (println "no method defined for " _))  
