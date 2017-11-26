(ns clj-asterisk.manager
  (:require [clj-asterisk.internal.connection :as connection]
            [clj-asterisk.internal.events :as events]
            [clojure.tools.logging :as log])
  (:use [clj-asterisk.internal.core :only [send-message read-response
                                           get-connection wait-async-response]]
        [clj-asterisk.internal.def]
        [slingshot.slingshot :only [throw+ try+]]))

(def ^:dynamic *server* {:name "127.0.0.1" :port 5038})

(def event-subscriptions {:no-events "off"
                          :with-events "on"})

(def known-banners #{"Asterisk Call Manager/1.0"
                     "Asterisk Call Manager/1.1"
                     "Asterisk Call Manager/1.2"
                     "Asterisk Call Manager/1.3"
                     "Asterisk Call Manager/2.8.0"})

(defalias with-connection clj-asterisk.internal.core/with-connection*)

(defmacro with-config
  "Binds the current asterisk configuration and executes the passed body.
   Allowed configuration values are :name and :port.
   If port is not specified defaults to 5038"
  [config & body]
  `(binding [*server* (merge *server* ~config)]
     ~@body))

(defn success?
  [response]
  (= (:Response response) "Success"))

(defn failed?
  [response]
  (not (success? response)))

(defn- authenticate
  "Authenticates a user using the bound connection in *context*"
  [user password subscription]
  (let [action-id (send-message :Login {:Username user
                                        :Secret password
                                        :Events (event-subscriptions subscription)})
        response (read-response action-id)]
    (if (failed? response)
      (throw+ {:type ::authentication-error :user user :action-id action-id :response response})
      response)))

(defn- handshake?
  "Validate the connection is against a supported asterisk manager server
   by comparing the first line with the known banners"
  []
  (let [banner (connection/readline (get-connection))]
    (or (contains? known-banners banner)
        (throw+ {:type ::unknown-banner :banner banner}))))

(defn logout
  "Disconnects the current manager session"
  []
  (connection/disconnect (get-connection)))

(defn login
  "Logins the specified user using the *server* configuration to connect
   Needs to be called inside with-config macro.

  (with-config {:name asterisk}
    (login user password :no-events)
    (login user password :with-events)

  The function returns a newly created context with the authenticated
  connection"
  [user password subscription]
  (try+
   (let [conn (connection/connect *server*)
         context (atom {:connection conn :server *server* :packets {} :timeout 5000})]
     (with-connection context
       (try+
        (when (handshake?)
          (events/async-reader context)
          (authenticate user password subscription)
          context)
        (catch :type e
          (log/error e)
          (logout)))))
   (catch Object _
     (log/error (:throwable &throw-context) " unexpected error"))))

(defn action
  "Executes an action using the current connection.

  manager/action :Originate {:Channel 'SIP/Trunk' :Extension 1000}
  manager/action :Ping

   returns the server response or nil (FIX: this)"
  ([operation]
     (action operation {}))
  ([operation parameters]
     (try+
      (let [action-id (send-message operation parameters)
            response (read-response action-id (:Timeout parameters))]
        (if (failed? response)
          (throw+ {:type ::action-error :operation operation :parameters parameters :action-id action-id :response response})
          (if (:Async parameters)
            ;;if action is async another packet is received with the same
            ;;Actionid response
            (wait-async-response action-id (:Timeout parameters))
            response)))
      (catch :type e
        (log/error e)
        e)
      (catch Object _
        (log/error (:throwable &throw-context) " unexpected error")))))

(defn set-user-data!
  [id value]
  (clj-asterisk.internal.core/set-user-data! id value))

(defn get-user-data
  [id]
  (clj-asterisk.internal.core/get-user-data id))

(defn remove-user-data!
  [id]
  (clj-asterisk.internal.core/remove-user-data! id))
