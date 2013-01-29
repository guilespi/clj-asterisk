(ns clj-asterisk.originate-test
  (:require [clj-asterisk.manager :as manager]
            [clj-asterisk.events :as events])
  (:use midje.sweet))

(def event-promise (promise))

(defmethod events/handle-event :default
  [event context]
  (println (str "event:" event)))

(fact "Create outgoing call"
      (manager/with-config
        {:name "192.168.0.127"}
        (let [context (manager/login "guille" "1234" :with-events)]
          (manager/with-connection context
            (let [response (manager/action :Originate {:Channel "SIP/1000"
                                                       :Context "test-context"
                                                       :Exten "1000"
                                                       :Priority "1"
                                                       :Timeout 60000
                                                       :CallerID "99970"
                                                       :Variables ["MESSAGE=This is a message"]
                                                       })]
              (select-keys response [:Message :Response]))))) => {:Message "Originate successfully queued", :Response "Success"})

