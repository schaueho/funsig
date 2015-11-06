(ns de.find-method.sigh.core
  (:require [com.stuartsierra.component :as component]))

(defprotocol ServiceLocatorProtocol
  (add-signature! [locator name lambdalist])
  (find-signature [locator name]))

(defrecord ServiceLocator [services]
  component/Lifecycle ServiceLocatorProtocol

  (start [locator]
    (assoc locator :services (atom {})))

  (stop [locator]
    (assoc locator :services nil))

  (add-signature! ;  "Add a signature to the service locator"
    [locator name lambdalist]
    (swap! (:services locator) assoc name {:lambdalist lambdalist 
                                           :implementations nil}))

  (find-signature ; "Finds the signature for a name"
    [locator name]
    (when-let [[name sigimpls] (find @(:services locator) name)]
      (:lambdalist sigimpls))))
