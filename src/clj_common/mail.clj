(ns clj-common.mail
  (:require postal.core)
  (:import
    java.io.InputStream
    java.util.Properties
    javax.activation.DataHandler
    javax.mail.Session
    javax.mail.Transport
    javax.mail.Authenticator
    javax.mail.internet.MimeMessage
    javax.mail.internet.MimeBodyPart
    javax.mail.internet.MimeMultipart
    javax.mail.internet.InternetAddress
    javax.mail.Message$RecipientType
    javax.mail.PasswordAuthentication
    javax.mail.util.ByteArrayDataSource))

; configuration
; :host - smptp server
; :username - username
; :password - password
; :from - from

(defn send-email
  ([configuration to subject body]
   (send-email configuration to subject body nil))
  ([configuration to subject body attachments]
   (let [properties (doto
                      (new Properties)
                      (.put "mail.smtp.host" (:host configuration))
                      (.put "mail.smtp.socketFactory.class" "javax.net.ssl.SSLSocketFactory")
                      (.put "mail.smtp.socketFactory.fallback" "false")
                      (.put "mail.smtp.port" "465")
                      (.put "mail.smtp.socketFactory.port" "465")
                      (.put "mail.smtp.starttls.enable" "true")
                      (.put "mail.smtp.auth" "true")
                      (.put "mail.store.protocol" "pop3")
                      (.put "mail.transport.protocol" "smtp")
                      (.put "mail.debug.auth" "true")
                      (.put "mail.pop3.socketFactory.fallback" "false"))
         session (Session/getInstance
                   properties
                   (proxy [Authenticator] []
                     (getPasswordAuthentication
                       []
                       (new
                         PasswordAuthentication
                         (:username configuration)
                         (:password configuration)))))
         message (new MimeMessage session)]
     (let [mime-multipart (new MimeMultipart)]
       (.addBodyPart
         mime-multipart
         (doto
           (new MimeBodyPart)
           (.setText body)))
       (doseq [attachment attachments]
         (.addBodyPart
           mime-multipart
           (doto
             (new MimeBodyPart)
             (.setDataHandler
               (new
                 DataHandler
                 (new
                   ByteArrayDataSource
                   (:content attachment)
                   (:content-type attachment))))
             (.setFileName "test"))))
       (doto message
         (.setFrom (new InternetAddress (:from configuration)))
         (.addRecipients Message$RecipientType/TO to)
         (.setSubject subject)
         (.setContent mime-multipart))
       (Transport/send message)))))

(comment
  (require 'clj-common.localfs)
  (send-email
    {
      :host "smtp.zoho.com"
      :username "<USERNAME>"
      :password "<PASSWORD"
      :from "<EMAIL>"}
    "<EMAIL>"
    "Test"
    "test message"
    [{
       :content-type "image/jpeg"
       :content (clj-common.localfs/input-stream ["Users" "vanja" "Desktop" "dashboard.jpg"])}])
)
