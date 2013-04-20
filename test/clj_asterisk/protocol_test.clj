(ns clj-asterisk.protocol-test
  (:use midje.sweet
        clj-asterisk.internal.protocol
        [slingshot.slingshot :only [throw+ try+]]))

(def parse-line (ns-resolve 'clj-asterisk.internal.protocol 'parse-line))

(fact "Conversion from hashmap to asterisk protocol"
      (clj->ast :Originate {:Channel "1234"}) => "Action: Originate\r\nChannel: 1234\r\n")

(fact "Conversion from hashmap to asterisk protocol using variables"
      (clj->ast :Originate {:Channel "1234"
                            :Variables ["CALLER_ID=555444"]}) => "Action: Originate\r\nChannel: 1234\r\nVariable: CALLER_ID=555444\r\n")

(fact "Conversion from hashmap to asterisk protocol using multiple variables"
      (clj->ast :Originate {:Channel "1234"
                            :Variables ["CALLER_ID=555444"
                                        "CALLID=0000000"]}) => "Action: Originate\r\nChannel: 1234\r\nVariable: CALLER_ID=555444\r\nVariable: CALLID=0000000\r\n")


(fact "Conversion from hashmap to asterisk protocol using variable with spaces"
      (clj->ast :Originate {:Channel "1234"
                            :Variables ["MESSAGE=este es un mensaje"]}) => "Action: Originate\r\nChannel: 1234\r\nVariable: MESSAGE=este es un mensaje\r\n")

(fact "Conversion from hashmap to asterisk protocol using variable with comas"
      (clj->ast :Originate {:Channel "1234"
                            :Variables ["MESSAGE=este, es un mensaje"]}) => "Action: Originate\r\nChannel: 1234\r\nVariable: MESSAGE=este\\, es un mensaje\r\n")

(fact "Conversion from string to hashmap"
      (ast->clj ["Response: true"
                 "Channel: 1234"]) => {:Response "true" :Channel "1234"})

(fact "Invalid line throws error"
      (try+
       (parse-line "this line is invalid")
       (catch :type e
         "catched")) => "catched")