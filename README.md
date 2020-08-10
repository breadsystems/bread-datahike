# bread-datahike

A BreadCMS Datastore plugin for using a Datahike backend.

[![Clojars Project](https://img.shields.io/clojars/v/systems.bread/bread-datahike.svg)](https://clojars.org/systems.bread/bread-datahike)

## Usage

```clojure
(ns my.bread.app
  (:require
    [org.http-kit.server :as http-kit] ; or whatever server you like...
    [systems.bread.bread-datahike.core :as datastore]
    [systems.bread.core :as bread]))

;; Build a BreadCMS app that uses the bread-datahike plugin.
(def app (bread/app {:plugins [(datastore/datahike-plugin {})]}))

;; Run a standard BreadCMS app with your HTTP server of choice.
(http-kit/run-server (bread/app->handler app) {:port 8080})
```

### Configuration options

Each example uses the default value for that option.

```clojure
(datastore/datahike-plugin
  {; Customize the param to look for in the request specifying
   ; the datahike timepoint to use for (store/as-of)
   :as-of-param :as-of
   ; Customize the date format to use to parse (:as-of params)
   :as-of-format "yyyy-MM-dd HH:mm:ss z"
   ; Shorthand for (bread/add-hook :hook/datastore.req->timepoint ...)
   ; This is the hook that returns the timepoint to pass to (store/as-of @db ...)
   ; if any such value is detected in the request.
   :->timepoint datastore/req->timepoint
   ; Shorthand for (bread/add-hook :hook/datastore.req->datastore ...)
   ; This is the hook that initializes the actual datastore instance.
   :->datastore datastore/req->datastore})
```