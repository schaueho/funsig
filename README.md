# funsig

funsig is a Clojure library designed to inject function implementations for function signatures.

Clojure provides multiple ways of inversing dependencies. Some are built-in, like multi-methods or protocols, some use libraries, like Stuart Sierra's component library. None of these, however, handle the most basic problem where some function depends on a single other function. funsig allows you to define a function signature independently from the implementation. Once you provide an implementation, you can simply refer to the signature of the function to use it, thereby inversing the dependency.

## Installation

For the latest release, add the following dependency to your `project.clj`:

[![Clojars Project](http://clojars.org/de.find-method/funsig/latest-version.svg)](http://clojars.org/de.find-method/funsig)

## Usage

Funsig assumes that you define the signature and afterwards the implementation. Do use this, require the two main macros `defsig` and `defimpl`:

```clojure

	(ns my.onion
		(:require [de.find-method.funsig :as di :refer [defsig defimpl]]))

```

You can then define the signature of the function your application level code has a dependency on with `defsig`:

```clojure

	(defsig printer [string])

	(defn print-account-multiplied [account multiplier]
		(let [result (* account multiplier)]
			(printer result)))
```

You also need to supply an implementation with `defimpl`:

```clojure

	(ns my.onion.printer
		(:require [de.find.method.funsig :as di :refer [defimpl]]
			      [my.onion :as mo :refer [printer]]))

	(defimpl printer [string]
		(println string))
```

Note that the implementation has a dependency on the signature, not the other way around -- your application code (`print-account-multiplied`) simply depends on the signature. Obviously, you need to load (require) the code defining the implementation somewhere -- if you get an error calling `print-account-multiplied` telling you there is no implementation for `printer`, this tells you that you never loaded `my.onion.printer`.

An important thing to know is that the parameter list of the signature and the implementation need to agree -- currently, _agreement_ means equality, not compatibility. Hence, both signature and implementation have the exact same parameter list `[string]`.

Argument destructuring and variadic function (implementations) are supported, but again, note that the argument lists need to be _equal_ currently. You can also provide docstrings and an argument map that will be added as meta-data as per `defn`.

```clojure

	(defsig another-sig "Expects one or two arguments" ([] [arg1]))

    (defimpl another-sig
		([]
			(println "No argument received"))
		([arg1]
			(println "One argument received")
			arg1))
```


### Handling multiple implementations of a signature

If you have multiple implementations for a signature in different namespaces, you should explicitly declare which implementation you want, otherwise the load order of the modules will determine which default implementation you get (the `defimpl` loaded last will win). You can determine a default implementation by setting the `:primary` key as meta data on the implementation:

```clojure

	(ns my.onion.fancy-printer
		(:require [de.find.method.funsig :as di :refer [defimpl]]
			      [my.onion :as mo :refer [printer]]))

	(defimpl ^:primary printer [string]
		(println "Fancy print" string))
```

Alternatively, if you don't want to specify the default implementation with the definition itself, you can set a default implementation via `set-default-implementation!`, like so:

```clojure

	(ns my.onion.app
		(:require [de.find-method.funsig :as di :refer [set-default-implementation!]]
			      [my.onion.printersig :refer [printer]]
				  [my.onion.fancy-printer :refer [printer-impl]]))

	(set-default-implementation! printer printer-impl)
```

You can do this with `set-default-implementation!` which expects the name of the signature and the name of the implementation -- the latter consists of the name of the signature plus `-impl`:

```clojure

	(ns my.onion.app
		(:require [de.find-method.funsig :as di :refer [set-default-implementation!]]
			      [my.onion.printersig :refer [printer]]
				  [my.onion.printerimpl2 :as impl2 :refer [printer-impl]]))

	(set-default-implementation! printer impl2/printer-impl)
```

Using a namespace prefix in front of the implementation function is good practice here.

You could use this feature to easily supply a different implementation in tests, e.g. for mock purposes.


### Using clean slate in tests

Funsig holds state information about signatures and implementations. Adding a new (mock) implementation for a signature can lead to pollution of the state that you might want to avoid in your tests.

Funsig is nothing more but a small set of macros plus a ServiceLocator record. If you just want to test a pair of signature and implementation without polluting the global service locator, simply bind `de.find-method.funsig/*locator*` to a freshly started locator (started via `start-new-locator`) like this (using [midje](https://github.com/marick/Midje) here):

```clojure

	(ns my.onion.fetchsig-test
		(:use midje.sweet)
		(:require [de.find.method.funsig :as di :refer [defsig defimpl *locator*]]
			      [de.find.method.funsig.core :as dicore :refer [start-new-locator]]))

	(fact "Invoking an implementation for a signature by calling the signature works"
		(binding [*locator* (start-new-locator)]
			(do (defsig fetch-foo [foo bar])
				(defimpl fetch-foo [foo bar] [foo bar])
					(fetch-foo 1 2)) => [1 2]))
```

With a new `*locator*` binding, you'll need to call both `defsig` and `defimpl`.

## More details

If you need some more details about the internals, refer to the [intro document](https://github.com/schaueho/funsig/blob/master/doc/intro.md).


## License

Copyright © 2015 Holger Schauer <holger.schauer@gmx.de>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
