(ns account-service.core
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :as rs]))

(s/defschema Account
  {:id      Long
   :balance s/Num})

(s/defschema NewAccount (dissoc Account :id))

(s/defschema Transfer
  {:id           Long
   :from-account s/Int
   :to-account   s/Int
   :amount       s/Num
   :status       s/Keyword})

(s/defschema NewTransfer (-> Transfer
                             (dissoc :id)
                             (dissoc :status)))

(s/defschema Deposit
  {:id Long
   :account s/Int
   :amount s/Num
   :status s/Keyword})

(s/defschema NewDeposit
  (-> Deposit
      (dissoc :id)
      (dissoc :status)))

(defonce accounts (atom {}))
(defonce seq-no (atom 0))

(defn -'' [x y]
  (if (>= x y)
    (-' x y)
    (throw (Exception. "no money"))))

(defn deposit [amount acct]
  (dosync
   (alter acct update :balance +' amount)))

(defn transfer [amount from to]
  (dosync
   (alter from update :balance -'' amount)
   (alter to update :balance +' amount)))

(def app
  (api
   {:swagger
    {:ui   "/"
     :spec "/swagger.json"
     :data {:info {:title "Account Service"}
            :tags [{:name "api"}]}}}
   (context "/api" []
            :tags ["api"]
            (POST "/account" []
                  :return Account
                  :summary "Creates an account in the system with an initial balance"
                  :body [account (describe NewAccount "new account")]
                  (ok (let [new-acct (ref (assoc account :id (swap! seq-no inc)))]
                        (swap! accounts assoc (:id @new-acct) new-acct)
                        @new-acct)))
            (GET "/accounts" []
                 :return [Account]
                 :summary "Returns all accounts"
                 (ok (map deref (vals @accounts))))
            (GET "/account/:id" []
                 :path-params [id :- Long]
                 :return (s/maybe Account)
                 :summary "Returns details of account"
                 (ok (if-let [acct (@accounts id)]
                       @acct)))
            (POST "/deposit" []
                  :return (s/maybe Deposit)
                  :body [depo (describe NewDeposit "new deposit")]
                  (let [acct (@accounts (:account depo))]
                    (try
                      (do
                        (deposit (:amount depo) acct)
                        (ok (-> depo
                                (assoc :id (swap! seq-no inc))
                                (assoc :status "OK"))))
                      (catch Exception e
                        (println e)
                        (not-modified)))))
            (POST "/transfer" []
                  :return (s/maybe Transfer)
                  :body [xfr (describe NewTransfer "new transfer")]
                  (let [from (@accounts (:from-account xfr))
                        to   (@accounts (:to-account xfr))]
                    (try
                      (do
                        (transfer (:amount xfr) from to )
                        (ok (-> xfr
                                (assoc :id (swap! seq-no inc))
                                (assoc :status "OK"))))
                      (catch Exception e
                        (not-modified))))))))
