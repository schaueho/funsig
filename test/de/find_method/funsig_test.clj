(ns de.find-method.funsig-test
  (:require [de.find-method.funsig :as di :refer :all]
            [de.find-method.testsigs :as testsig :refer [fetch-multiple]]
            [de.find-method.testimpl1 :as impl1 :refer [fetch-multiple-impl]]
            [de.find-method.testimpl2 :as impl2]
            [de.find-method.testimpl3 :as impl3]
            [midje.sweet :refer :all]))

(fact "Variadic signatures also work"
      (do (defsig fetch-foo ([] [foo] [foo bar]))
          (defimpl fetch-foo ([] 'foo) ([foo] foo) ([foo bar] [foo bar]))
          (fetch-foo 1 2)) => [1 2])

(facts "Handling multiple implementations"
      ;; (fact "After testimpl1, testimpl2 and templ3 are loaded, I should get the results of the third implementation"
       ;;       (fetch-multiple 2) => 'foo1)
       ;; -- lein midje scans source files in a different order from repl or
       ;;    lein midje :autotest, so disabled.
      (fact "But I can set a different default implementation"
            (do (set-default-implementation! fetch-multiple impl1/fetch-multiple-impl)
                (testsig/fetch-multiple 2) => 'foo1))
      (fact "I can use namespaces and aliases, too"
            (do (set-default-implementation! fetch-multiple impl3/fetch-multiple-impl)
                (de.find-method.testsigs/fetch-multiple 2)) => 'foo3))
