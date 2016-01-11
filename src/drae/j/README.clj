(ns drae.j.README
  "Overview of compiled DRAE classes for Java within the drae.j subpackage."
  )

(def +readme+
  "This package exists solely to include documentation of the compiled Java classes
   within drae.j.*. Because metadata is removed from compiled namespaces, it is not
   possible to include namespace-level documentation for compiled namespaces, so we 
   include it here for the compiled classes within the package.

   - _[[drae.j.Corpus]]_: Class for accessing default corpus libraries.

   - _[[drae.j.Doc]]_: Class for creating VDocument (and associated pages and visual elements).

   - _[[drae.j.Paxll]]_: Class for performing queries to Pathway Commons 2 server

     - _drae.j.BoundingBox_: A bounding box (given as a field in most visual elements). Subclass of java.awt.geom.Rectangle2D.Double.

   Within the [[drae.j.Doc]] namespace, there are also a set of classes for documents and their parts.

   All visual elements are instances of the drae.j.VisualElement.El interface, which return proper values for the *getBbox* and *getItems* methods:

   - _drae.j.VisualElement.VDocument_: A single document. Getters: *filename*, *getItems*

   - _drae.j.VisualElement.VPage_: A single page within a document. Getters: *getBbox*, *getItems*

   - _drae.j.VisualElement.VText_: A single line of text within a page. Getters: *getBbox*, *getItems*, *text*

   - _drae.j.VisualElement.VTextToken_: A single word token within a text line. Getters: *getBbox*, 
                                        *text*

   - _drae.j.VisualElement.VDiagram_: ( _Not currently implemented_ ) A diagram from the paper (image with associated text lines). Getters: *getBbox*, *image*, *getItems*

   - _drae.j.VisualElement.VImage_: A single image from the page. Getters: *bitmap-path* *getBbox*. 
          To get the complete image path from a VImage, call [[full-bitmap-path-for]] on the image and
          it's containing document.

   - _drae.j.VisualElement.Blob_: ( _Not currently implemented_ ) A single blob detected in an image.

   A bounding box is a subclass of Rectangle.Double2D, and so can be accessed using those methods,
   including *getX*, *getY*, *getWidth*, *getHeight*.

   The easiest way to create a VDocument is to call the [[drae.j.Doc/getVDocument]] method on a
   pdf file instance, such as that returned by [[drae.j.Corpus/getCorpusFile]] method.

  "
  nil)

