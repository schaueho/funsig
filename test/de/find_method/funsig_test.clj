(ns de.find-method.funsig-test
  (:require [de.find-method.funsig :refer :all]
            [midje.sweet :refer :all]))

(fact "Variadic signatures also work"
      (do (defsig fetch-foo ([] [foo] [foo bar]))
          (defimpl fetch-foo ([] 'foo) ([foo] foo) ([foo bar] [foo bar]))
          (fetch-foo 1 2)) => [1 2])
