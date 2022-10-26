# reitit-ring-defaults

[![cljdoc](https://cljdoc.org/badge/com.fbeyer/reitit-ring-defaults)][cljdoc]
[![Clojars](https://img.shields.io/clojars/v/com.fbeyer/reitit-ring-defaults.svg)][clojars]

Lifts [ring-defaults] middleware into data-driven middleware, providing
sensible defaults per route.

## Rationale

Reitit provides advanced middleware capabilities and comes with a small number
of "standard" middleware.  For many common tasks such as session management,
additional middleware is required.

Many people use `wrap-defaults` from [ring-defaults] as a sensible default
chain.  However, some middleware included in `site-defaults` will conflict with
an idiomatic Reitit setup:

- Static resources with `create-resource-handler` and `create-file-handler`
- Parameter and multi-part handling

The result is a sub-optimal performance and developer's confusion when requests
are not handled as expected.

Furthermore, it is not obvious at which place in Reitit's configuration
`wrap-defaults` should go.

## Installation

deps.edn:

```
com.fbeyer/reitit-ring-defaults {:mvn/version "0.1.0"}
```

Leiningen/Boot:

```
[com.fbeyer/reitit-ring-defaults "0.1.0"]
```

## Basic usage

The `ring-defaults-middleware` is designed as a replacement for `wrap-defaults`.
In order to have any effect, you will need to add a `:defaults` key to your
route data.  A good starting point is one of the curated configurations
provided by ring-defaults, such as `api-defaults`:

```clojure
(require '[reitit.ring :as ring]
         '[reitit.ring.middleware.defaults :refer [ring-defaults-middleware]]
         '[ring.middleware.defaults :refer [api-defaults]])

;; ...

(def app
  (ring/ring-handler
    (ring/router
      ["/api"
       {:middleware ring-defaults-middleware
        :defaults   api-defaults}
       ["/ping" handler]
       ["/pong" handler]]
      routes)))
```

You can treat the `:defaults` data like any other route data: You can specify
it globally or per-route, and route configurations will be merged with parent
configurations.

Any middleware not configured per route will not mount, and have zero runtime
impact.

## Extended defaults

Reitit provides excellent support for content negotiation and coercion.

Since it is very common to use these, and opting out has no runtime penalty,
there is a `defaults-middleware` that includes additional Reitit middleware
as a sensible default.

## Warning on Session Middleware

Like `wrap-defaults`, reitit-defaults-middleware includes Ring's `wrap-session`,
and using its default configuration in Reitit route data will have surprising
effects, as [Reitit will mount one instance per route][reitit-session-issue].

The recommended solution with reitit-defaults-middleware is to explicitly
configure a session store.  Since the default in-memory store will not be
suitable for non-trivial production deployments, you will want to do that anyway.

```clojure
(require '[ring.middleware.session.memory :as memory])

;; single instance
(def session-store (memory/memory-store))

;; inside, with shared store
(def app
  (ring/ring-handler
    (ring/router
      ["/api"
       {:middleware ring-defaults-middleware
        :defaults   (-> site-defaults
                        (assoc-in [:session :store] session-store))}
       ["/ping" handler]
       ["/pong" handler]])))
```

## License

Copyright 2022 Ferdinand Beyer.  
Distributed under the [MIT License](LICENSE).

[cljdoc]: https://cljdoc.org/jump/release/com.fbeyer/reitit-ring-defaults
[clojars]: https://clojars.org/com.fbeyer/reitit-ring-defaults
[reitit]: https://github.com/metosin/reitit
[ring-defaults]: https://github.com/ring-clojure/ring-defaults
[reitit-session-issue]: https://github.com/metosin/reitit/issues/205
