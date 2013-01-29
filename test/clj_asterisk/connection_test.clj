(ns clj-asterisk.connection-test
  (:use midje.sweet
        clj-asterisk.internal.connection
        [slingshot.slingshot :only [throw+ try+]]))


(fact "Connection to valid ip/port connects"
      (when-let [conn (connect {:name "192.168.0.127" :port 5038})]
        "connected") => "connected")

(fact "Connection to invalid ip/port timesout"
      (try+
       (connect {:name "192.168.0.126" :port 5038})
       (catch Object _
         (str (:throwable &throw-context)))) => "java.net.ConnectException: Operation timed out")

(fact "Read first line from connection returns banner"
      (let [conn (connect {:name "192.168.0.127" :port 5038})]
        (readline conn)) => "Asterisk Call Manager/1.1")

