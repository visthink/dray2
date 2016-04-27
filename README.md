![](https://bitbucket.org/rwferguson/drae/wiki/images/logo.png "DRAY")  ![](images/dray-small.jpg "Bringing you figures and tables since 2016!")

# **DRAY: A System for Document Representation and Analysis**

## Introduction

DRAY is a system for analyzing tables and figures in PDF documents. DRAY can read in PDF documents and then use *producers* to 1) select regions of a particular type (called *working sets*), and 2) run NLP and other recognition agents on working sets of a particular type. For example, one agent could be used to select images with captions and a second could be used to run a entity recognition system on the result.

DRAY also has a GUI that allows the user to manually select working set regions and then run a set of predefined producers on those working sets.

Current functionality is limited to marking up table and diagram content (which can be stored in a JSON overlay file) and generating JSON representations of table content.# DRAE: Diagrammatic Reasoning and Analysis Engine

## Documentation and API

[Online Manual](https://bitbucket.org/rwferguson/drae/wiki/Home)

[Clojure API Documentation](http://rwferguson.bitbucket.org/drae/API/)

[Java API](http://rwferguson.bitbucket.org/drae/gui/index.html)

[Roadmap](https://bitbucket.org/rwferguson/drae/wiki/RoadMap)

[Contributors](https://bitbucket.org/rwferguson/drae/wiki/Contributors)

## Quickstart

### Installation using a precompiled jar file

+ Install [pdftoxml](http://pdf2xml.sourceforge.net/). DRAE uses pdftoxml to do initial element extraction from the PDF, and to create image backgrounds and thumbnails.
 
+ Edit [drae-settings.edn](./drae-settings.edn) to point to the pdftoxml executable. E.g.:

>  `:pdftoxml-executable "/Users/fergusonrw/bin/pdftoxml"`

+ Run the system from the jar file:

>  `java -jar <jarfilename>.jar`

### Compiling a new uberjar

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
(*ronald.w.ferguson@leidos.com, daniel.j.powell@leidos.com*), or added as an issue on our [BitBucket site](https://bitbucket.org/rwferguson/drae).

## License

Copyright © 2015-2016 Leidos Holdings, Ltd.

Licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). 



I