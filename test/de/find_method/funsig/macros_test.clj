(ns de.find-method.funsig.macros-test
  (:require [midje.sweet :refer :all]
            [de.find-method.funsig.core :as si :refer :all]
            [de.find-method.funsig.macros :refer :all]))

(facts "Defining signatures"
       (let [locator (start-new-locator)]
         (fact "After signature definition, a function is defined"
               (do (defsig locator fetch-foo [foo bar])
                   (and (bound? #'fetch-foo)
                        (fn? fetch-foo))) => true)
         (fact "Add documentation per  signature definition, a function is defined"
               (do (defsig locator fetch-foo "Fetch foo" [foo bar])
                   (:doc (meta #'fetch-foo))) => "Fetch foo")
         (fact "Calling a signature that doesn't have an implementation will throw an exception"
               (do (defsig locator fetch-foo [foo bar])
                   (fetch-foo 1 2) => (throws Exception "No implementation registered for de.find-method.funsig.macros-test/fetch-foo")))))

(facts "Defining implementations"
       (let [locator (start-new-locator)]
         (fact "Adding implementations without signature will throw an exception"
               (defimpl locator my-fetch-foo [foo bar] 'this-will-not-work) => (throws Exception "No signature registered for my-fetch-foo"))
         (fact "Defining an implementation will define an implementation function"
               (do (defsig locator fetch-foo1 [foo bar])
                   (defimpl locator fetch-foo1 [foo bar] [foo bar])
                   (and (bound? #'fetch-foo1-impl)
                        (fn? fetch-foo1-impl))) => true)
         (fact "Defining an implementation for a signature requires matching lambdalists"
               (do (defsig locator fetch-foo2 [foo bar baz])
                   (defimpl locator fetch-foo2 [foo bar] [foo bar]))
               => (throws Exception
                          "Lambda lists for de.find-method.funsig.macros-test/fetch-foo2 don't match: ([foo bar baz])!=([foo bar])"))
         (fact "Invoking an implementation for a signature by calling the signature works"
               (do (defsig locator fetch-foo3 [foo bar])
                   (defimpl locator fetch-foo3 [foo bar] [foo bar])
                   (fetch-foo3 1 2)) => [1 2])
         (fact "Argument destructuring works as normally"
               (do (defsig locator fetch-foo4 [foo bar & {:keys [baz] :or {baz 20}}])
                   (defimpl locator fetch-foo4 [foo bar & {:keys [baz] :or {baz 20}}] [foo bar baz])
                   (fetch-foo4 1 2)) => [1 2 20])
         (fact "Variadic signatures also work"
               (do (defsig locator fetch-foo5 ([] [foo] [foo bar]))
                   (defimpl locator fetch-foo5 ([] 'foo) ([foo] foo) ([foo bar] [foo bar]))
                   (fetch-foo5 1 2)) => [1 2])))
