(ns ggsoft.git.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as cstr]
            [fipp.edn :refer [pprint] :rename {pprint fipp}]
            [ggsoft.git.commands.core :refer :all]
            [ggsoft.git.commands.multi :refer [perform-command]]
            [ggsoft.git.repo :refer [default-git current-version]]
            [clojure.edn]
            [unilog.config :refer [start-logging!]]
            [clojure.tools.logging :refer [log info debug error warn]])

  (:import java.io.File
           org.eclipse.jgit.api.Git
           org.eclipse.jgit.lib.Ref
           org.eclipse.jgit.lib.Repository
           org.eclipse.jgit.storage.file.FileRepositoryBuilder))

(set! *warn-on-reflection* true)

(declare set-git-dir)

(def logging-conf
  {:level     "all"
   :console   true
   :files     ["program.log"
               {:name    "program-json.log"
                :encoder "json"}]
   :overrides {}})


(defn init-logging! []
  (start-logging! logging-conf))



(defn valid-base-version-string? [v]
  (-> (re-matches #"\d+\.\d+\.\d+" v)
      some?))

(defrecord Version [major minor revision])

(defn inc-version [v]
  (-> v
      (update :revision inc)))

(defn parse-version [ver]
  (let [[major minor revision] (map #(Integer/parseInt %)
                                    (cstr/split ver #"\."))]
    (->Version major minor revision)))

(defn create-version-file []
  (spit ".VERSION" (current-version  (default-git))))

(defn update-version-in-file
  "replaces old version string with new one in
   provided file"
  [file-name old-version new-version]
  (let [contents (slurp file-name)
        updated (cstr/replace
                 contents
                 old-version
                 new-version)]
    (spit file-name updated)))

(def cli-opts
  [["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-f" "--file PATH" "Input file path"
    :default nil]
   ;; A non-idempotent option (:default is applied first)
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc] ; Prior to 0.4.1, you would have to use:
   ;; :assoc-fn (fn [m k _] (update-in m [k] inc))
   ;; A boolean option defaulting to nil
   ["-h" "--help"
    :id :help]])

(defn- handle-errors [errs]
  (println "Error:")
  (doseq [e errs]
    (println e)))

(defn resolve-command [arg]
  (condp = arg
    "recorded-version" :recorded-version
    "current-version"  :current-version
    "bump-version-tag" :bump-version-tag
    "update"           :update
    :unknown-command))

(defn handle-parse-result [res]
  (let [{:keys [arguments errors]} res
        command                                    (resolve-command (first arguments))
        status                                     (if (or (= command :unknown-command)
                                                           (some? errors))
                                                     :error
                                                     :ok)]
    (-> res
        (assoc :command command
               :status status))))

(defn -main [& args]
  (init-logging!)
  (let [{:keys [options arguments summary errors] :as res}
        (-> args
            (parse-opts cli-opts)
            handle-parse-result)]
    (fipp res)
    (cond
      (some? errors)                     (handle-errors errors)
      (some-> options :help some?)       (do
                                           (info "usage: " (first args) " [opts] command")
                                           (info summary))
      (= :ok (-> res :logging :status)) (do
                                          (info "performing command " res)))))
