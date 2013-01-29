(ns clj-asterisk.events
   (:require [clojure.tools.logging :as log]))

(defmulti handle-event
  "The event handler dispatches on the event name
   which is a string value"
  :Event)

(defmethod handle-event :default
  [event context]
  (log/trace (str "Event received -> " (pr-str event))))