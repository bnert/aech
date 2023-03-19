# aech
A `fetch` api/client for Clojure(Script)

It supports both HTTP/1.1 and HTTP/2, with asynchronous execution being the default.

This API is meant to align with `fetch` browser API, and bring those idioms
into a client which can be used on the server, node.js, or browser.

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
