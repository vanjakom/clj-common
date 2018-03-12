(ns clj-common.2d
  (:import javax.imageio.ImageIO))

; structures
; point
;    :x
;    :y

; java 2d coordinate system
; upper left corner is (0,0)


(def color-white {:red (float 1.0) :green (float 1.0) :blue (float 1.0)})
(def color-black {:red (float 0.0) :green (float 0.0) :blue (float 0.0)})
(def color-green {:red (float 0.0) :green (float 1.0) :blue (float 0.0)})

(defn random-color [] {:red (rand) :green (rand) :blue (rand)})

(defn create-image-context [width height]
  (new
    java.awt.image.BufferedImage
    width
    height
    java.awt.image.BufferedImage/TYPE_INT_RGB))

(defn context-width [image-context]
  (.getWidth image-context))

(defn context-height [image-context]
  (.getHeight image-context))

(defn color->awt-color [color]
  (new java.awt.Color ^float (:red color) ^float (:green color) ^float (:blue color)))


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