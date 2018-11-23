
(ns app.comp.workspace
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.comp.space :refer [=<]]
            [respo.core :refer [defcomp <> action-> cursor-> list-> input button span div]]
            [app.config :as config]
            [respo-alerts.comp.alerts :refer [comp-prompt comp-alert]]
            [clojure.string :as string]
            [feather.core :refer [comp-i comp-icon]]
            ["copy-text-to-clipboard" :as copy!])
  (:require-macros [clojure.core.strint :refer [<<]]))

(def style-hint {:display :inline-block, :color (hsl 0 0 80), :margin "0 16px"})

(defcomp
 comp-locale
 (states k v)
 (let [state (or (:data states) {:copied? false})]
   (div
    {:class-name "locale-card",
     :style (merge
             ui/flex
             ui/column
             {:padding "8px 16px",
              :display :inline-flex,
              :background-color :white,
              :margin 8}
             (if (:copied? state)
               {:transform "scale(1.08)", :box-shadow (<< "0 0 3px ~(hsl 0 0 0 0.2)")}))}
    (div
     {:style (merge
              ui/row-parted
              {:min-width 240, :font-family ui/font-code, :overflow :auto})}
     (div
      {:style ui/row-middle}
      (cursor->
       :rename
       comp-prompt
       states
       {:trigger (<> k), :initial k}
       (fn [result d! m!]
         (when (not (string/blank? result)) (d! :locale/rename-one {:from k, :to result}))))
      (=< 8 nil)
      (comp-icon
       :copy
       {:font-size 14, :color (hsl 0 80 80), :cursor :pointer}
       (fn [e d! m!]
         (copy! (<< "lang.~{k}"))
         (m! (assoc state :copied? true))
         (js/setTimeout (fn [] (m! (assoc state :copied? false))) 600))))
     (cursor->
      :remove
      comp-alert
      states
      {:trigger (comp-i :x 14 (hsl 0 80 80)), :text "确认要删除这个字段?"}
      (fn [e d! m!] (d! :locale/rm-one k))))
    (div
     {}
     (<> "zhCN" style-hint)
     (cursor->
      "zhCN"
      comp-prompt
      states
      {:trigger (<> (get v "zhCN")), :initial (get v "zhCN")}
      (fn [result d! m!]
        (when (not (string/blank? result))
          (d! :locale/edit-one {:lang "zhCN", :key k, :text result})))))
    (div
     {}
     (<> "enUS" style-hint)
     (cursor->
      "enUS"
      comp-prompt
      states
      {:trigger (<> (get v "enUS")), :initial (get v "enUS")}
      (fn [result d! m!]
        (when (not (string/blank? result))
          (d! :locale/edit-one {:lang "enUS", :key k, :text result}))))))))

(defcomp
 comp-lang-table
 (states locales total)
 (div
  {:style (merge ui/flex {:overflow :auto, :background-color (hsl 0 0 90), :padding 8})}
  (list->
   {}
   (->> locales
        (sort-by (fn [[k v]] (count k)))
        (map (fn [[k v]] [k (cursor-> k comp-locale states k v)]))))
  (let [size (count locales)]
    (div
     {:style (merge ui/center {:padding 16})}
     (if (= size total)
       (<> (str "all " size " locales are displayed"))
       (<> (str "displaying first " size " of " total " results")))))))

(defcomp
 comp-search-box
 (states query need-save?)
 (let [state (or (:data states) {:text ""})]
   (div
    {:style (merge ui/row-parted {:padding 16})}
    (div
     {:style {}}
     (input
      {:value (:text state),
       :style ui/input,
       :placeholder query,
       :on-input (fn [e d! m!] (m! (assoc state :text (:value e)))),
       :on-keydown (fn [e d! m!]
         (when (= "Enter" (.-key (:event e))) (d! :session/query (:text state))))})
     (=< 16 nil)
     (cursor->
      :add
      comp-prompt
      states
      {:trigger (button {:style ui/button, :inner-text "Create"}),
       :initial (do (println state states) (:text state))}
      (fn [result d! m!]
        (when (not (string/blank? result))
          (d! :locale/add-one result)
          (d! :session/query result)))))
    (button
     {:style (merge ui/button (when need-save? {:background-color :blue, :color :white})),
      :inner-text "Codegen",
      :on-click (fn [e d! m!] (d! :effect/codegen nil))}))))

(defcomp
 comp-workspace
 (states locales query total need-save?)
 (div
  {:style (merge ui/flex ui/column {:overflow :auto})}
  (cursor-> :search comp-search-box states query need-save?)
  (cursor-> :table comp-lang-table states locales total)))
