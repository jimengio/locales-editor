
(ns app.config (:require [app.util :refer [get-env!]]))

(def bundle-builds #{"release" "local-bundle"})

(defn cdn? [] )

(def dev?
  (let [debug? (do ^boolean js/goog.DEBUG)]
    (cond
      (exists? js/window) debug?
      (exists? js/process) (not= "true" js/process.env.release)
      :else true)))

(def site
  {:port 8008,
   :title "多语言编辑",
   :icon "http://cdn.tiye.me/logo/jimeng-360x360.png",
   :dev-ui "http://localhost:8100/main.css",
   :release-ui "http://cdn.tiye.me/favored-fonts/main.css",
   :cdn-url "http://cdn.tiye.me/locales-editor/",
   :cdn-folder "tiye.me:cdn/locales-editor",
   :upload-folder "tiye.me:repo/chenyong/locales-editor/",
   :server-folder "tiye.me:servers/locales-editor",
   :theme "#eeeeff",
   :storage-key "locales-editor",
   :storage-path "locales.edn"})
