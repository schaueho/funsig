(ns de.find-method.testimpl3
  (:require [de.find-method.funsig :as di :refer [defimpl]]
            [de.find-method.testsigs :as testsig :refer [fetch-multiple]]))

(defimpl testsig/fetch-multiple [foo] 'foo3)
