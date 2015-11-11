(ns de.find-method.testimpl1
  (:require [de.find-method.funsig :as di :refer [defimpl]]
            [de.find-method.testsigs :as testsig :refer [fetch-multiple]]))

(defimpl fetch-multiple [foo] 'foo1)

