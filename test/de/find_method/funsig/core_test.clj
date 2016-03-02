(ns de.find-method.funsig.core-test
  (:use midje.sweet)
  (:require [de.find-method.funsig.core :refer :all]))

(facts "Adding signatures"
       (fact "After adding, I can find a signature and receive the arglist"
             (let [locator (start-new-locator)]
               (add-signature! locator :testfn ['testarg])
               (find-signature locator :testfn) => ['testarg])))

(facts "Adding implementations"
       (let [locator (start-new-locator)]
         (fact "Adding an implementation requires a matching signature"
               (add-implementation! locator :nosuchsign {} ['foo] :nosuchsign-impl)
                => (throws Exception "No signature registered for :nosuchsign"))
         (fact "I can find added implementations"
               (do (add-signature! locator :testfn ['testarg])
                   (add-implementation! locator :testfn {} ['testarg] :testfn-impl)
                   (find-implementation locator :testfn)) => :testfn-impl)
         (fact "I can add multiple implementations"
               (do (add-implementation! locator :testfn {} ['testarg] :testfn-impl2)
                   (find-implementation locator :testfn)) => :testfn-impl2
               (-> (find @(:services locator) :testfn)
                   second
                   :implementations
                   count) => 2)
         (fact "After adding multiple implementations, I can set a default"
               (do (set-default-implementation! locator :testfn :testfn-impl)
                   (find-implementation locator :testfn)) => :testfn-impl)
         (fact "Trying to set an unknown defaukt implementations will throw"
               (set-default-implementation! locator :testfn :unknown-impl)
               => (throws Exception ":unknown-impl is not a known implementation for :testfn"))
         (fact "Using ^:primary will set default implementation"
               (do (add-implementation! locator :testfn {:primary true} ['testarg] :testfn-impl3)
                   (find-implementation locator :testfn)) => :testfn-impl3
               (-> (find @(:services locator) :testfn)
                   second
                   :default-impl) => :testfn-impl3)))
