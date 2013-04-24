(ns clj-asterisk.load-test
  (:require [clj-asterisk.manager :as manager]
            [clj-asterisk.events :as events]
            [clojure.tools.logging :as log]))

(defmethod events/handle-event :default
  [event context]
  (log/info (format "Unhandled Event => %s" event)))

(defn ping [context index]
  (log/info "Pinging" index)
  (manager/with-connection context
    (select-keys (manager/action :PING)
                 [:Response :Ping])))

(defn run 
  [threads]
  (log/info "Running...")
  (manager/with-config
    {:name "192.168.0.127"}
    (let [context (manager/login "guille" "1234" :with-events)]
      (let [dispatched (map #(future (ping context %)) (range 0 threads))]
        (loop [pending dispatched iteration 0 finished 0]
          (if (or (> iteration 10) (not (seq pending)))
            (log/info (format "Finished %s Pendings %s" finished (count pending)))
            (let [still-pending (filter (comp not realized?) pending)
                  step-finished (- (count pending) (count still-pending))]
              (log/info "Pings finished this round:" step-finished)
              (Thread/sleep 100)
              (recur still-pending (inc iteration) (+ finished step-finished))))))
      (manager/with-connection context
        (manager/logout)))))

(defn -main 
  "Synchronous test for multiple actions at the same time, parameter is how many concurrent actions to send

   lein trampoline run -m clj-asterisk.load-test 1000"
  [threads]
  (run (Integer. threads)))
