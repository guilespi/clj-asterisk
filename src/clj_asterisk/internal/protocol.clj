(ns clj-asterisk.internal.protocol
  (:require [clojure.tools.logging :as log])
  (:use [clojure.string :only [blank? join split trim]]
        [slingshot.slingshot :only [throw+]]))

(defn- parse-line
  [line]
  (let [kv (split line #":")]
    (if (= (count kv) 2)
      {(keyword (kv 0)) (trim (kv 1))}
      (throw+ {:type ::invalid-line :line line}))))

(defn end-of-message?
  "Returns true if the line represents the end of a distinct packet"
  [line]
  (blank? line))

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
         (str "\r\n" (join "\r\n" (map #(format "Variable: %s" %) vars))))
       "\r\n"))

(defn ast->clj
  "Given a list of lines from the asterisk manager protocol creates a
   corresponding clojure hashmap."
  [lines]
  (log/info (str "Parsing " lines))
  (reduce #(merge %1 (parse-line %2)) {} lines))