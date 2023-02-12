# dyn-connector

Libraries for M2M communication to Dyn/PigrecoOS

## Features

* Supported languages: Java, Go, PHP, Javascript
* OAuth2 compliant
* Stateless and stateful modes
* Transparent Auth and tokens expiration management

## Installation and usage

All modules are developed using native libraries. You should simply include one of these in your project, without the need for any external dependency.

**Client ID** and **Client Secret** are authorization constants that could be obtained from [PigrecoOS](http://www.pigrecoos.it) services.

**Access token**, used with the only get/set callback required by the Connector, must to be saved on a persistent storage, for example a file or a DB.

For **Operation** and related parameters please refer to [PigrecoOS](http://www.pigrecoos.it) API section.

---

*Copyright 2023 [Modulo srl](http://www.modulo.srl) - Licensed under the Apache license*
