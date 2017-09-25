(ns clj-common.geo)

(use 'clj-common.clojure)

(def model
  {
    :location
    {
      :longitude :double
      :latitude :double}

    :rect
    {
      :min-longitude :double
      :min-latitude :double
      :max-longitude :double
      :max-latitude :double}})

(defn create-location [longitude latitude]
  {:longitude longitude :latitude latitude})

; http://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
(defn distance [location1 location2]
  (let [lat1 (:latitude location1) lon1 (:longitude location1)
        lat2 (:latitude location2) lon2 (:longitude location2)
        earth-radius 6371]
    (let [lat-distance (Math/toRadians (- lat2 lat1))
          lon-distance (Math/toRadians (- lon2 lon1))
          sin-lat-distance-half (Math/sin (/ lat-distance 2))
          sin-lon-distance-half (Math/sin (/ lon-distance 2))
          a (+
              (* sin-lat-distance-half sin-lat-distance-half)
              (*
                (Math/cos (Math/toRadians lat1))
                (Math/cos (Math/toRadians lat2))
                sin-lon-distance-half
                sin-lon-distance-half))
          c (*
              (Math/atan2
                (Math/sqrt a)
                (Math/sqrt (- 1 a)))
              2)]
      (*
        earth-radius
        c
        1000))))

(defn distance->human-string [distance]
  (cond
    (> distance 1000) (str (long (/ distance 1000)) "km")
    :else (str distance "m")))

(def distance-two-locations distance)

(defn speed-km [location1 location2]
  (let [distance (distance location1 location2)
        duration (- (:timestamp location2) (:timestamp location1))]
    (if (and (> distance 0) (> duration 0))
      (* 3.6 (/ distance duration))
      0)))

(defn angle-to-radians [angle]
  (Math/toRadians angle))

(defn radians-to-degrees [radians]
  (Math/toDegrees radians))

; Calculating Bearing or Heading angle between two points
; http://www.igismap.com/formula-to-find-bearing-or-heading-angle-between-two-points-latitude-longitude/
(defn bearing [location1 location2]
  (let [latitude1 (angle-to-radians (:latitude location1))
        longitude1 (angle-to-radians (:longitude location1))
        latitude2 (angle-to-radians (:latitude location2))
        longitude2 (angle-to-radians (:longitude location2))
        delta-longitude (angle-to-radians (- (:longitude location2) (:longitude location1)))
        x (* (Math/cos latitude2) (Math/sin delta-longitude))
        y (-
            (* (Math/cos latitude1) (Math/sin latitude2))
            (* (Math/sin latitude1) (Math/cos latitude2) (Math/cos delta-longitude)))]
    (radians-to-degrees (Math/atan2 x y))))



(defn location-in-rect
  "location :location
   rect :rect"
  [location rect]
  (let [longitude (:longitude location)
        latitude (:latitude location)]
    (and
      (> longitude (:min-longitude rect))
      (< longitude (:max-longitude rect))
      (> latitude (:min-latitude rect))
      (< latitude (:max-latitude rect)))))

; common functions for multiple-reduce
(defn min-aggregate [field state location]
  (min state (get location field)))
(defn max-aggregate [field state location]
  (max state (get location field)))

(def max-aggregate-longitude (partial max-aggregate :longitude))
(def min-aggregate-longitude (partial min-aggregate :longitude))
(def max-aggregate-latitude (partial max-aggregate :latitude))
(def min-aggregate-latitude (partial min-aggregate :latitude))

(defn rect-for-location-seq [location-seq]
  (let [first-location (first location-seq)
        aggregate (multiple-reduce
                    [:max-longitude (:longitude first-location) max-aggregate-longitude
                     :min-longitude (:longitude first-location) min-aggregate-longitude
                     :max-latitude (:latitude first-location) max-aggregate-latitude
                     :min-latitude (:latitude first-location) min-aggregate-latitude]
                    location-seq)
        longitude-span (- (:max-longitude aggregate) (:min-longitude aggregate))
        center-longitude (+ (:min-longitude aggregate) (/ longitude-span 2))
        latitude-span (- (:max-latitude aggregate) (:min-latitude aggregate))
        center-latitude (+ (:min-latitude aggregate) (/ latitude-span 2))]
    [center-longitude center-latitude longitude-span latitude-span]))

(defn location-seq->longitude-latitude-radius [location-seq]
  (let [first-location (first location-seq)
        aggregate (multiple-reduce
                    [:max-longitude (:longitude first-location) max-aggregate-longitude
                     :min-longitude (:longitude first-location) min-aggregate-longitude
                     :max-latitude (:latitude first-location) max-aggregate-latitude
                     :min-latitude (:latitude first-location) min-aggregate-latitude]
                    location-seq)]
    [
      (+
        (:min-longitude aggregate)
        (/ (- (:max-longitude aggregate) (:min-longitude aggregate)) 2))
      (+
        (:min-latitude aggregate)
        (/ (- (:max-latitude aggregate) (:min-latitude aggregate)) 2))
      (distance
        {:longitude (:min-longitude aggregate) :latitude (:min-latitude aggregate)}
        {:longitude (:max-longitude aggregate) :latitude (:max-latitude aggregate)})]))

