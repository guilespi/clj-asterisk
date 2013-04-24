(ns clj-asterisk.internal.connection
  (:require [clojure.tools.logging :as log])
  (:use [slingshot.slingshot :only [try+]])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader)))

(defn connect
  "Connects to the specified server of the form

   {:name localhost :port 1231}

   Returns the newly created connection"
  [server]
  (let [socket (Socket. (:name server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:in in :out out :socket socket})]
    conn))

(defn write
  "Sends a message over a connection"
  [conn msg]
  (doto (:out @conn)
    (.println msg)
    (.flush)))

(defn readline
  "Reads a single line from a connection"
  [connection]
  (try+
   ;;if reading from a closed socket exception is raised nil is returned
   (.readLine (:in @connection))
   (catch Object _
     ;;this has no handling since it's the standard path when socket disconnects
     )))

(defn connected?
  [connection]
  (nil? (:exit @connection)))

(defn disconnect
  [connection]
  (.close (:out @connection))
  (.close (:socket @connection))
  (dosync (alter connection merge {:exit true})))