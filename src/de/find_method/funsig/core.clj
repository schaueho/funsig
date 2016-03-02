(ns de.find-method.funsig.core)

(defprotocol ServiceLocatorProtocol
  (add-signature!
    [locator name lambdalist]
    "Add a signature to the service locator")
  (find-signature
    [locator name]
    "Finds the signature for a name")
  (add-implementation!
    [locator name metamap lambdalist implname]
    "Add an implementation to the service locator")
  (find-implementation
    [locator name]
    "Finds the implementation for a name")
  (set-default-implementation!
    [locator name implname]
    "Sets a default implementation for a signature"))

(declare find-sigimpls matching-lambdalists?)

(defrecord ServiceLocator [services]
  ServiceLocatorProtocol

  (add-signature!
    [locator name lambdalist]
    (swap! (:services locator) assoc name {:lambdalist lambdalist
                                           :implementations nil
                                           :default-impl nil}))

  (find-signature
    [locator name]
    (when-let [[name sigimpls] (find-sigimpls locator name)]
      (:lambdalist sigimpls)))

  (add-implementation!
    [locator name metamap lambdalist implname]
    (if-let [[name sigimpls] (find-sigimpls locator name)]
      (if (matching-lambdalists? (:lambdalist sigimpls) lambdalist)
        (do (swap! (:services locator)
                   update-in [name :implementations] conj implname)
            (when (:primary metamap)
               (set-default-implementation! locator name implname)))
        (throw (ex-info (str "Lambda lists for " name " don't match: "
                             (:lambdalist sigimpls) "!=" lambdalist)
                        {:signature name
                         :lambda-sig sigimpls
                         :lambda-impl lambdalist})))
      (throw (ex-info (str "No signature registered for " name)
                      {:signature name}))))

  (find-implementation
    [locator name]
    (when-let [[name sigimpls] (find-sigimpls locator name)]
      (when (seq (:implementations sigimpls))
        (if-let [default-impl (:default-impl sigimpls)]
          (some (hash-set default-impl) (:implementations sigimpls))
          (first (:implementations sigimpls))))))

  (set-default-implementation!
    [locator name implname]
    (let [sigimpls (find-sigimpls locator name)]
      (when (or (not (vector? sigimpls))
                (not-any? (hash-set implname)
                           (:implementations (second sigimpls))))
        (throw (ex-info (str implname " is not a known implementation for " name)
                        {:signature name
                         :known-implementations (second sigimpls)})))
      (swap! (:services locator)
             assoc-in [name :default-impl] implname))))

(defn- find-sigimpls [locator name]
  "Return lambdalist and implementations for name"
  (find @(:services locator) name))

(defn- matching-lambdalists? [lambdalist1 lambdalist2]
  (= lambdalist1  lambdalist2))

(defn start-new-locator []
  "Start a new service locator"
  (map->ServiceLocator {:services (atom {})}))
