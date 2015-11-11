(ns de.find-method.funsig-test
  (:require [de.find-method.funsig :refer :all]
            [de.find-method.testsigs :as testsig :refer [fetch-multiple]]
            [de.find-method.testimpl1 :as impl1 :refer [fetch-multiple-impl]]
            [de.find-method.testimpl2 :as impl2]
            [midje.sweet :refer :all]))

(fact "Variadic signatures also work"
      (do (defsig fetch-foo ([] [foo] [foo bar]))
          (defimpl fetch-foo ([] 'foo) ([foo] foo) ([foo bar] [foo bar]))
          (fetch-foo 1 2)) => [1 2])

(facts "Handling multiple implementations"
      (fact "After testimpl1 and testimpl2 are loaded, I get the results of the second implementation"
            (fetch-multiple 2) => 'foo2)
      (fact "But I can set a different default implementation"
            (do (set-default-implementation! fetch-multiple fetch-multiple-impl)
                (fetch-multiple 2) => 'foo1)))
