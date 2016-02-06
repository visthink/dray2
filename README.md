# DRAE: Diagrammatic Reasoning and Analysis Engine

DRAE extracts elements from PDF and other vector files, and attempts to perform reasoning over those files.

Web site: <https://bitbucket.org/rwferguson/drae>

## License

Copyright © 2015-2016 Leidos Holdings, Ltd.

## Documentation and API

The Clojure API is documented in [./doc/API](./doc/API/index.html).

The Java API is documented in [./doc/gui](./doc/gui/index.html).

## Installation using a precompiled jar file

+ Install [pdftoxml](http://pdf2xml.sourceforge.net/). DRAE uses pdftoxml to do initial element extraction from the PDF, and to create image backgrounds and thumbnails.
 
+ Edit [drae-settings.edn](./drae-settings.edn) to point to the pdftoxml executable. E.g.:

>  `:pdftoxml-executable "/Users/fergusonrw/bin/pdftoxml"`

+ Run the system from the jar file:

>  `java -jar <jarfilename>.jar`

## Compiling a new uberjar

To compile a new uberjar:

* Install [Leiningen](http://leiningen.org/), the Clojure build system based on Maven. This 
  is done via a single downloadable script.

* Create a new profiles.clj file from sample-profiles.clj. 

>   + Copy [sample-profiles.clj](sample-profiles.clj) to `profiles.clj` (in the same directory).  
>   + Edit `profiles.clj` to point to the Java compiler, e.g.: 
      `:java-cmd "/Library/Java/JavaVirtualMachines/jdk1.7/bin/java"`

* To compile the uberjar, use the lein command:

> `lein uberjar`

* You can also run the unit tests with the following command from the DRAE root directory:

> `lein test`

## Feedback

Feedback can either be sent directly to the developers 
(*ronald.w.ferguson@leidos.com, daniel.j.powell@leidos.com*), or added as an issue on our BitBucket site.