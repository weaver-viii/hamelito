# hamelito

[![Build Status](https://travis-ci.org/ragnard/hamelito.png?branch=master)](https://travis-ci.org/ragnard/hamelito)

> As the younger, distant cousin to [HAML](http://haml.info/), Hamelito
> looks up to his handsome relative, but has different goals in life.

hamelito is a [Clojure](http://www.clojure.org) library implementing a
small subset of HAML. Parsing is done using the completely awesome
parser combinator library [kern](https://github.com/blancas/kern/) by
Armando Blancas.

The main goal is to allow HAML documents to be used as templates for
[enlive](http://github.com/cgrand/enlive) and the plan is to support
only the features of HAML that make sense for generating static
documents.

## Status

Early days.

Things that should work:
- Basic element content:
  - `%element`, `.class`, `#id` and valid combinations thereof
- Inline content:
  - `%h1 Hello`
- Text content
  - `Blahonga`
    - Probably doesn't preserve white space the way you would expect
- Arbitrarily, valid, nested tags/content
- Attributes
  - Ruby style: `%a{:href => 'www.xkcd.org'}`
    - Single and double quoted values
  - HTML style: `%a(href='www.xkcd.org')`
    - Unquoted, single and double quoted values
- Comments
  - Html
  - Conditional
- Doctypes 
  - Only `!!!` is currently supported an will yield HTML5 doctype

HTML translation [tests](https://github.com/ragnard/hamelito/blob/master/test/hamelito/html_test.clj).

Things todo/investigate:
- Better errors
- Be more strict with valid identifiers when parsing
- Escaping
- Comments
  - Silent
- Whitespace handling/preservation
- More doctypes, especially !!! XML with encoding
- Filters?
  - Maybe these make sense:
    - :plain
    - :cdata
    - :css
    - :escaped
    - :javascript

## Usage

The main public API is in `hamelito.api`;

```clojure
(require '[hamelito.api :as haml])

;; The html function takes a HAML source and returns an HTML string

(haml/html "%h1 Hola!")
;; => "<h1>Hola!</h1>"

;; A HAML source is anyhting that implements the CharSeq protocol, ie.
;; can provide seq of chars, for example Reader:

(haml/html (java.io.StringReader. "%h1 Hola!"))
;; => "<h1>Hola!</h1>"

;; Hamelito parses HAML into Hiccup data and leans on Hiccup to do the
;; rendering. You can also ask for the Hicucp data:

(haml/hiccup "%h1 Hola")
;; => ([:h1 "Hola"])

;; Hamelito can also return enlive data:

(haml/enlive-node-seq "%h1 Hola")
;; => ({:content ["Hola"], :attrs {}, :tag :h1})
```

### enlive

As of version 1.1.1, enlive supports pluggable parsers. The `hamelito.api/enlive-parser` 
function implements the pluggable parser interface, and can be used like so:

```clojure
(require '[net.cgrand.enlive-html :as enlive])
(require '[hamelito.api :as haml])

(in-ns 'test)

;; To use HAML for all templates in the current namespace:

(enlive/set-ns-parser! haml/enlive-parser)

(deftemplate haml-template "template.haml"
  [])


;; To use HAML for a specific template/snippet:

(deftemplate haml-template {:parser haml/enlive-parser}
  (java.io.StringReader. "%h1 Hello")
  [])

```


A `haml-resource` utility function is provided to simplify using HAML
templates on the classpath with enlive:

```clojure
(require '[net.cgrand.enlive-html :as enlive])
(use '[hamelito.api :only [haml-resource]])

(defsnippet snippet1 (haml-resource "page1.haml")
  [:body :h1] (enlive/content "Hola!"))

(deftemplate page1 (haml-resource "page1.haml")
  [:head :title] (enlive/content "Welcome!"))
```

## License

Copyright © 2013 Ragnar Dahlen

Distributed under the Eclipse Public License, the same as Clojure.
