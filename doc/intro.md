# Introduction to funsig

## Motivation

[Dependency inversion](https://en.wikipedia.org/wiki/Dependency_inversion_principle), the 'D' in the [SOLID](https://en.wikipedia.org/wiki/SOLID_(object-oriented_design)) acronym, is not exactly a new topic. The principle states the following two rules:

1. High-level modules should not depend on low-level modules. Both should depend on abstractions.
2. Abstractions should not depend on details. Details should depend on abstractions.

Many current architecture recommendations rely on these rules extensively, most notably Bob Martins's [clean architecture](https://blog.8thlight.com/uncle-bob/2012/08/13/the-clean-architecture.html), which is next to identical to the Jeffrey Palermo's [onion architecture](http://jeffreypalermo.com/blog/the-onion-architecture-part-1/).

There are many ways to handle [dependency inversion in Clojure](http://blog.find-method.de/index.php?/archives/209-Dependency-inversion-in-Clojure.html): some are built-in like [protocols](http://http://clojure.org/protocols) or [multimethods](http://http://clojure.org/multimethods), some use libraries like Stuart Sierra's [component library](https://github.com/stuartsierra/component). The funsig library doesn't want to overcome these, but instead seeks to complement them.

Protocols are a great solution if the following two assumptions are true:

1. you have a set of functions with high cohesion, i.e. functions that belong semantically together and
2. you are okay with providing an object (of a type that extends the protocol) at call time.

The second assumption probably requires some more explanation.

```clojure

	(defprotocol Fly
		"A simple protocol for flying"
		(fly [this] "Method to fly"))

	(defrecord Bird [name species]
		Fly
		(fly [this] (str (:name this) " flies...")))
```

In this example, you need to define a _type_ (here via `defrecord`) on which you provide the implementation. This makes sense here, as birds are certainly resonable types. But Clojure is a language in which just using the built-in data types will take you a long way, very often you just might have a function operating on a map or a vector and you might not want to `extend-type` these basic types with all of your protocols just to inverse a dependency.

I'll skip the discussion of multi-methods but address Stuart Sierra's component library next, which is rightfully rather prominent. To quote from the readme file, "component is a tiny Clojure framework for managing the lifecycle and dependencies of software components which have _runtime state_" (emphasis mine). 

Component works great and I seriously recommend it when the dependency you need to manage involves state. It's worth pointing out that a) component itself relies on protocols (the `Lifecycle`) and b) as types can implement multiple protocols, a really nice approach is to combine your own protocol with `component/Lifecycle`. However, not all dependencies you'll encounter in an application involve management of state, quite to the contrary. 


## Enter funsig

funsig shoots lower than both of these nice solutions: it provides dependency management on a per-function level. What this means is simply that you can define a function signature with `defsig` and then provide implementations with `defimpl`. Implementations will depend on the signature. Let's say we have some application code that depends on a `printer` function:

You can then define the signature of the function your application level code has a dependency on with `defsig`:

```clojure

	(ns my.onion)

	(defn printer [string]
		(println string))

	(defn print-account-multiplied [account multiplier]
		(let [result (* account multiplier)]
			(printer result)))
```

If you expect to exchange the dependency on the `printer` implementation, you could do define the signature with `defsig`:

```clojure

	(ns my.onion
		(:require [de.find-method.funsig :as di :refer [defsig defimpl]]))

	(defsig printer [string])

	(defn print-account-multiplied [account multiplier]
		(let [result (* account multiplier)]
			(printer result)))
```

Here's the implementation:

```clojure

	(ns my.onion.simle-printer
		(:require [de.find.method.funsig :as di :refer [defimpl]]
			      [my.onion :as mo :refer [printer]]))

	(defimpl printer [string]
		(println string))
```

Note that the implementation has a dependency on the signature, not the other way around. Also, your application code (`print-account-multiplied`) simply depends on the signature -- here the signature is in the same file, but reference to the var in another namespace (i.e. using `require\:refer`) also works normally, as in this demo taken from funsigs tests:

```clojure

	(ns de.find-method.testimpl3
		(:require [de.find-method.funsig :as di :refer [defimpl]]
			      [de.find-method.testsigs :as testsig :refer [fetch-multiple]]))

	(defimpl testsig/fetch-multiple [foo] 'foo3)
```

Somewhere, you also need to `load` the code for the implementation (typically via `require`). If you consider an application, this could happen in your typical `core.clj` file or whereever you handle the application configuration and/or startup.

When multiple implementations are provided, you can determine a default implementation by setting the `:primary` key as meta data on the implementation, otherwise the last implementation being defined (loaded) will be used.

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

`set-default-implementation!` expects the signature name and the implementation name which consists of the signature name plus `-impl`.

The separation of concerns between definition of the abstraction (the signature) and the implementation allows to break dependencies between modules and functions. Any application code depending on the signature doesn't need to know which implementation is used.

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


## Implementation overview

Although appearing to handle dependency injection, funsig is really based on the [service locator pattern](http://martinfowler.com/articles/injection.html#UsingAServiceLocator). The service locator is hidden with Clojure macros, though.

### Signature and implementation notes

The macros `defsig` and `defimpl` operate on instances of `core/ServiceLocator` which implements the `core/ServiceLocatorProtocol`. ServiceLocator objects use a Clojure atom for managing state about signatures and implementations.

Defining a signature with name `example-name` and argument list `param` via `defsig` will do two things:

- add a signature `example-name` with `params` to the locator
- define a function `example-name` in the current namespace (e.g. `my.app.example-sig`) that will retrieve the default implementation for `example-name` (if any) and apply the given parameters to it.

Similarly, defining an implementation with name `example-name`, argument list `param` and some body will do two things:

- define a function `example-name-impl` in the current namespace (e.g. `my.app.example-impl1`) that takes `params` as an argument list and the body as function body
- adds `my.app.example-impl1/example-name-impl` as an implementation for `example-name` to the locator.

Good macro practice would dictate to generate a unique name for the implementation via `gensym`, but this would not allow for convenient use of the implementation functions name for setting the default implementation via `set-default-implementation!`.

Not surprisingly, defining signatures and implementations involves a lot of side-effects. If you don't like that you may want to take a look at [clj-di](https://github.com/nvbn/clj-di) which uses local bindings instead of global vars (although clj-di also uses an atom internally to manage registered names).

### State management

The top-level module `de.find-method.funsig` is mostly just a small facade redirecting to code in `core` and `macros`. There is the important addition that it defines a dynamic global var `*locator*` that holds _the_ locator on which the above protocol usually runs.

In other words, when you call the top-level `defsig` and `defimpl`, you are operating on `de.find-method.funsig/*locator*`. If you have a need to use a different service locator, you can bind a new one (created via `core/start-new-locator` to it:

```clojure

	(binding [*locator* (start-new-locator)]
		(defsig fetch-foo [foo bar])

	    (defimpl fetch-foo [foo bar]
		   ... implementation code ...
```

This can also be put to good use in tests to provide mock implementations, but be aware that a fresh locator obviously will not know about any signatures.


## Integration with Stuart Sierra's component library

Funsig as a library is largely compatible with Stuart Sierra's [component library](https://github.com/stuartsierra/component) if used with a twist. Which is just another way of saying that, basically, funsig breaks Stuart's recommendations ([notes for library authors](https://github.com/stuartsierra/component#notes-for-library-authors)) in almost every way if used as discussed above: it relies on dynamic binding to convey state and it performs side-effects at the top of a file.

However, this is simply a conveniance provided from the top-level `funsig.clj` module. Instead you can simply call `start-new-locator` from `de.find-method.funsig.core` and hand over the resulting locator to the macros `defsig` and `defimpl` from `de.find-method.funsig.macros`. So, just use the following requirements instead, when using this library as a component:

```clojure

    (ns my.onion.mycomponent
  		(:require [de.find.method.funsig.macros :as di :refer [defsig defimpl]]
			      [de.find.method.funsig.core :as dicore :refer [start-new-locator]]))

	(defrecord MyComponent [locator etc]
	    component/Lifecycle
	    (start [mycomponent]
		       (assoc mycomponent :locator (start-new-locator)))

	    (stop [mycomponent]
              (dissoc mycomponent :locator)))
```

This defines the component. You would then use this locator when defining signatures and implementations (here simply assuming you have a global var `locator` holding a reference to your service locator component, in practice you'll want another indirection that uses the locator from a system configuration): 

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
