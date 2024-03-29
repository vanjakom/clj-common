(ns clj-common.2d
  (:import javax.imageio.ImageIO)
  (:require
   [clj-common.io :as io]))

; structures
; point
;    :x
;    :y

; java 2d coordinate system
; upper left corner is (0,0)


(def color-white {:red (float 1.0) :green (float 1.0) :blue (float 1.0) :alpha (float 1.0)})
(def color-black {:red (float 0.0) :green (float 0.0) :blue (float 0.0) :alpha (float 1.0)})
(def color-green {:red (float 0.0) :green (float 1.0) :blue (float 0.0) :alpha (float 1.0)})
(def color-blue {:red (float 0.0) :green (float 0.0) :blue (float 1.0) :alpha (float 1.0)})
(def color-yellow {:red (float 1.0) :green (float 1.0) :blue (float 0.0) :alpha (float 1.0)})
(def color-magenta {:red (float 1.0) :green (float 0.0) :blue (float 1.0) :alpha (float 1.0)})
(def color-red {:red (float 1.0) :green (float 0.0) :blue (float 0.0) :alpha (float 1.0)})
(def color-transparent {:red (float 1.0) :green (float 1.0) :blue (float 1.0) :alpha (float 0.0)})

(defn random-color [] {:red (rand) :green (rand) :blue (rand) :alpha (float 1.0)})

(defn create-image-context [width height]
  (new
    java.awt.image.BufferedImage
    width
    height
    java.awt.image.BufferedImage/TYPE_INT_ARGB))

(defn input-stream->image-context [input-stream]
  (ImageIO/read input-stream))
(def input-stream->image input-stream->image-context)

(defn context-width [image-context]
  (.getWidth image-context))

(defn context-height [image-context]
  (.getHeight image-context))

(defn color
  ([red green blue]
   (color red green blue 1.0))
  ([red green blue alpha]
   {
    :red (float (/ red 255))
    :green (float (/ green 255))
    :blue (float (/ blue 255))
    :alpha (float (/ alpha 255))}))

(defn hex->color
  "Converts hex color representation ( HTML ) to color used in draw methods"
  [hex-string]
  (let [color (java.awt.Color/decode hex-string)]
    {
     :red (/ (float (.getRed color)) 255)
     :green (/ (float (.getGreen color)) 255)
     :blue (/ (float (.getBlue color)) 255)
     :alpha (/ (float (.getAlpha color)) 255)}))

#_(hex->color "#ff0000") ;; {:red 1.0, :green 0.0, :blue 0.0, :alpha 1.0}
#_(hex->color "#0000FF") ;; {:red 0.0, :green 0.0, :blue 1.0, :alpha 1.0}

(defn color->awt-color [color]
  (new
   java.awt.Color
   ^float (:red color)
   ^float (:green color)
   ^float (:blue color)
   ^float (:alpha color)))

(defn offset-point [point x-offset y-offset]
  {
    :x (+ (:x point) x-offset)
    :y (+ (:y point) y-offset)})

(defn fill-poly [image-context points color]
  (let [x-coords (int-array (map :x points))
        y-coords (int-array (map :y points))
        color (color->awt-color color)
        graphics (.getGraphics image-context)]
    (.setColor graphics color)
    (.fillPolygon graphics x-coords y-coords (count x-coords))
    (.dispose graphics)))

(defn draw-poly [image-context points color]
  (let [x-coords (int-array (map :x points))
        y-coords (int-array (map :y points))
        color (color->awt-color color)
        graphics (.getGraphics image-context)]
    (.setColor graphics color)
    (.drawPolygon graphics x-coords y-coords (count x-coords))
    (.dispose graphics)))

(defn draw-polyline [image-context points color]
  (let [x-coords (int-array (map :x points))
        y-coords (int-array (map :y points))
        color (color->awt-color color)
        graphics (.getGraphics image-context)]
    (.setColor graphics color)
    (.drawPolyline graphics x-coords y-coords (count x-coords))
    (.dispose graphics)))

(defn draw-polyline [image-context points color width]
  (let [x-coords (int-array (map :x points))
        y-coords (int-array (map :y points))
        color (color->awt-color color)
        graphics (.getGraphics image-context)]
    (.setColor graphics color)
    (.setStroke graphics
                (new java.awt.BasicStroke
                     width
                     java.awt.BasicStroke/CAP_ROUND
                     java.awt.BasicStroke/JOIN_ROUND))
    (.setRenderingHint graphics
                       java.awt.RenderingHints/KEY_ANTIALIASING
                       java.awt.RenderingHints/VALUE_ANTIALIAS_ON)
    (.drawPolyline graphics x-coords y-coords (count x-coords))
    (.dispose graphics)))

(defn draw-image [image-context [center-x center-y] sprite-image-context]
  ; what to do with background color
  ; render with offset, check if it fits ... skip if not
  (let [graphics (.getGraphics image-context)]
    (.drawImage
      graphics
      sprite-image-context
      (- center-x (/ (context-width sprite-image-context) 2))
      (- center-y (/ (context-height sprite-image-context) 2))
      (context-width sprite-image-context)
      (context-height sprite-image-context)
      (color->awt-color color-transparent)
      nil)))

(defn draw-text [image-context color ^String text x y]
  (let [graphics (.getGraphics image-context)]
    (.setColor graphics (color->awt-color color))
    (.drawString graphics text (int x) (int y))))

(defn set-point [image-context color x y]
  (.setRGB image-context x y (.getRGB (color->awt-color color))))

(defn set-point-safe [image-context color x y]
  (when
      (and
       (>= x 0) (< x (context-width image-context))
       (>= y 0) (< y (context-height image-context)))
    (.setRGB image-context x y (.getRGB (color->awt-color color)))))

(defn set-point-width-safe [image-context color width x y]
  (let [context-width (context-width image-context)
        context-height (context-height image-context)
        half (int (Math/floor (/ width 2)))]
    (if (> width 1)
      (doseq [x (range (- x half) (inc (+ x half)))]
        (doseq [y (range (- y half) (inc (+ y half)))]
          (when
              (and (>= x 0) (< x context-width) (>= y 0) (< y context-height))
            (.setRGB image-context x y (.getRGB (color->awt-color color))))))
      (when
          (and (>= x 0) (< x context-width) (>= y 0) (< y context-height))
        (.setRGB image-context x y (.getRGB (color->awt-color color)))))))

(defn set-points [image-context points color]
  (let [color-rgb (.getRGB (color->awt-color color))]
    (doseq [{x :x y :y} points]
      (.setRGB image-context x y color-rgb))))

(defn extract-subimage [{
                          image-context :image-context
                          x :x
                          y :y
                          width :width
                          height :height :as configuration}]
  (.getSubimage image-context x y width height))

; copied from clojure-repl old clj-common project
(defn create-thumbnail
  "Creates thumbnail that will fit into max-dimension x max-dimension square"
  [max-dimension ^java.awt.image.BufferedImage image]
  (let [width (.getWidth image)
        height (.getHeight image)
        factor (min (/ max-dimension width) (/ max-dimension height))
        new-width (int (* width factor))
        new-height (int (* height factor))
        empty-image (create-image-context new-width new-height)
        graphics (.getGraphics empty-image)]
    (.drawImage graphics image 0 0 new-width new-height nil)
    (.dispose graphics)
    empty-image))


(defn write-background [image-context color]
  (fill-poly
    image-context
    (list
       {:x 0 :y 0}
       {:x (context-width image-context) :y 0}
       {:x (context-width image-context) :y (context-height image-context)}
       {:x 0 :y (context-height image-context)})
    color))

(defn write-to-stream [image-context output-stream]
  (ImageIO/write
    image-context
    "BMP"
    output-stream))

(defn write-png-to-stream [image-context output-stream]
  (ImageIO/write
    image-context
    "PNG"
    output-stream))

(defn image-context->input-stream
  "Writes image to PNG bytes and provides input-stream"
  [image-context]
  (let [buffer-output-stream (io/create-buffer-output-stream)]
    (write-png-to-stream image-context buffer-output-stream)
    (io/buffer-output-stream->input-stream buffer-output-stream)))
