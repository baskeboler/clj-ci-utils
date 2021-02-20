(ns ggsoft.git.commands.recorded-version
  (:require [clojure.tools.logging :refer [info warn debug error]]
            [ggsoft.git.commands.multi :refer [perform-command]]
            [clojure.java.io :as io])
  (:import [java.io File]))


(defn recorded-version []
  (let [f (File. ".VERSION")]
    (if-not (.exists f)
      (do
        (error  "version file does not exist")
        (throw (ex-info "version file does not exist" {})))
      (slurp f))))


(defmethod perform-command :recorded-version
  [_]
  (let [v (recorded-version)]
    (info "Recorded version: " v)
    v))
