(ns hamelito.hiccup
  (:require [hamelito.doctypes :as doctypes]
            [hamelito.parser   :as parser]
            [hiccup.core       :as hiccup]
            [clojure.string    :as string]))

;; bring in the cond->
(when (< (:minor *clojure-version*) 5)
  (use 'hamelito.util))

(defn- flat-conj
  [vec xs]
  (apply conj vec xs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Hiccup Conversion

(defprotocol ToHiccup
  (-to-hiccup [this]))

(defn element-tag
  [{:keys [name id classes]}]
  (keyword (str (or name "div")
                (when id
                  (str "#" id))
                (when-not (empty? classes)
                  (str "." (string/join "." classes))))))

(defn element->hiccup
  [{:keys [attributes inline-content children] :as element}]
  (cond-> [(element-tag element)]

          (not (empty? attributes))
          (conj attributes)

          (not (string/blank? inline-content)) 
          (conj inline-content)

          children
          (flat-conj (mapv -to-hiccup children))))

(defn comment->hiccup
  [{:keys [text condition children]}]
  (concat (if condition
            ["<!--[" condition "]>"]
            ["<!-- "])
          (when text
            [text])
          (mapv -to-hiccup children)
          (if condition
            ["<![endif]-->"]
            [" -->"])))

(defmulti filtered-block->hiccup :type)

(defmethod filtered-block->hiccup :plain
  [filtered-block]
  (apply str (interpose "\n" (:lines filtered-block))))

(defmethod filtered-block->hiccup :javascript
  [filtered-block]
  [:script (apply str (interpose "\n" (:lines filtered-block)))])

(defmethod filtered-block->hiccup :cdata
  [filtered-block]
  (str "<![CDATA["
       (apply str (interpose "\n" (:lines filtered-block)))
       "]]>"))

(defmethod filtered-block->hiccup :default
  [filtered-block]
  (throw (ex-info (format "Unknown filter type: %s" (:type filtered-block))
                  {:node filtered-block})))

(extend-protocol ToHiccup
  hamelito.parser.Element
  (-to-hiccup [this] (element->hiccup this))

  hamelito.parser.Text
  (-to-hiccup [this] (:text this))

  hamelito.parser.FilteredBlock
  (-to-hiccup [this] (filtered-block->hiccup this))
  
  hamelito.parser.Comment
  (-to-hiccup [this] (comment->hiccup this))
  
  hamelito.parser.Doctype
  (-to-hiccup [this] (doctypes/lookup-doctype :html5 (:value this)))
  
  hamelito.parser.Document
  (-to-hiccup [this] (concat
                      (mapv -to-hiccup (:doctypes this))
                      (mapv -to-hiccup (:elements this)))))

(defn- to-hiccup
  [parse-tree]
  (-to-hiccup parse-tree))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn hiccup
  "Returns hiccup data from the given haml-source. A haml-source is
  anything that satisfies the CharSeq protocol, typically a String or
  a Reader."
  [haml-source]
  (let [parse-tree (parser/parse-tree haml-source)]
    (to-hiccup parse-tree)))

(defn html
  "Returns a string with the result of rendering the Hiccup data
  generated by the hiccup function."
  [haml-source]
  (hiccup/html (seq (hiccup haml-source))))
