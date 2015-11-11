(ns de.find-method.testimpl2
  (:require [de.find-method.funsig :as di :refer [defimpl]]
            [de.find-method.testsigs :as testsig :refer [fetch-multiple]]))

(defimpl fetch-multiple [foo] 'foo2)

