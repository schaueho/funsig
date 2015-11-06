(ns de.find-method.sigh.macros-test
  (:require [com.stuartsierra.component :as component]
            [midje.sweet :refer :all]
            [de.find-method.sigh.core :as si :refer :all]
            [de.find-method.sigh.macros :refer :all]))

(def test-locator (component/start (->ServiceLocator nil)))

(facts "Defining signatures"
       (fact "Macro expansion"
             (do (defsig fetch-foo [foo bar] :locator test-locator)
                 (fetch-foo 1 2)) => [1 2]))
