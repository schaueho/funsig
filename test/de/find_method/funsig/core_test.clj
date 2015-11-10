(ns de.find-method.funsig.core-test
  (:use midje.sweet)
  (:require [de.find-method.funsig.core :refer :all]))

(facts "Adding signatures"
       (fact "After adding, I can find a signature and receive the arglist"
             (let [locator (start-new-locator)]
               (add-signature! locator :testfn ['testarg])
               (find-signature locator :testfn) => ['testarg])))
