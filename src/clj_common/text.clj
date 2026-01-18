(ns clj-common.text)

(def mapping {"А" "A", "Б" "B", "В" "V", "Г" "G", "Д" "D", "Ђ" "Đ", "Е" "E",
              "Ж" "Ž", "З" "Z", "И" "I", "Ј" "J", "К" "K", "Л" "L", "Љ" "LJ",
              "М" "M", "Н" "N", "Њ" "NJ", "О" "O", "П" "P", "Р" "R", "С" "S",
              "Ш" "Š", "Т" "T", "Ћ" "Ć", "У" "U", "Ф" "F", "Х" "H", "Ц" "C",
              "Ч" "Č", "Џ" "DŽ", "а" "a", "б" "b", "в" "v", "г" "g", "д" "d",
              "ђ" "đ", "е" "e", "ж" "ž", "з" "z", "и" "i", "ј" "j", "к" "k",
              "л" "l", "љ" "lj", "м" "m", "н" "n", "њ" "nj", "о" "o", "п" "p",
              "р" "r", "с" "s", "ш" "š", "т" "t", "ћ" "ć", "у" "u", "ф" "f",
              "х" "h", "ц" "c", "ч" "č", "џ" "dž", "Ња" "Nja", "Ње" "Nje",
              "Њи" "Nji", "Њо" "Njo", "Њу" "Nju", "Ља" "Lja", "Ље" "Lje",
              "Љи" "Lji", "Љо" "Ljo", "Љу" "Lju", "Џа" "Dža", "Џе" "Dže",
              "Џи" "Dži", "Џо" "Džo", "Џу" "Džu"})

(defn cyrillic->latin [text]
  (reduce (fn [t [c l]] (clojure.string/replace t c l))
          text
          (sort-by (comp - count key) mapping)))

(defn latin->cyrillic [text]
  (let [latin-to-cyrillic (into {}
                                (map (fn [[k v]] [v k])
                                     mapping))]
    (reduce (fn [t [l c]] (clojure.string/replace t l c))
            text
            (sort-by (comp - count key) latin-to-cyrillic))))

#_(cyrillic->latin "Жарачка планина - врх Бандера")
;; "Žaračka planina - vrh Bandera"

(latin->cyrillic "Žaračka planina - vrh Bandera")
;; "Жарачка планина - врх Бандера"





