(ns session.server
  (:require [session.datomic :as datomic]
            [session.views.common :as common]
            [noir.core :as nc]
            [noir-async.core :as nac]
            [aleph.http :as http]
            [noir.server :as server]))


(nc/defpage "/" []
  (common/layout
   (seq [[:div.navbar.navbar-fixed-top
          [:div.navbar-inner
           [:div.container {:style "margin-left:80px"}
            [:span {:href "#" :style "font-size:20px;margin-left:0px"} "session"]
            [:ul.nav]]]]
         [:div.container {:style "margin-left:20px"}
          [:div.content]]])))

(nc/defpage "/get_session" args
  {:status 202
   :headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str (datomic/get-datomic-session))})

(nac/defpage-async "/service" [] conn
  (datomic/subscribe-channel (:request-channel conn))
  (nac/on-receive conn #(datomic/service-request (read-string %))))


(defn -main [& m]
  (let [mode :dev
        port (Integer/parseInt (first m))
        noir-handler (server/gen-handler {:mode mode})]
    (session.datomic/setup  (last m))
    (http/start-http-server
     (http/wrap-ring-handler noir-handler)
     {:port port :websocket true})
    (datomic/process-requests-thread @datomic/conn)))
