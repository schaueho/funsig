(ns de.find-method.sigh.macros
  (:require [de.find-method.sigh.core :as si :refer :all]))

(defmacro defsig
  "Define a signature, a combination of a function name and parameter list"
  [name params & {:keys [locator] :or {locator nil}}]
  `(do
     (si/add-signature! ~locator '~name '[~@params])
     (defn ~name [~@params]
       [~@params])))
