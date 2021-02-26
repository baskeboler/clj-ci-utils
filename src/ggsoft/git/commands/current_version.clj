(ns ggsoft.git.commands.current-version
  (:require [ggsoft.git.commands.multi :refer [perform-command]]
            [ggsoft.git.repo :refer [default-git current-version]]))




(defmethod perform-command :current-version
  [_]
  (let [v (current-version (default-git))]
    (println v)
    v))
