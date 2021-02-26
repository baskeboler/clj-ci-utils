(ns ggsoft.git.commands.get-versions
  (:require [clojure.string :as cstr]
            [ggsoft.git.commands.multi :refer [perform-command]]))
(def rx #"(\d+(\.\d+){2})(\-\d+)(\-[\w\d]{8})")

(defn get-versions [f]
  (let [s (slurp f)
        matches (re-seq rx s)]
    (println (cstr/join \newline (map first matches)))))

(defmethod perform-command :get-versions
  [{:keys [options] :as arg}]
  (let [{:keys [file]} options]
    (get-versions file)))
