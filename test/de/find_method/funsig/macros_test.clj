(ns de.find-method.funsig.macros-test
  (:require [com.stuartsierra.component :as component]
            [midje.sweet :refer :all]
            [de.find-method.funsig.core :as si :refer :all]
            [de.find-method.funsig.macros :refer :all]))

(facts "Defining signatures"
       (fact "After signature definition, a function is defined"
             (do (defsig fetch-foo [foo bar])
                 (and (bound? #'fetch-foo)
                      (fn? fetch-foo))) => true)
       (fact "Calling a signature that doesn't have an implementation will throw an exception"
             (do (defsig fetch-foo [foo bar])
                 (fetch-foo 1 2) => (throws Exception "No implementation registered for fetch-foo"))))

(facts "Defining implementations"
       (fact "Adding implementations without signature will throw an exception"
             (defimpl my-fetch-foo [foo bar] 'this-will-not-work) => (throws Exception "No signature registered for my-fetch-foo"))
       (fact "Defining an implementation will define an implementation function"
              (do (defsig fetch-foo [foo bar])
                  (defimpl fetch-foo [foo bar] [foo bar])
                  (and (bound? #'fetch-foo-impl)
                       (fn? fetch-foo-impl))))
       (fact "Defining an implementation for a signature requires matching lambdalists"
              (do (defsig fetch-foo [foo bar baz])
                  (defimpl fetch-foo [foo bar] [foo bar]))
              => (throws Exception
                         "Lambda lists for fetch-foo don't match: [foo bar baz]!=[foo bar]"))
       (fact "Invoking an implementation for a signature by calling the signature works"
              (do (defsig fetch-foo [foo bar])
                  (defimpl fetch-foo [foo bar] [foo bar])
                  (fetch-foo 1 2)) => [1 2])
       (fact "Argument destructuring works as normally"
             (do (defsig fetch-foo [foo bar & {:keys [baz] :or {baz 20}}])
                  (defimpl fetch-foo [foo bar & {:keys [baz] :or {baz 20}}] [foo bar baz])
                  (fetch-foo 1 2)) => [1 2 20])
       (fact "Variadic signatures also work"
             (do (defsig fetch-foo ([] [foo] [foo bar]))
                 (defimpl fetch-foo ([] 'foo) ([foo] foo) ([foo bar] [foo bar]))
                 (fetch-foo 1 2)) => [1 2]))
