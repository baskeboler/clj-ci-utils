(ns ggsoft.git.commands.update
  (:require [ggsoft.git.commands.multi :refer [perform-command]]
            [ggsoft.git.commands.recorded-version :refer [recorded-version]]
            [ggsoft.git.repo :as grepo]
            [clojure.tools.logging :refer [info error debug warn]]
            [clojure.string :as cstr])) 

(defn update-version-in-file
  "replaces old version string with new one in
   provided file"
  [file-name old-version new-version]
  (let [contents (slurp file-name)
        updated (cstr/replace
                 contents
                 old-version
                 new-version)]
    (spit file-name updated)
    (spit ".VERSION" updated)))

(defmethod perform-command :update
  [{:keys [options] :as arg}]
  (info "running update")
  (let [{:keys [file] } options
        current         (grepo/current-version (grepo/default-git))
        recorded        (recorded-version)]
    (update-version-in-file file recorded current)))
