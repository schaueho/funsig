(ns de.find-method.funsig.macros
  (:require [clojure.string :as string]
            [de.find-method.funsig.core :as si :refer :all]))

(defmacro defsig
  "Define a signature, a combination of a function name and parameter list"
  [locator signame & params]
  (let [nsname (symbol (str (ns-name *ns*)) (name signame))
        locator# locator
        m (if (string? (first params))
            {:doc (first params)}
            {})
        params (if (string? (first params))
                 (next params)
                 params)
        m (if (map? (first params))
            (conj m (first params))
            m)
        params (if (map? (first params))
                 (next params)
                 params)
        params (if (seq? (first params))
                 params
                 (list params))]
  `(do
     (si/add-signature! ~locator# '~nsname '~@params)
     (defn ~signame ~(assoc m :arglists `'~@params) [& varargs#]
       (if-let [implementation# (find-implementation ~locator# '~nsname)]
         (apply (resolve implementation#) varargs#)
         (throw (Exception. (str "No implementation registered for " '~nsname))))))))

(defmacro defimpl
  "Define an implementation for a signature"
  [locator signame & sigs]
  (when (not (seq? sigs))
    (throw (Exception. (str "Implementation definition for "
                            signame " doesn't have a valid signature"))))

  (let [sigvar# (resolve signame)
        nsname# (when sigvar#
                  (ns-name (:ns (meta sigvar#))))
        nsname# (if (nil? nsname#)
                 signame
                 (symbol (name nsname#) (name signame)))
        localname# (string/replace-first signame #".*/" "")
        implname# (symbol (str localname# "-impl"))
        implnsname# (symbol (name (ns-name *ns*)) (str localname# "-impl"))
        sigs# (first sigs)
        params# (cond (list? sigs#) (map first sigs) ; variadic function
                      (vector? sigs#) (list sigs#)   ; normal [arglist] &forms definition
                      :else
                      (throw
                       (Exception. (str
                                    "Implementation definition for "
                                    nsname#
                                    " doesn't have a valid signature"))))]
    (if (list? sigs#) ; variadic or simple function implementation
      `(do (defn ~implname# ~@sigs)
           (add-implementation! ~locator '~nsname# '~params# '~implnsname#))
      `(do (defn ~implname# [~@(first sigs)] ~@(rest sigs))
           (add-implementation! ~locator '~nsname# '~params# '~implnsname#)))))

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
