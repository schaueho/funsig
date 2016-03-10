(ns de.find-method.funsig.macros
  (:require [clojure.string :as string]
            [de.find-method.funsig.core :as si
             :refer [add-signature! find-implementation add-implementation! set-default-implementation!]]))

(defmacro defsig
  "Define a signature, a combination of a function name and parameter list"
  [locator signame & params]
  (let [nsname (symbol (str (ns-name *ns*)) (name signame))
        locator# locator
        meta# (if (string? (first params))
                {:doc (first params)}
                {})
        params (if (string? (first params))
                 (next params)
                 params)
        meta# (if (map? (first params))
                (conj meta# (first params))
                meta#)
        params (if (map? (first params))
                 (next params)
                 params)
        params (if (seq? (first params))
                 params
                 (list params))]
  `(do
     (add-signature! ~locator# '~nsname '~@params)
     (defn ~signame ~(assoc meta# :arglists `'~@params) [& varargs#]
       (if-let [implementation# (find-implementation ~locator# '~nsname)]
         (apply (resolve implementation#) varargs#)
         (throw (ex-info (str "No implementation registered for " '~nsname)
                         {:signature '~nsname})))))))

(defmacro defimpl
  "Define an implementation for a signature"
  [locator signame & sigs]
  (when-not (seq? sigs)
    (throw (ex-info (str "Implementation definition for "
                         signame " doesn't have a valid signature")
                    {:signature signame})))

  (let [sigvar# (resolve signame)
        nsname# (when sigvar#
                  (ns-name (:ns (meta sigvar#))))
        nsname# (if (nil? nsname#)
                 signame
                 (symbol (name nsname#) (name signame)))
        localname# (string/replace-first signame #".*/" "")
        implname# (symbol (str localname# "-impl"))
        implnsname# (symbol (name (ns-name *ns*)) (str localname# "-impl"))
        sigs# sigs
        meta# (if (map? (meta signame))
                (meta signame)
                {})
        meta# (if (map? (first sigs#))
                (merge meta# (first sigs#))
                meta#)
        sigs# (if (map? (first sigs#))
                 (rest sigs#)
                 sigs#)
        params# (cond (vector? (first sigs#)) (list (first sigs#)) ; normal [arglist] &forms definition
                      (list? (first sigs#))      (map first sigs#) ; variadic function
                      :else
                      (throw
                       (ex-info (str
                                 "Implementation definition for "
                                 nsname#
                                 " doesn't have a valid signature")
                                {:signature sigs#})))]
      `(do (add-implementation! ~locator '~nsname# '~meta# '~params# '~implnsname#)
           (defn ~implname# ~meta# ~@sigs#))))

(defmacro set-implementation!
  "Set an implementation for a signature to be used as default"
  ; we need this as a macro because otherwise the names (vars) will be
  ; evaluated to the respective function objects -- which will all be
  ; unknown in the locator (the locator stores symbols only)
  [locator signame implname]
  (let [sigvar# (resolve signame)
        nsname# (when sigvar#
                  (ns-name (:ns (meta sigvar#))))
        nsname# (if (nil? nsname#)
                 signame
                 (symbol (name nsname#) (name signame)))
        implvar# (resolve implname)
        implnsname# (when implvar#
                      (ns-name (:ns (meta implvar#))))
        implnsname# (if (nil? implnsname#)
                      implname
                      (symbol (name implnsname#) (name implname)))]
  `(set-default-implementation! ~locator '~nsname# '~implnsname#)))
