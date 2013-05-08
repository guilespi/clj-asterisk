# clj-asterisk

clj-asterisk is a Clojure binding for [Asterisk Manager API](http://www.voip-info.org/wiki/view/Asterisk+manager+API).

## Install

Add the following dependency to your `project.clj` file:

    [clj-asterisk "0.2.2"]

## Usage

### Logging in

When logging in, you can subscribe to events or not using the `:with-events` and `:no-events` keywords. 
The login function returns the context object to be used in future action invocations.

```clojure

(:require [clj-asterisk.manager :as manager)

(manager/with-config
  {:name "asterisk.host"}
  (manager/login "user" "secret" :no-events)) => {context}

```

### Sending Commands

Commands are sent using the `action` function. In case the command parameter has a :Timeout key the action waits that time for the
response instead of the default, 5000 ms.

```clojure

(:require [clj-asterisk.manager :as manager)

(manager/with-config
  {:name "asterisk.host"}
  (let [context (manager/login "user" "secret" :no-events)]
    (manager/with-connection context
      (manager/action :PING)))) => {:Response "Success" :ActionID "2" :Ping "Pong" :Timestamp "1358699209.141867"}

(manager/with-config
  {:name "asterisk.host"}
  (let [context (manager/login "user" "pass" :with-events)]
    (manager/with-connection context
      (let [response (manager/action :Originate {:Channel "SIP/1000"
                                                 :Context "test-context"
                                                 :Exten "1000"
                                                 :Priority "1"
                                                 :Timeout 60000
                                                 :CallerID "99970"
                                                 :Variables ["VAR=VALUE"]
                                                 })]
        response)))) => {:Message "Originate successfully queued", :Response "Success"}

```

### Originating Calls

In order to send many calls over the same connection the :Async parameter should be used.

```clojure

(manager/with-config
  {:name "asterisk.host"}
  (let [context (manager/login "user" "pass" :with-events)]
    (manager/with-connection context
      (let [response (manager/action :Originate {:Channel "SIP/1000"
                                                 :Context "test-context"
                                                 :Exten "1000"
                                                 :Priority "1"
                                                 :Timeout 60000
                                                 :CallerID "99970"
                                                 :Async "true"
                                                 :Variables ["VAR=VALUE"]
                                                 })]
        response)))) => {:Reason "4", :Response "Success"}
```

In this case the response packet contains the completion reason according to these statuses:

```
Connected = "4"
Busy = "5"
Congestion = "8"
No Answer = "3"
Failure = "0"
```

Consider this strategy will block your thread until dialing completion, not until call completion. In
case you need to know when the call ends you need to wait for the `Hangup` event.

### Waiting for events

Events are dispatched using the multimethod `handle-event` and dispatching on the event name, so you decide which
events to handle declaring each needed multimethod.

```clojure

(:require [clj-asterisk.manager :as manager]
          [clj-asterisk.events :as events])

(defmethod events/handle-event "FullyBooted"
  [event context]
  (println event)) => {:Status "Fully Booted", :Privilege "system,all", :Event "FullyBooted"}

(manager/with-config
  {:name "asterisk.host"}
  (manager/login "user" "secret" :with-events))

```

### Executing CLI Commands

CLI Commands are supported using the :COMMAND action, if the command succeeds the response string is made available as a sequence of lines in the `Data` attribute of the response map.

```clojure
(manager/with-config    
        {:name "asterisk.host"}
        (let [context (manager/login "user" "password" :no-events)]
          (manager/with-connection context
            (select-keys (manager/action :COMMAND {:command "core show channels"})
                         [:Response :Data])))) 
      => {:Response "Success" 
          :Data ("Channel              Location             State   Application(Data)             " 
                 "0 active channels" 
                 "0 active calls" 
                 "11 calls processed")
```

## TODO

* Mock and fake some tests in order to run without a live asterisk

## License

Copyright (C) 2013 Guillermo Winkler

Distributed under the Eclipse Public License, the same as Clojure.
