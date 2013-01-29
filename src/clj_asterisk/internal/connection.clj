(ns clj-asterisk.internal.connection
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
        conn (ref {:in in :out out})]
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
  (.readLine (:in @connection)))

(defn connected?
  [connection]
  (nil? (:exit @connection)))

(defn disconnect
  [connection]
  ;TODO:destroy the damn socket?
  (dosync (alter connection merge {:exit true})))