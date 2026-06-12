(ns reitit.ring.middleware.defaults.multipart
  "IMPLEMENTATION NOTE:
   Reitit's multipart middleware parses and coerces in a single wrap, but
   the two halves have contradictory placement requirements: parsing must
   happen before the `nested-params`, `keyword-params` and `anti-forgery`
   middleware, which expect multipart params merged into `:params`, while
   the coercion result only survives after `coerce-request-middleware`,
   which overwrites `:parameters` wholesale (see the note added in 0.10.0
   to `reitit.ring.middleware.multipart`). We split it into two separate
   middlewares — `multipart-params-middleware` does parsing via Ring and
   `multipart-coercion-middleware` coerces the same way Reitit does it."
  (:require [reitit.coercion :as rc]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]))

(def multipart-params-middleware
  "Parses multipart params into `:multipart-params` and `:params`, like
   the stock `ring.middleware.multipart-params/wrap-multipart-params`.

   Parsing only: coercion into `[:parameters :multipart]` is done by the
   `multipart-coercion-middleware`, mounted after request coercion.

   Splitting into separate parsing and coercion steps also lets coercion
   errors surface inside `coercion/coerce-exceptions-middleware` as 400s
   instead of escaping the chain as 500s."
  {:name ::multipart-params
   :compile (fn [data _]
              (when-let [opts (get-in data [:defaults :params :multipart])]
                (if (true? opts)
                  wrap-multipart-params
                  #(wrap-multipart-params % opts))))})

(defn- coerced-request [request coercers]
  (if-let [coerced (if coercers (rc/coerce-request coercers request))]
    (update request :parameters merge coerced)
    request))

;; The impl is taken from `reitit.ring.middleware.multipart` mostly verbatim
(def multipart-coercion-middleware
  "Coerces the multipart params pre-parsed by `multipart-params-middleware`
   into `[:parameters :multipart]`.

   Coercion only: the request body is not touched. Must be mounted after the
   `coercion/coerce-request-middleware`, which overwrites `:parameters` and
   would discard anything merged into it earlier.

   It mounts only when multipart params are enabled in the `:defaults` route
   data and the route has a `:multipart` params schema."
  {:name ::multipart-coercion
   :compile (fn [{:keys [parameters coercion] :as data} route-opts]
              (when (and (get-in data [:defaults :params :multipart])
                         (:multipart parameters))
                (let [parameter-coercion {:multipart (rc/->ParameterCoercion
                                                      :multipart-params :string true true)}
                      opts (assoc route-opts ::rc/parameter-coercion parameter-coercion)
                      coercers (rc/request-coercers coercion parameters opts)]
                  {:data {:swagger {:consumes ^:replace #{"multipart/form-data"}}}
                   :wrap (fn [handler]
                           (fn
                             ([request]
                              (-> request
                                  (coerced-request coercers)
                                  (handler)))
                             ([request respond raise]
                              (-> request
                                  (coerced-request coercers)
                                  (handler respond raise)))))})))})
