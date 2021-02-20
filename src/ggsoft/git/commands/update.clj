(ns ggsoft.git.commands.update
  (:require [ggsoft.git.commands.multi :refer [perform-command]]
            [ggsoft.git.repo :as grepo]
            [clojure.tools.logging :refer [info error debug warn]]
            [clojure.string :as cstr])) 



(defmethod perform-command :update
  [_]
  (info "running update"))
