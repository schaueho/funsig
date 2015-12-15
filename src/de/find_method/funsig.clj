(ns de.find-method.funsig
  (:require [de.find-method.funsig.core :as core]
            [de.find-method.funsig.macros :as macros]))

(defonce ^:dynamic *locator* (core/start-new-locator))

(defmacro defsig
  "Define a signature, a combination of a function name and parameter list"
  [name & params]
  `(macros/defsig *locator* ~name ~@params))

(defmacro defimpl
  "Define an implementation for a signature"
  [name & sigs]
  `(macros/defimpl *locator* ~name ~@sigs))

(defmacro set-default-implementation!
  "Set a default implementation for a signature with multiple implementations"
  [name implname]
  `(macros/set-implementation! *locator* ~name ~implname))
