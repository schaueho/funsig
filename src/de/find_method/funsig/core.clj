(ns de.find-method.funsig.core)

(defprotocol ServiceLocatorProtocol
  (add-signature! [locator name lambdalist])
  (find-signature [locator name])
  (add-implementation! [locator name lambdalist implname])
  (find-implementation [locator name]))

(declare find-sigimpls matching-lambdalists?)

(defrecord ServiceLocator [services]
  ServiceLocatorProtocol

  (add-signature! ;  "Add a signature to the service locator"
    [locator name lambdalist]
    (swap! (:services locator) assoc name {:lambdalist lambdalist
                                           :implementations nil
                                           :default-impl nil}))

  (find-signature ; "Finds the signature for a name"
    [locator name]
    (when-let [[name sigimpls] (find-sigimpls locator name)]
      (:lambdalist sigimpls)))

  (add-implementation! ; "Add an implementation to the service locator"
    [locator name lambdalist implname]
    (if-let [[name sigimpls] (find-sigimpls locator name)]
      (if (matching-lambdalists? (:lambdalist sigimpls) lambdalist)
        ; TODO: handle multiple implementations correctly
        (swap! (:services locator)
               update-in [name :implementations] conj implname)
        (throw (Exception. (str "Lambda lists for " name " don't match: "
                                (:lambdalist sigimpls) "!=" lambdalist))))
      (throw (Exception. (str "No signature registered for " name)))))

  (find-implementation ; "Finds the implementation for a name"
    [locator name]
    (when-let [[name sigimpls] (find-sigimpls locator name)]
      (when (seq (:implementations sigimpls))
        (first (:implementations sigimpls))))))

(defn- find-sigimpls [locator name]
  "Return lambdalist and implementations for name"
  (find @(:services locator) name))

(defn- matching-lambdalists? [lambdalist1 lambdalist2]
  (= lambdalist1  lambdalist2))

(defn start-new-locator []
  "Start a new service locator"
  (->ServiceLocator (atom {})))
