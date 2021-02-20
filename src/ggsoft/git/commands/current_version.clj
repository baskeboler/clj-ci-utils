(ns ggsoft.git.commands.current-version
  (:require [ggsoft.git.commands.multi :refer [perform-command]]
            [ggsoft.git.repo :refer [default-git current-version]]
            [clojure.tools.logging :refer [info]])
  (:import [org.eclipse.jgit.api Git]))




(defmethod perform-command :current-version
  [_]
  (let [v (current-version (default-git))]
    (info "Current version: " v)
    v))
