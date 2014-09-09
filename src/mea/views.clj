(ns mea.views
  (:use [hiccup core page]))

(defn index-page []
  (html5
   [:head
    [:title "Mea - A Participant Database"]
    (include-css "/css/style.css")]
   [:body
    [:h1 "Welcome!"]]))
