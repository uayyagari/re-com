(ns re-demo.datepicker
  (:require
    [goog.date.Date]
    [reagent.core      :as    reagent]
    [reagent.ratom     :refer-macros [reaction]]
    [cljs-time.core    :refer [today days minus plus day-of-week before?]]
    [cljs-time.coerce  :refer [to-local-date]]
    [cljs-time.format  :refer [formatter unparse]]
    [re-com.core       :refer [h-box v-box box gap single-dropdown datepicker datepicker-dropdown checkbox label title p button md-icon-button]]
    [re-com.datepicker :refer [iso8601->date datepicker-dropdown-args-desc]]
    [re-com.validate   :refer [date-like?]]
    [re-com.util       :refer [now->utc]]
    [re-demo.utils     :refer [panel-title title2 args-table github-hyperlink status-text]]))


(def ^:private days-map
     {:Su "S" :Mo "M" :Tu "T" :We "W" :Th "T" :Fr "F" :Sa "S"})


(defn- toggle-inclusion!
  "convenience function to include/exclude member from"
  [set-atom member]
  (reset! set-atom
          (if (contains? @set-atom member)
            (disj @set-atom member)
            (conj @set-atom member))))

(defn- checkbox-for-day
  [day enabled-days]
  [v-box
   :align    :center
   :children [[label
               :style {:font-size "smaller"}
               :label (day days-map)]
              [checkbox
               :model     (@enabled-days day)
               :on-change #(toggle-inclusion! enabled-days day)]]])

(defn- parameters-with
  "Toggle controls for some parameters."
  [content enabled-days disabled? show-today? show-weeks?]
  [v-box
   :gap      "15px"
   :align    :start
   :children [[gap :size "20px"]
              [title :level :level3 :label "Parameters"]
              [h-box
               :gap      "20px"
               :align    :start
               :children [[checkbox
                           :label     [box :align :start :child [:code ":disabled?"]]
                           :model     disabled?
                           :on-change #(reset! disabled? %)]
                          [checkbox
                           :label     [box :align :start :child [:code ":show-today?"]]
                           :model     show-today?
                           :on-change #(reset! show-today? %)]
                          [checkbox
                           :label     [box :align :start :child [:code ":show-weeks?"]]
                           :model     show-weeks?
                           :on-change #(reset! show-weeks? %)]]]
              [h-box
               :gap      "2px"
               :align    :center
               :children [[checkbox-for-day :Su enabled-days]
                          [checkbox-for-day :Mo enabled-days]
                          [checkbox-for-day :Tu enabled-days]
                          [checkbox-for-day :We enabled-days]
                          [checkbox-for-day :Th enabled-days]
                          [checkbox-for-day :Fr enabled-days]
                          [checkbox-for-day :Sa enabled-days]
                          [gap :size "5px"]
                          [box :align :start :child [:code ":selectable-fn"]]]]
              [:span [:code "e.g. (fn [date] (#{1 2 3 4 5 6 7} (day-of-week date)))"]]
              content]])


(defn- date->string
  [date]
  (if (date-like? date)
    (unparse (formatter "dd MMM, yyyy") date)
    "no date"))


(defn- show-variant
  [variation]
  (let [model1          (reagent/atom #_nil  #_(today)                    (now->utc))                      ;; Test 3 valid data types
        model2          (reagent/atom #_nil  #_(plus (today) (days 120))  (plus (now->utc) (days 120)))    ;; (today) = goog.date.Date, (now->utc) = goog.date.UtcDateTime
        model3          (reagent/atom nil)
        disabled?       (reagent/atom false)
        show-today?     (reagent/atom true)
        show-weeks?     (reagent/atom false)
        enabled-days    (reagent/atom (-> days-map keys set))
        as-days         (reaction (-> (map #(% {:Su 7 :Sa 6 :Fr 5 :Th 4 :We 3 :Tu 2 :Mo 1}) @enabled-days) set))
        selectable-pred (fn [date] (@as-days (day-of-week date))) ; Simply allow selection based on day of week.
        label-style     {:font-style "italic" :font-size "smaller" :color "#777"}]
    (case variation
      :inline [(fn inline-fn
                 []
                 [parameters-with
                  [h-box
                   :gap      "20px"
                   :align    :start
                   :children [[v-box
                               :gap      "5px"
                               :children [[label
                                           :style label-style
                                           :label [:span " :maximum - " (date->string @model2) [:br] ":start-of-week - Sunday"]]
                                          [datepicker
                                           :model         model1
                                           :maximum       model2
                                           :disabled?     disabled?
                                           :show-today?   @show-today?
                                           :show-weeks?   @show-weeks?
                                           :selectable-fn selectable-pred
                                           :on-change     #(do #_(js/console.log "model1:" %) (reset! model1 %))]
                                          [label :style label-style :label (str "selected: " (date->string @model1))]
                                          [h-box
                                           :gap      "6px"
                                           :margin   "10px 0px 0px 0px"
                                           :align    :center
                                           :children [[label :style label-style :label "Change model:"]
                                                      [md-icon-button
                                                       :md-icon-name "zmdi-arrow-left"
                                                       :size         :smaller
                                                       :disabled?    (not (date-like? @model1))
                                                       :on-click     #(when (date-like? @model1)
                                                                        (reset! model1 (minus @model1 (days 1))))]
                                                      [md-icon-button
                                                       :md-icon-name "zmdi-arrow-right"
                                                       :size         :smaller
                                                       :disabled?    (if (and (date-like? @model1) (date-like? @model2))
                                                                       (not (before? (to-local-date @model1)
                                                                                     (to-local-date @model2)))
                                                                       true)
                                                       :on-click     #(when (date-like? @model1)
                                                                        (reset! model1 (plus @model1 (days 1))))]
                                                      [button
                                                       :label    "Reset"
                                                       :class    "btn btn-default"
                                                       :style    {:padding  "1px 4px"}
                                                       :on-click #(reset! model1 nil)]]]]]

                              [v-box
                               :gap      "5px"
                               :children [[label
                                           :style label-style
                                           :label [:span ":minimum - " (date->string @model1) [:br] ":start-of-week - Monday"]]
                                          [datepicker
                                           :start-of-week 0
                                           :model         model2
                                           :minimum       model1
                                           :show-today?   @show-today?
                                           :show-weeks?   @show-weeks?
                                           :selectable-fn selectable-pred
                                           :disabled?     disabled?
                                           :on-change     #(do #_(js/console.log "model2" %) (reset! model2 %))]
                                          [label :style label-style :label (str "selected: " (date->string @model2))]]]]]
                  enabled-days
                  disabled?
                  show-today?
                  show-weeks?])]
      :dropdown [(fn dropdown-fn
                   []
                   [parameters-with
                    [h-box
                     :size     "auto"
                     :align    :start
                     :children [[gap :size "120px"]
                                [datepicker-dropdown
                                 :model         model3
                                 :show-today?   @show-today?
                                 :show-weeks?   @show-weeks?
                                 :selectable-fn selectable-pred
                                 :placeholder   "Select a date"
                                 :format        "dd MMM, yyyy"
                                 :disabled?     disabled?
                                 :on-change     #(reset! model3 %)]]]
                    enabled-days
                    disabled?
                    show-today?
                    show-weeks?])])))


(def variations ^:private
  [{:id :inline   :label "Inline"}
   {:id :dropdown :label "Dropdown"}])


(defn datepicker-examples
  []
  (let [selected-variation (reagent/atom :inline)]
    (fn examples-fn []
      [v-box
       :size     "auto"
       :gap      "10px"
       :children [[panel-title "Date Components"
                               "src/re_com/datepicker.cljs"
                               "src/re_demo/datepicker.cljs"]
                  [h-box
                   :gap      "100px"
                   :children [[v-box
                               :gap      "10px"
                               :width    "450px"
                               :children [[title2 "[datepicker ... ] & [datepicker-dropdown ... ]" {:font-size "24px"}]
                                          [status-text "Stable"]
                                          [p "An inline or popover date picker component."]
                                          [args-table datepicker-dropdown-args-desc]]]
                              [v-box
                               :gap       "10px"
                               :size      "auto"
                               :children  [[title2 "Demo"]
                                           [h-box
                                            :gap      "10px"
                                            :align    :center
                                            :children [[label :label "Select a demo"]
                                                       [single-dropdown
                                                        :choices   variations
                                                        :model     selected-variation
                                                        :width     "200px"
                                                        :on-change #(reset! selected-variation %)]]]
                                           [show-variant @selected-variation]]]]]]])))


;; core holds a reference to panel, so need one level of indirection to get figwheel updates
(defn panel
  []
  [datepicker-examples])
