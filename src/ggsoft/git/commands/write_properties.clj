(ns ggsoft.git.commands.write-properties
  (:require [ggsoft.git.commands.multi :refer [perform-command]]
            [ggsoft.git.repo :as grepo]
            [clojure.edn :as edn]
            [clojure.data.json :as json]))
(defn write-properties [program-name output-file format]
  (let [data {:program-name program-name
              :version (grepo/current-version (grepo/default-git))}
        content (condp = format
                  :edn (str data)
                  :json (json/write-str data))]

    (spit output-file content)))

(defmethod perform-command :write-properties
  [arg]
  (let [{:keys [options] } arg
        {:keys [file application-name output-format] :or {file "app.edn"}} options]
    (write-properties application-name file output-format)))
    
         
         
