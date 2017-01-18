(ns clj-common.geo)

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
(defn distance-two-locations [location1 location2]
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
