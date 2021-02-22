(ns ggsoft.git.commands.recorded-version
  (:require [ggsoft.git.commands.multi :refer [perform-command]]
            [clojure.java.io :as io])
  (:import [java.io File]))


(defn recorded-version []
  (let [f (File. ".VERSION")]
    (if-not (.exists f)
      (do
        (println  "version file does not exist")
        (throw (ex-info "version file does not exist" {})))
      (slurp f))))


(defmethod perform-command :recorded-version
  [_]
  (let [v (recorded-version)]
    (println v)
    v))
