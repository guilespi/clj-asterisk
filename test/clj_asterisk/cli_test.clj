(ns clj-asterisk.cli-test
  (:require [clj-asterisk.manager :as manager]
            [clj-asterisk.events :as events])
  (:use midje.sweet))

(fact "Send CLI command"
      (manager/with-config
        {:name "192.168.0.127"}
        (let [context (manager/login "guille" "1234" :no-events)]
          (manager/with-connection context
            (select-keys (manager/action :COMMAND {:command "core show channels"})
                         [:Response])))) => {:Response "Success"})


