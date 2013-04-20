(ns clj-asterisk.internal.core
  (:require [clj-asterisk.internal.connection :as connection]
            [clj-asterisk.internal.protocol :as protocol]
            [clojure.tools.logging :as log])
  (:use [clojure.core.incubator :only [dissoc-in]]
        [slingshot.slingshot :only [throw+ try+]]))

(def ^:dynamic *context* nil)

(def action-id (atom 0))
(defn- next-id
  "Returns an action id to be used in messages as identifier"
  []
  (swap! action-id inc))

(defmacro with-connection*
  "Executes the given sets of commands in body using the specified
   manager connection"
  [context & body]
  `(binding [*context* ~context]
     ~@body))

(defn send-message
  "Sends a message over the specified context connection.
   Returns the action id associated with the message in order
   to wait for a response on it.

   *action* needs to be a parameters since the protocol requires it to be
   the first attribute in the packet and hashmap do not ensure ordering"
  [action message]
     (log/info (format "Sending action %s with parameters %s" action message))
     (let [message-id (next-id)
           prepared-message (protocol/clj->ast action (merge message {:ActionID message-id}))
           conn (:connection @*context*)]
       (do
         (swap! *context* #(assoc-in % [:packets message-id] (promise)))
         (connection/write conn prepared-message)
         message-id)))

(defn read-response
  "Searches the internal packet buffer of the connection
   for a specific action response, if found, removes it from
   the packet list and returns it"
  ([action-id]
     (read-response action-id (:timeout @*context*)))
  ([action-id timeout]
     (if-let [p (get (:packets @*context*) action-id)]
       (if-let [response (deref p (or timeout (:timeout @*context*)) nil)]
         (do
           (swap! *context* #(dissoc-in % [:packets action-id]))
           response)
         (throw+ {:type ::timeout :action-id action-id}))
       (throw+ {:type ::nopromise :action-id action-id}))))

(defn get-connection
  "Returns the associated connection to the current context"
  []
  (:connection @*context*))

(defn get-action-promise
  [action-id]
  (get-in @*context* [:packets action-id]))

(defn current-context
  []
  *context*)

(defn set-user-data!
  [id value]
  (swap! *context* #(assoc-in % [:user-data id] value))
  value)

(defn get-user-data
  [id]
  (get-in @*context* [:user-data id]))

(defn remove-user-data!
  [id]
  (swap! *context* #(dissoc-in % [:user-data id])))
