(ns ggsoft.git.commands.write-properties
  (:require [ggsoft.git.commands.multi :refer [perform-command]]
            [ggsoft.git.repo :as grepo]
            [clojure.edn :as edn]))
(defn write-properties [program-name output-file]
  (let [data {:program-name program-name
              :version (grepo/current-version (grepo/default-git))}]
    (spit output-file (str data))))

(defmethod perform-command :write-properties
  [arg]
  (let [{:keys [options] } arg
        {:keys [file ] :or {file "app.edn"}} options]
    (write-properties "application" file)))
    
         
         
