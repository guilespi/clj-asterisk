(ns clj-asterisk.internal.protocol
  (:require [clojure.tools.logging :as log])
  (:use [clojure.string :only [blank? join split trim]]
        [slingshot.slingshot :only [throw+]]))

(defn- parse-line
  [line]
  (let [kv (split line #":")]
    (when (= (count kv) 2)
      {(keyword (kv 0)) (trim (kv 1))})))

(defn end-of-message?
  "Returns true if the line represents the end of a distinct packet"
  [line]
  (blank? line))

(defn end-of-command?
  "Returns true if the line represents the end of a cli command response"
  [line]
  (= line "--END COMMAND--"))

(defn escape-variable
  "Escapes commas since are used as separators inside asterisk variable parser"
  [variable]
  (let [[_ name value] (re-find #"([^=]+)=(.+)$" variable)]
    (str name "=" (clojure.string/replace value #"," "\\\\,"))))

(defn clj->ast
  "Converts the message from a clojure hashmap to the stringified
   asterisk manager protocol, the expected message is of the form:

   {:Action Originate
    :Context Outgoing
    ...}

   The created message is as specified in the docs:
  
   Action: <action type><CRLF>
   <Key 1>: <Value 1><CRLF>
   <Key 2>: <Value 2><CRLF>
   ...
   Variable: <Variable 1>=<Value 1><CRLF>
   Variable: <Variable 2>=<Value 2><CRLF>
   ...
   <CRLF>"
  [action message]
  (str "Action: " (name action) "\r\n"
       (join "\r\n" (map #(format "%s: %s" (name (% 0)) (% 1)) (dissoc message :Variables)))
       (when-let [vars (:Variables message)]
         (str "\r\n" (join "\r\n" (map #(format "Variable: %s" (escape-variable %)) vars))))
       "\r\n"))

(defn process-line
  [packet line]
  (if-let [parsed-line (parse-line line)]
    (merge packet parsed-line)
    (if (= (:Response packet) "Follows")
      (assoc packet :Data (conj (or (:Data packet) []) line))
      (throw+ {:type ::invalid-line :line line}))))

(defn ast->clj
  "Given a list of lines from the asterisk manager protocol creates a
   corresponding clojure hashmap."
  [lines]
  (log/info (str "Parsing " lines))
  (let [packet (reduce #(process-line %1 %2) {} lines)]
    (if (= (:Response packet) "Follows")
      {:Response "Success" 
       :ActionID (:ActionID packet)
       :Data (filter (comp not end-of-command?) (:Data packet))}
      packet)))