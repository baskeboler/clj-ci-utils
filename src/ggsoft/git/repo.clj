(ns ggsoft.git.repo
  (:import [org.eclipse.jgit.lib Repository Ref] 
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.storage.file FileRepositoryBuilder]
           [java.io File])
  (:require [clojure.java.io :as io]
            [clojure.string :as cstr]))
(defn ^Repository git-repo
  ([^File  dir]
   (-> (FileRepositoryBuilder.)
       (.setMustExist true)
       (.setGitDir dir)
       (.readEnvironment)
       (.findGitDir)
       (.build)))
  ([]
   (-> (FileRepositoryBuilder.)
       (.readEnvironment)
       (.findGitDir)
       (.build))))

(defn set-git-dir [d]
  (let [dir (io/file d)]
    (if (= (.getName dir) ".git")
      d
      (str d "/.git"))))

(defn git-repo? [dir]
  (try
    (-> (io/as-file (set-git-dir dir))
        git-repo
        .isBare
        not)
    (catch Exception _
      false)))

(defn default-git
  []
  (Git. (git-repo)))

(defn git [^Repository repo] (Git. repo))

(defn ^:export current-version [^Git g]
  (-> g
      (.describe)
      (.setLong true)
      (.call)))

(defn create-tag [^Git g tag-name]
  (-> g
      (.tag)
      (.setName tag-name)
      (.call)))

(defn tag-list [^Git g]
  (-> g
      .tagList
      .call
      (->> (map #(.getName ^Ref %)))))

(defn latest-tag [^Git g]
  (last (tag-list g)))


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
