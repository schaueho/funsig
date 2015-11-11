(ns de.find-method.funsig.macros
  (:require [de.find-method.funsig.core :as si :refer :all]))

(defmacro defsig
  "Define a signature, a combination of a function name and parameter list"
  [locator name params]
  (let [locator# locator]
  `(do
     (si/add-signature! ~locator# '~name '[~@params])
     (defn ~name [& varargs#]
       (if-let [implementation# (find-implementation ~locator# '~name)]
         (apply implementation# varargs#)
         (throw (Exception. (str "No implementation registered for " '~name))))))))

(defmacro defimpl
  "Define an implementation for a signature"
  [locator name & sigs]
  (when (not (seq? sigs))
    (throw (Exception. "Implementation definition doesn't have a valid signature")))
  (let [implname# (symbol (str name "-impl"))
        sigs# (first sigs)
        params# (cond (list? sigs#) (mapv first sigs) ; variadic function
                      (vector? sigs#) sigs#           ; normal [arglist] &forms definition
                      :else
                      (throw
                       (Exception. "Implementation definition doesn't have a valid signature")))]
    (if (list? sigs#) ; variadic or simple function implementation
      `(do (defn ~(symbol (str name "-impl")) ~@sigs)
           (add-implementation! ~locator '~name '~params# ~(symbol (str name "-impl"))))
      `(do (defn ~(symbol (str name "-impl")) [~@(first sigs)] ~@(rest sigs))
           (add-implementation! ~locator '~name '~params# ~(symbol (str name "-impl")))))))

(defmacro set-implementation!
  "Set an implementation for a signature to be used as default"
  ; we need this as a macro because otherwise the names (vars) will be
  ; evaluated to the respective function objects -- which will all be
  ; unknown in the locator (the locator stores symbols only)
  [locator name implname]
  `(set-default-implementation! ~locator '~name ~implname))
