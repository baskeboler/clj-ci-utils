(ns ggsoft.git.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as cstr]
            [ggsoft.git.commands.core]
            [ggsoft.git.commands.multi :refer [perform-command]]
            [ggsoft.git.repo :refer [default-git current-version]]
            [clojure.edn]
            [clojure.data.json]
            [clojure.set :as sets])
  #_(:import (java.io File)
           (org.eclipse.jgit.api Git)
           (org.eclipse.jgit.lib Ref Repository)
           (org.eclipse.jgit.storage.file FileRepositoryBuilder)))

(set! *warn-on-reflection* true)

(declare set-git-dir)

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
  [["-a" "--application-name APP_NAME" "Application Name"
    :default "application"
    :id :application-name]

   ["-f" "--file PATH" "Input file path"
    :default nil]
   ["-o" "--output-format FORMAT" "Output format (json or edn)"
    :default :edn
    :parse-fn keyword
    :validate [#{:json :edn} "invalid output format"]]
   ;; A non-idempotent option (:default is applied first)
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc]
                                        
   ["-h" "--help"
    :id :help]])

(def usage-str
  [""
   "Usage: ci-utils [OPTIONS] command"
   "command may be one of:"
   ""
   "- current-version: "
   "- registered version "
   "- update: updates the file replacing recorded version with "
   "          the current version and saving it as the new recorded version"
   "- write-properties: writes a properties file with the application name and"
   "                    the current version"
   "- get-versions: scans file for version strings"
   ""])

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
    "get-versions"     :get-versions
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
                      (into #{} (for  [[k v] opt-map :when (some? v)] k)))]
        {:status  :error
         :message (apply str "Include following options: " (cstr/join ", " (map str (vec opt-diff))))})
      {:status :ok})))

(def mandatory-options-by-command
  {:recorded-version nil
   :current-version  nil
   :get-versions     #{:file}
   :write-properties #{:file :application-name}
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
  ;; (init-logging!)
  (let [{:keys [options arguments summary errors] :as res}
        (-> args
            (parse-opts cli-opts)
            handle-parse-result)]
    ;; (println res)
    (cond
      (some? errors)               (do
                                     (handle-errors errors)
                                     1)
      (some-> options :help some?) (do
                                     (println (cstr/join \newline usage-str))
                                     (println summary)
                                     0)
      (= :ok (-> res :status))     (do
                                     (perform-command res)
                                     0)
      :else                        (do
                                     (println "failed.")
                                     1))))
