# funsig

funsig is a Clojure library designed to inject function implementations for function signatures.

Clojure provides multiple ways of inversing dependencies. Some are built-in, like multi-methods or protocols, some use libraries, like Stuart Sierra's component library. None of these, however, handle the most basic problem where some function depends on a single other function. funsig allows you to define a function signature independently from the implementation. Once you provide an implementation, you can simply refer to the signature of the function to use it, thereby inversing the dependency.

## Installation

For the latest *alpha* release, add the following dependency to your `project.clj`:

```clojure

	[de.find-method/funsig "0.1.0"]
```


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

Argument destructuring and variadic function (implementations) are supported, but again, note that the argument list need to be _equal_ currently.

### Handling multiple implementations of a signature

If you have multiple implementations for a signature in different namespaces, you should explicitly declare which implementation you want, otherwise the load order of the modules will determine which default implementation you get (the `defimpl` loaded last will win). You can do this with `set-default-implementation!` which expects the name of the signature and the name of the implementation -- the latter consists of the name of the signature plus `-impl`:

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

Funsig is nothing more but a small set of macros plus a ServiceLocator record. If you just want to test a pair of signature and implementation without polluting the global service locator, simply bind `de.find-method.funsig/*locator*` to a freshly started locator (started via `start-new-locator`) like this:

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

Note that with a new `*locator*` binding, you need to call both `defsig` and `defimpl`.


### Integration with Stuart Sierra's component library

Funsig as a library is largely compatible with Stuart Sierra's [component library](https://github.com/stuartsierra/component) if used with a twist. Which is just another way of saying that, basically, funsig breaks Stuarts recommendations ([notes for library authors](https://github.com/stuartsierra/component#notes-for-library-authors)) in almost every way if used as discussed above: it relies on dynamic binding to convey state and it performs side-effects at the top of a file.

However, this is simply a conveniance provided from the top-level `funsig.clj` module. Instead you can simply call `start-new-locator` from `de.find-method.funsig.core` and hand over the resulting locator to the macros `defsig` and `defimpl` from `de.find-method.funsig.macros`. So, just use the following requirements instead, when using this library as a component:

```clojure

	(:require [de.find.method.funsig.macros :as di :refer [defsig defimpl]]
		      [de.find.method.funsig.core :as dicore :refer [start-new-locator]]))

	(defrecord MyComponent [locator etc]
	    component/Lifecycle
	    (start [mycomponent]
		       (assoc mycomponent :locator (start-new-locator)))

	    (stop [mycomponent]
              (dissoc mycomponent :locator)))
```

This defines the component. You would then use this locator when defining signatures and implmentations (here simply assuming you have a global var `locator` holding a reference to your service locator): 

```clojure

    (ns my.onion.appcode
		(:require [de.find.method.funsig.macros :as di :refer [defsig]]))

    ;;;... application code using the macros directly ...
	(defsig locator fetch-foo [foo bar])

    (ns my.onion.fetch-impl
		(:require [de.find.method.funsig.macros :as di :refer [defimpl]]))

	(defimpl locator fetch-foo [foo bar] [foo bar]
		(fetch-foo 1 2))
```

### Todo

- Provide an example with docstring and metadata.


## License

Copyright © 2015 Holger Schauer <holger.schauer@gmx.de>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
