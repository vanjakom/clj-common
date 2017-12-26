(ns clj-common.2d)

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

(defn width [image-context]
  (.getWidth image-context))

(defn height [image-context]
  (.getHeight image-context))

(defn color->awt-color [color]
  (new java.awt.Color ^float (:red color) ^float (:green color) ^float (:blue color)))


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
  (let [x-coords (int-array (map :x points))
        y-coords (int-array (map :y points))
        color-rgb (.getRGB (color->awt-color color))]
    (doseq [{x :x y :y} points]
      (.setRGB image-context x y color-rgb))))

(defn write-background [image-context color]
  (fill-poly
    image-context
    (list
       {:x 0 :y 0}
       {:x (width image-context) :y 0}
       {:x (width image-context) :y (height image-context)}
       {:x 0 :y (height image-context)})
    color))

(defn write-to-stream [image-context output-stream]
  (javax.imageio.ImageIO/write
    image-context
    "BMP"
    output-stream))
