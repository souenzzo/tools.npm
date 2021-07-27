(ns br.com.souenzzo.tools.npm-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

(deftest hello
  (is (= 3
        (+ 2 3))))
