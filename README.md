![](https://bitbucket.org/rwferguson/dray/wiki/images/dray-small.png "Bringing you figures and tables since 2016!")

# **DRAY: A System for Document Representation and Analysis**

*DRAY is currently in development.*

## Introduction

DRAY is a system for analyzing tables and figures in PDF documents. DRAY can read in PDF documents and then use *producers* to 1) select regions of a particular type (called *working sets*), and 2) run NLP and other recognition agents on working sets of a particular type. For example, one agent could be used to select images with captions and a second could be used to run a entity recognition system on the result.

DRAY also has a GUI that allows the user to manually select working set regions and then run a set of predefined producers on those working sets.

Current functionality is limited to marking up table and diagram content (which can be stored in a JSON overlay file) and generating JSON representations of table content.# DRAE: Diagrammatic Reasoning and Analysis Engine

## Documentation and API

[Instructional Wiki](https://bitbucket.org/rwferguson/dray/wiki/Home)

[Clojure API](http://rwferguson.bitbucket.org/dray/API/)

[Java API](http://rwferguson.bitbucket.org/dray/gui/index.html)

[Development Roadmap](https://bitbucket.org/rwferguson/dray/wiki/RoadMap)


## Quickstart

### Installation using a precompiled jar file

+ Unzip the most recent archive from the [downloads folder](https://bitbucket.org/rwferguson/dray/downloads).

+ Install [pdftoxml](http://pdf2xml.sourceforge.net/). 
 
+ Edit [dray-settings.edn](./dray-settings.edn) to point to the pdftoxml executable. E.g.:

   `:pdftoxml-executable "/Users/fergusonrw/bin/pdftoxml"`

+ Run the system GUI using the dray script:

   `dray`


## Feedback

For now, feedback can be [added as an issue](https://bitbucket.org/rwferguson/dray/issues?status=new&status=open). We are hoping to add an email list in the near future.

## License

Copyright © 2015-2016 Leidos Holdings, Ltd.

Licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). 

## Acknowledgments

Code by Ronald W. Ferguson, Daniel Powell, Yanlin Guo, and Shane Frasier. Manual written by Danniel Powell with updates from Ron Ferguson.

Dray cart image by [Prawney Vintage](http://www.clipartof.com/portfolio/prawny-vintage). Licensed from ClipArtOf.com.

