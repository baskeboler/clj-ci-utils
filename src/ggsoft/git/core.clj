(ns ggsoft.git.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as cstr]
            [fipp.edn :as f :refer [pprint] :rename {pprint fipp}]
            [ggsoft.git.commands.core :refer :all]
            [ggsoft.git.commands.multi :refer [perform-command]]
            [ggsoft.git.repo :refer [default-git current-version]]
            [clojure.edn]
            [unilog.config :refer [start-logging!]]
            [clojure.tools.logging :refer [log trace info debug error warn]]
            [clojure.set :as sets])
  (:import (java.io File)
           (org.eclipse.jgit.api Git)
           (org.eclipse.jgit.lib Ref Repository)
           (org.eclipse.jgit.storage.file FileRepositoryBuilder)))

(set! *warn-on-reflection* true)

(declare set-git-dir)

(def logging-conf
  {:level     "info"
   :console   true
   :file "program.log"
   :files     [{:name "program.log"}
               {:name    "program-json.log"
                :encoder "json"}]
   :overrides {"ggsoft.git.core"            "debug"
               "ggsoft.git.repo" "debug"}})

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

(def usage-str
  ["Usage: ci-utils [OPTIONS] command"
   "command may be one of:"
   ""
   "- current-version: "
   "- registered version "
   "- update-file: updates the file replacing recorded version with "
   "  the current version and saving it as the new recorded version"])

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
    "write-properties" :write-properties
    :unknown-command))

(defn validate-command-mandatory-opts
  [command mandatory-opts]
  (fn [cmd opt-map]
    (if  (not (sets/subset?
               (set mandatory-opts)
               (into #{}
                     (for [[k v] opt-map
                           :when (some? v)]
                       k))))
      (let [opt-diff (sets/difference
                      (set mandatory-opts)
                      (into #{} (keys opt-map)))]
        {:status :error
         :message (str "Include following options: " (cstr/join ", " (map str opt-diff)))})
      {:status :ok})))

(def mandatory-options-by-command
  {:recorded-version nil
   :current-version  nil
   :write-properties #{:file}
   :update           #{:file}
   :bump-version-tag nil})

(def validator-by-command
  (->>
   (doall
    (for [[cmd mandatories] mandatory-options-by-command
          :when (some? mandatories)]
      [cmd (validate-command-mandatory-opts cmd mandatories)]))
   (into {})))

(defn- ok [& args]
  {:status :ok})

(defn- validate-command
  [command opts]
  (let [v (get validator-by-command command ok)]
    (v command opts)))

#_(reduce
   (fn [res v]
     (cond
       (= :error (:status res))
       res

       :otherwise))
   {:status :ok}
   validator-by-command)

(defn handle-parse-result [res]
  (let [{:keys [arguments
                errors
                options]} res
        command           (resolve-command (first arguments))
        status            (if (or (= command :unknown-command)
                                  (some? errors))
                            :error
                            :ok)
        wrap-result       (fn [r]
                            (if (= (:status r) :error)
                              r
                              (merge
                               r
                               (validate-command command options))))]
    (-> res
        (assoc :command command
               :status status)
        (wrap-result))))

(defn -main [& args]
  (init-logging!)
  (let [{:keys [options arguments summary errors] :as res}
        (-> args
            (parse-opts cli-opts)
            handle-parse-result)]
    (trace res)
    (cond
      (some? errors)               (handle-errors errors)
      (some-> options :help some?) (do
                                     (println "usage: " (first args) " [opts] command")
                                     (println summary))
      (= :ok (-> res :status))     (do
                                     (info "performing command " res)
                                     (println
                                      (perform-command res))))))
