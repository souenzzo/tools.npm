(ns br.com.souenzzo.tools.npm
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.build.api :as b])
  (:import (java.security MessageDigest DigestInputStream)
           (java.util Base64)
           (java.util.zip GZIPInputStream)
           (org.apache.commons.compress.archivers ArchiveStreamFactory)))

(set! *warn-on-reflection* true)

(defn install
  [{:keys [path]
    :or   {path "."}}]
  (let [base (io/file path)
        {:strs [packages]} (json/read (io/reader (io/file base "package-lock.json")))
        packages (remove #(get % "dev")
                   (dissoc packages ""))]
    (doseq [[target {:strs [resolved integrity] :as pkg}] packages
            :let [targetf (io/file base target)
                  [alg target-b64-digest] (string/split integrity #"-" 2)
                  md (MessageDigest/getInstance ({"sha512" "SHA-512"} alg alg))
                  in (DigestInputStream. (io/input-stream resolved) md)
                  tar (.createArchiveInputStream (ArchiveStreamFactory.) "tar"
                        (GZIPInputStream. in))]]
      (loop []
        (when-let [tar-entry (some-> tar .getNextEntry)]
          (let [entry-target (io/file targetf (string/replace (.getName tar-entry)
                                                #"^package/"
                                                ""))]
            (io/make-parents entry-target)
            (io/copy tar entry-target)
            (recur))))
      (let [dig (slurp (.encode (Base64/getEncoder)
                         (.digest md)))]
        (when-not (= target-b64-digest dig)
          (b/delete {:path (str targetf)})
          (throw (ex-info (str "Dependency " resolved " do not match integrity")
                   {:cognitect.anomalies/category :cognitect.anomalies/unavailable
                    :expected                     target-b64-digest
                    :actual                       dig
                    :pkg                          pkg})))))))
