(ns de.find-method.sigh.macros
  (:require [de.find-method.sigh.core :as si :refer :all]))

(defmacro defsig
  "Define a signature, a combination of a function name and parameter list"
  [name params]
  `(do
     (si/add-signature! *locator* '~name '[~@params])
     (defn ~name [~@params]
       (if-let [implementation# (find-implementation *locator* '~name)]
         (implementation# ~@params)
         (throw (Exception. (str "No implementation registered for " '~name)))))))

(defmacro defimpl
  "Define an implementation for a signature"
  [name params & forms]
  `(let [implname# (symbol (str '~name "-impl"))]
     (defn ~(symbol (str name "-impl")) [~@params]
       ~@forms)
     (add-implementation! *locator* '~name '[~@params] ~(symbol (str name "-impl")))))
