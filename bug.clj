(ns bug
  (:require [clojure.spec.alpha :as s]
            [reitit.coercion.spec]
            [reitit.ring :as ring]
            [reitit.ring.coercion]))

(s/def ::a string?)
(s/def ::A (s/keys :req-un [::a]))
(s/def ::b vector?)
(s/def ::B (s/keys :req-un [::b]))

(def route
  (ring/ring-handler
   (ring/router
    [["/test" {:post {:parameters {:body (s/or :A ::A :B ::B)}
                      :handler    identity}}]]
    {:data {:coercion   reitit.coercion.spec/coercion
            :middleware [reitit.ring.coercion/coerce-exceptions-middleware
                         reitit.ring.coercion/coerce-request-middleware]}})))

(update-in (route {:body-params    {:b 23}
                   :request-method :post
                   :uri            "/test"})
           [:body :spec] clojure.edn/read-string)

(comment {:status 400,
          :body
          {:spec
           (spec-tools.core/spec
            {:spec (clojure.spec.alpha/or :A :bug/A :B :bug/B),
             :type [:or [:map]],
             :leaf? true}),
           :problems
           [{:path [:A],
             :pred "(clojure.core/fn [%] (clojure.core/contains? % :a))",
             :val {},
             :via [:bug/A],
             :in []}
            {:path [:B],
             :pred "(clojure.core/fn [%] (clojure.core/contains? % :b))",
             :val {},
             :via [:bug/B],
             :in []}],
           :type :reitit.coercion/request-coercion,
           :coercion :spec,
           :value {:b 23},
           :in [:request :body-params]}})
