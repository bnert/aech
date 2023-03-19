(ns aech.decompression
  (:require
    [clojure.string :as str])
  (:import
    [java.io BufferedInputStream
             InputStream]
    [java.util.zip GZIPInputStream
                   InflaterInputStream
                   ZipException
                   Inflater]))

(defn inflate
  "Returns a zlib inflate'd version of the given byte array
  or InputStream."
  [b]
  (when b
    ;; This weirdness is because HTTP servers
    ;; lie about what kind of deflation
    ;; they're using, so we try one way, then if that doesn't work,
    ;; reset and try the other way
    (let [stream (BufferedInputStream. b)
          _ (.mark stream 512)
          iis (InflaterInputStream. stream)
          readable? (try (.read iis) true
                         (catch ZipException _ false))
          _ (.reset stream)
          iis' (if readable?
                 (InflaterInputStream. stream)
                 (InflaterInputStream. stream (Inflater. true)))]

      iis')))

(defn gunzip
  "Returns a gunzip'd version of the given byte array or input stream."
  [b]
  (when b
    (when (instance? InputStream b)
      (GZIPInputStream. b))))


(defn apply-decompress-seq
  "Allows for deflating compression in the order a server applied it"
  [resp enc-seq]
  (reduce
    (fn [resp' encoding]
      (case encoding
        "gzip"    (update resp' :body gunzip)
        "deflate" (update resp' :body inflate)
        resp'))
    resp
    enc-seq))

(defn decompress-body [resp]
  (if-let [content-encoding (get-in resp [:headers "content-encoding"])]
    (let [enc? (map str/trim (str/split content-encoding #","))]
      (if (seq enc?)
        (apply-decompress-seq resp enc?)
        resp))
    resp))

