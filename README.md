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