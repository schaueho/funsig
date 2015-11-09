(defproject de.find-method/funsig "0.1.0-SNAPSHOT"
  :description "Signature dependency inversion and injection for functions"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [commons-codec "1.6"]
                 [com.stuartsierra/component "0.2.3"]]
  :offline? true
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}})
