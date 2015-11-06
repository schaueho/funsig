(ns de.find-method.sigh.sigh-test
  (:use midje.sweet)
  (:require [com.stuartsierra.component :as component]
            [de.find-method.sigh.core :refer :all]))

;(declare testfn)

;(defn testfn [])

(facts "Adding signatures"
       (fact "After adding, I can find a signature and receive the arglist"
             (let [locator (component/start (->ServiceLocator nil))]
               (add-signature! locator :testfn ['testarg])
               (find-signature locator :testfn) => ['testarg])))
