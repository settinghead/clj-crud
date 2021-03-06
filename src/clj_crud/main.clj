(ns clj-crud.main
  (:require [clojure.tools.logging :refer [info debug]]
            [com.stuartsierra.component :as component]
            [clj-crud.core :as core]
            [clj-crud.system.database :as database]
            [clojure.string :as string])
  (:gen-class))

(defn -main [& args]
  (info "Running main with args: " args)
  (let [port (try (Long/parseLong (first args))
                  (catch Exception _ -1))
        _ (assert (pos? port) (str "Something is wrong with the port argument: " (first args)))
        database-url (let [db-url (second args)]
                       (assert (.startsWith db-url "postgres:")
                               (str "Something is wrong with the database argument: " (second args)))
                       (database/db-url-for-heroku db-url))
        system (core/crud-system (core/production-config port database-url))]
    (component/start system)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (info "Shutting down main")
                                 (component/stop system))))))

(defn migrate [& args]
  (let [database-url (System/getenv "DATABASE_URL")
        to-version (try (Long/parseLong (first args))
                        (catch Exception _ nil))]
    (assert (and database-url
                 (.startsWith database-url "postgres:")) "DATABASE_URL is required")
    (let [db-url (database/db-url-for-heroku database-url)]
      (-> (core/migration-system {:db-connect-string db-url
                                  :migrate-to-version to-version})
          component/start
          component/stop))))
