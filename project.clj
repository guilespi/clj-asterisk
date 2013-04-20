(defproject clj-asterisk "0.2.1"
  :description "Clojure bindings for Asterisk Manager API"
  :url "http://www.github.com/guilespi/clj-asterisk"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.incubator "0.1.2"]
                 [midje "1.5.1"]
                 [slingshot "0.10.3"]
                 [org.clojure/tools.logging "0.2.3"]]
  :plugins [[lein-midje "3.0.0"]])
