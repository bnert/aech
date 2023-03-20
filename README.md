# aech
A `fetch`-ish api/client for Clojure(Script), based on clojure.core.async idioms.

It supports both HTTP/1.1 and HTTP/2.

The goal of this library is to provide a simple, `fetch`-like API which
can be used on the JVM, node, and browser, with async support using
channel based communication via `clojure.core.async`.


## Kudos
This project was intially forked from [`hato`](https://github.com/gnarroway/hato)


## Status
This is exeperimental, still working out adapting the `fetch` api to clojure.

Additionally, I haven't started on the cljs side, though that work will be
more or less a thin layer around globally available `fetch`


## Installation
`aech` requires JDK 11 and above, and if using in a javascript environment, requires
a JS runtime with a globally available `fetch`.


## License
Released under the MIT License: http://www.opensource.org/licenses/mit-license.php
