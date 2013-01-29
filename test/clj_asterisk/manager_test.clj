(ns clj-asterisk.manager-test
  (:require [clj-asterisk.manager :as manager]
            [clj-asterisk.events :as events])
  (:use midje.sweet))

(fact "Login valid user"
      (manager/with-config
        {:name "192.168.0.127"}
        (let [context (manager/login "guille" "1234" :no-events)]
          context)) => anything)

(fact "Send PING command"
      (manager/with-config
        {:name "192.168.0.127"}
        (let [context (manager/login "guille" "1234" :no-events)]
          (manager/with-connection context
            (select-keys (manager/action :PING)
                         [:Response :Ping])))) => {:Response "Success" :Ping "Pong"})

(def event-promise (promise))
(defmethod events/handle-event "FullyBooted"
  [event context]
  (deliver event-promise event))

(fact "Wait for events"
      (manager/with-config
        {:name "192.168.0.127"}
        (let [context (manager/login "guille" "1234" :with-events)]
          (deref event-promise 5000 "Event arrival timed out"))) => {:Status "Fully Booted", :Privilege "system,all", :Event "FullyBooted"})


