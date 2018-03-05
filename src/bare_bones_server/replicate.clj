(ns bare-bones-server.replicate
  (:require [slingshot.slingshot :refer [try+ throw+]]
            [clojure.tools.nrepl :as c]
            [clojure.tools.nrepl.server :as s]
            [clojure.tools.nrepl.transport :as t]
            [clojure.tools.nrepl.middleware :as m]
            [clojure.tools.nrepl.middleware.session :as m.session]
            [clojure.tools.nrepl.misc :as misc]))

;; copy sessions to follower(s)

;; TODO
;; namespace the keys we use?

;; :session -> {:username :session :transport}
(def team (atom {}))

(defn session->member [session]
  (get @team session))

(defn add-member! [member]
  (swap! team assoc (:session member) member))

(defn del-member! [player]
  (swap! team dissoc (:session player)))

(defn broadcast [session msg]
  (when-let [member (session->member session)]
    (doseq [{:keys [transport session]} (vals @team)]
      (try+
        (t/send transport {:session   session
                           :username  (:username member)
                           :broadcast msg})
        (catch java.net.SocketException _ex
          (del-member! member))))))

(defn join-middleware [handler]
  (fn [{:keys [op transport session username] :as msg}]
    (cond
      (not= "join" op) (handler msg)
      (nil? session) (t/send transport
                             (misc/response-for msg :status [:err :no-session]))
      :else (do (add-member! {:username  username
                              :session   session
                              :transport transport})
                (broadcast session {:joined session})
                (t/send transport (misc/response-for msg :status :done))))))

(m/set-descriptor!
  #'join-middleware
  {:requires #{#'m.session/session}
   :expects  #{}
   :handles  {"join"
              {:doc      "Join the replicate REPL."
               :requires {"username" "The name of the user"
                          "session"  "The session to of the current user"}
               :optional {}
               :returns  {"status" "Done"}}}})


(defn follow-middleware [handler]
  (fn [{:keys [op transport session username] :as msg}]
    (prn 'msg msg)                                          ;; TODO log instead
    (cond
      (not= "follow" op) (handler msg)
      (nil? session) (t/send transport
                             (misc/response-for msg :status [:err :no-session]))
      :else (do (add-member! {:username  username
                              :session   session
                              :transport transport})
                (broadcast session {:joined session})
                (t/send transport (misc/response-for msg :status :done))))))

(m/set-descriptor!
  #'follow-middleware
  {:requires #{#'m.session/session}
   :expects  #{}
   :handles  {"follow"
              {:doc      "Follow another team member."
               :requires {"username" "The name of the (joined) team member to follow"}
               :optional {}
               :returns  {"status" "Done"}}}})

(defn eval-middleware [handler]
  (fn [{:keys [op session code ns] :as msg}]
    (when (= "eval" op)
      (broadcast session {:code code :ns ns}))
    (handler msg)))

(m/set-descriptor! #'eval-middleware
                   {:requires #{}
                    :expects  #{"eval"}
                    :handles  {}})

(defrecord Broadcast [transport]
  clojure.tools.nrepl.transport.Transport
  (recv [this timeout]
    (t/recv transport timeout))
  (send [this msg]
    (t/send transport msg)
    (when-let [session (:session msg)]
      (broadcast session msg))))

(defn broadcast-middleware [handler]
  (fn [msg] (handler (update-in msg [:transport] ->Broadcast))))

(m/set-descriptor! #'broadcast-middleware
                   {:requires #{}
                    :expects  #{}
                    :handles  {}})

(defn default-handler [& other-handlers]
  (-> (apply s/default-handler other-handlers)
      broadcast-middleware
      eval-middleware
      follow-middleware))

(defn server [port & other-handlers]
  (s/start-server :port port :handler (apply default-handler other-handlers)))

(comment
  ;; for debugging :)

  ;; in server repl
  (use 'concerto.core)

  ;; in client repl A
  (require '[clojure.tools.nrepl :as nrepl])
  (def conn (nrepl/connect :port 8003))
  (def client (nrepl/client conn 1000))
  (def session (nrepl/new-session client))
  (nrepl/message client {:op :join :username "A" :session session})

  ;; in client repl B
  (require '[clojure.tools.nrepl :as nrepl])
  (def conn (nrepl/connect :port 8003))
  (def client (nrepl/client conn 1000))
  (def session (nrepl/new-session client))
  (nrepl/message client {:op :join :username "B" :session session})
  (nrepl/message client {:op :eval :code "1" :session session})
  )
