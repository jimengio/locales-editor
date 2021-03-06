
(ns app.client
  (:require [respo.core :refer [render! clear-cache! realize-ssr! *changes-logger]]
            [respo.cursor :refer [update-states]]
            [app.comp.container :refer [comp-container]]
            [cljs.reader :refer [read-string]]
            [ws-edn.client :refer [ws-connect! ws-send!]]
            [app.schema :as schema]
            [app.config :as config]
            [recollect.patch :refer [patch-twig]]
            [cumulo-util.core :refer [on-page-touch]]
            [clojure.string :as string])
  (:require-macros [clojure.core.strint :refer [<<]]))

(declare dispatch!)

(declare connect!)

(declare simulate-login!)

(defonce *states (atom {}))

(defonce *store (atom nil))

(defn simulate-login! []
  (let [raw (.getItem js/localStorage (:storage-key config/site))]
    (if (some? raw)
      (do (println "Found storage.") (dispatch! :user/log-in (read-string raw)))
      (do (println "Found no storage.")))))

(defn dispatch! [op op-data]
  (when (and config/dev? (not= op :states)) (println "Dispatch" op op-data))
  (case op
    :states (reset! *states (update-states @*states op-data))
    :effect/connect (connect!)
    (ws-send! {:kind :op, :op op, :data op-data})))

(defn connect! []
  (ws-connect!
   (<< "ws://localhost:~(:port config/site)")
   {:on-open (fn [] (comment simulate-login!)),
    :on-close (fn [event] (reset! *store nil) (js/console.error "Lost connection!")),
    :on-data (fn [data]
      (case (:kind data)
        :patch
          (let [changes (:data data)]
            (when config/dev? (js/console.log "Changes" (clj->js changes)))
            (reset! *store (patch-twig @*store changes)))
        (println "unknown kind:" data)))}))

(defn log-changes! [changes]
  (println
   (->> changes
        (map (fn [change] (pr-str [(first change) (get change 1)])))
        (string/join "\n"))))

(def mount-target (.querySelector js/document ".app"))

(defn on-keydown! [event]
  (comment js/console.log event)
  (cond
    (and (= (.-key event) "s") (.-metaKey event))
      (do (.preventDefault event) (dispatch! :effect/codegen nil))
    (and (= (.-key event) "i") (.-metaKey event))
      (let [target (js/document.querySelector ".add-button")]
        (js/console.log target)
        (.click target))))

(defn render-app! [renderer]
  (renderer mount-target (comp-container (:states @*states) @*store) dispatch!))

(def ssr? (some? (.querySelector js/document "meta.respo-ssr")))

(defn main! []
  (println "Running mode:" (if config/dev? "dev" "release"))
  (if ssr? (render-app! realize-ssr!))
  (render-app! render!)
  (connect!)
  (add-watch *store :changes #(render-app! render!))
  (add-watch *states :changes #(render-app! render!))
  (on-page-touch (fn [] (when (nil? @*store) (connect!))))
  (.addEventListener js/window "keydown" (fn [event] (on-keydown! event)))
  (comment reset! *changes-logger (fn [old-el new-el changes] (log-changes! changes)))
  (println "App started!"))

(defn reload! [] (clear-cache!) (render-app! render!) (println "Code updated."))
