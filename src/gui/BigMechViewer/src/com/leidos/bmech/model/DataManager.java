/**
 * The DataManager class manages the working sets, analyses, and selected items in a 
 * given VDcoument. 
 * 
 * @author Daniel Powell
 * @author Ron Ferguson
 */
package com.leidos.bmech.model;

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataListener;
import javax.swing.ListModel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import com.leidos.bmech.analysis.EvidenceGatherer;
//import com.leidos.bmech.view.DataManagerView;

import drae.j.BoundingBox;
import drae.j.Doc;
import drae.j.Producer.Table;
import drae.j.VisualElement.El;
import drae.j.VisualElement.VDocument;
import drae.j.VisualElement.VImage;
import drae.j.VisualElement.VPage;
import drae.j.VisualElement.VTable;
import drae.j.VisualElement.VText;

//import javax.swing.event.ListDataListener;

public class DataManager extends Observable 
                         implements ListModel<VPage> {

  private File                     pdfFile;         // Current PDF file.
  public  VDocument                vDocument;       // Its VDocument object.
  private BufferedImage[]          pageImageList;   // ! Move to view ! 
  private int[]                    pageImageStatus; // ! Move to view !
  private int                      offsetX;         // ! Move to view !
  private int                      offsetY;         // ! Move to view !
  private WorkingSet               headWS;          // Top working set (of all working sets).
  public  WorkingSet               currentWS;       // Current working set.
  public  int                      currentPage;     // Current page.
  public  List<El>                 selectedEls;     // Currently selected elements.

  private Map<String, Rectangle2D> imageBBMap;
  // private DataManagerView          view;
  BufferedImage                    defaultImage;
  EvidenceGatherer                 eg;
  int                              preprocessState; // -1: not started; 0:
                                                    // underway; 1: done
  List<ListDataListener> listeners;
  
  public DataManager() {
    
    super();
    
   // view = new DataManagerView(this);
    
    imageBBMap = new HashMap<String, Rectangle2D>();
    defaultImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    eg = new EvidenceGatherer();
    preprocessState = -1;// not started
    pageImageList   = new BufferedImage[0];
    pageImageStatus = new int[0];
    
  }

  @SuppressWarnings("unchecked")

  public List<VPage> getPages() {
    return (List<VPage>) vDocument.getItems();
  }
  
  public List<El> getSelectedEls () {
    return selectedEls;
  }
  
  public void setSelectedEls (List<El> els) {
    selectedEls = els;
  }
  
  public int getCurrentPage () {
    return currentPage;
  }
  
  public void setCurrentPage (int p) {
    currentPage = p;
  }
  
  // Interface ModelList methods
  // -----------------------------------------------------------
  public VPage getElementAt(int pagenum) {
    return this.getPages().get(pagenum);
  }
  
  public int getSize() {
    @SuppressWarnings("unchecked")
    List<VPage> pages = (List<VPage>) vDocument.getItems();
    return pages.size();
  }
  
  public WorkingSet getCurrentWS () {
     return currentWS;
  }
  
  public void setCurrentWS (WorkingSet ws) {
    currentWS = ws;
  }
  
  public void addListDataListener(ListDataListener l) {
    listeners.add(l);
  }
  
  public void removeListDataListener(ListDataListener l) {
    listeners.remove(l);
  }
  
  /**
   * initialize DataManager with data from a vdocument
   * 
   * @param doc
   */
  public DataManager(VDocument doc) {
    this();
    loadFromVDocument(doc);
  }

  /**
   * initialize DataManager with data from pdf. Don't load images
   * 
   * @param pdf
   *          the pdf to load
   */
  public DataManager(File pdf) {
    this();
    this.pdfFile = pdf;
    loadFromPdf(true);
  }

  /**
   * initialize DataManager with data from pdf. Internal vdocument will be
   * loaded by the constructor.
   * 
   * @param pdf
   * @param lazyLoadImages
   *          false if should load images, true if should not load images
   */
  public DataManager(File pdf, boolean lazyLoadImages) {
    this();
    this.pdfFile = pdf;
    loadFromPdf(lazyLoadImages);
  }

 // public void setView(DataManagerView v) {
 //   view = v;
 // }

  public void loadFromVDocument(VDocument doc) {
    // call clojure to populate the VDocument
    vDocument = doc;
    headWS = new WorkingSet(null, "document");
    headWS.setFilename((String) doc.getFilename());
    currentWS = headWS;
   //  view.setCurrentWS(headWS);
    @SuppressWarnings("rawtypes")
    List vPages = (List) vDocument.getItems();
    //pageIconList = new BufferedImage[vPages.size()];
    // pageImageList = new BufferedImage[vPages.size()];

    for (int i = 0; i < vPages.size(); i++) {
      createPageWS(i);
    }
    setPreprocessState(-1);

  }

  /**
   * load the page images from pdf and load the vdocument data from a closure
   * call. param lazyLoadImages -- set to true if images should only be loaded
   * when they are used
   */
  public void loadFromPdf(boolean lazyLoadImages) {
    setPreprocessState(-1);

    if (pdfFile == null) {
      System.err.println("ERROR: pdfFile is null");
      return;
    }
    if (!pdfFile.exists()) {
      System.err.println("ERROR: pdfFile doesnt exist");
      return;
    }
    System.out.println(pdfFile);
    // call clojure to populate the VDocument
    vDocument = (VDocument) Doc.getVDocument(pdfFile);
    headWS = new WorkingSet(null, "document");
    try {
      headWS.setFilename(pdfFile.getCanonicalPath().replace('\\', '/'));
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
   // view.setCurrentWS(headWS);
    currentWS = headWS;
    @SuppressWarnings("rawtypes")
    List vPages = (List) vDocument.getItems();
    pageImageList = new BufferedImage[vPages.size()];
    // pageIconList = new BufferedImage[vPages.size()];
    pageImageStatus = new int[vPages.size()];
    for (int i = 0; i < vPages.size(); i++) {
      pageImageStatus[i] = -1;
      pageImageList[i] = defaultImage;
    //  pageIconList[i] = defaultImage;
    }

    for (int i = 0; i < vPages.size(); i++) {
      System.out.println("Creating page ws " + i);
      createPageWS(i);
    }
    System.out.println("Done creating pages");

  }

  private void createPageWS(int pageIndex) {
    
    @SuppressWarnings("rawtypes")
    List vPages = (List) vDocument.getItems();
    VPage vPage = (VPage) vPages.get(pageIndex);
    // Go through and add elements to the Working Set
    WorkingSet pageWS = headWS.createChild("page" + (pageIndex + 1));
    @SuppressWarnings("rawtypes")
    List items = (List) vPage.getItems();
    for (int j = 0; j < items.size(); j++) {
      if (items.get(j) instanceof drae.j.VisualElement.El) {
        pageWS.addItem((El) items.get(j));
        El el = (El) items.get(j);
        if (el instanceof VText) {
          pageWS.getLayerList().addElementToLayer("text", el);
        } else if (el instanceof VImage) {
          pageWS.getLayerList().addElementToLayer("images", el);
          VImage image = (VImage) el;
          File path = new File((String) image.bitmap_path);
          imageBBMap.put(path.getName(), (BoundingBox) image.getBbox());
        }

      }
    }
    // pageWS.setImage(pageImage);
    pageWS.setPage(pageIndex + 1);
  }

  public LayerList getLayerList() {
    if (currentWS == null)
      return null;
    return currentWS.getLayerList();
  }

  public File getPdfFile() {
    return pdfFile;
  }

  /**
   * called from the GUI, sets the File to the pdf and calls the loadFromPdf()
   * to get the data.
   * 
   * @param pdfFile
   */
  public void setPdfFile(File pdfFile, boolean lazyLoadImages) {
    this.pdfFile = pdfFile;
    loadFromPdf(lazyLoadImages);
  }

  public void setPdfFile(File pdfFile) {
    setPdfFile(pdfFile, false);
  }

 //public BufferedImage[] getPageIconList() {
 //  return pageIconList;
 // }

 // public void setPageIconList(BufferedImage[] pageIconList) {
 //   this.pageIconList = pageIconList;
 // }

 // @SuppressWarnings("unused")
 // private void runBEE() {
 //   /*
 //    * String dataSubDir =
 //    * FilenameUtils.removeExtension(pdfFile.getAbsolutePath()) + ".xml_data";
 //    * System.out.println(dataSubDir);
 //    */
 // }

  /**
   * Get a reference to the drae.j.VDocument instance currently being used by
   * the GUI
   * 
   * @return the current drae.j.VDocument instance
   */
  public VDocument getVDocument() {
    return vDocument;
  }

  // @SuppressWarnings("unused")
  // private void setVDocument(VDocument vdoc) {
  // this.vDocument = vdoc;
  // }

  public WorkingSet getHeadWorkingSet() {
    return headWS;
  }

  /**
   * Get the first VText item in the VDocument at (x, y)
   * 
   * @param x
   * @param y
   * @return
   */
  public El getElAt(int x, int y) {
    
    for (El el : currentWS.getItems()) {
      BoundingBox bb = (BoundingBox) el.getBbox();
      if (x >= bb.getMinX() && 
          x <= bb.getMaxX() && 
          y >= bb.getMinY() && 
          y <= bb.getMaxY())  {
        return el;
      }
    }
    return null;
  }

  public El getElAt(Point p) {
    return this.getElAt(p.x, p.y);
  }

  /**
   * Return a list of working sets that contain the given x,y Useful for
   * detecting mouse events
   * 
   * @param x
   * @param y
   * @return the list of working sets
   */
  public List<WorkingSet> getWSAt(int x, int y) {
    List<WorkingSet> ret = new ArrayList<WorkingSet>();
    for (WorkingSet child : currentWS.getChildren()) {
      Rectangle bb = (Rectangle) child.getBboxWide();
      if (x >= bb.getMinX() && x <= bb.getMaxX() && y >= bb.getMinY() && y <= bb.getMaxY()) {
        ret.add(child);
      }
    }
    return ret;
  }

  public List<WorkingSet> getWSAt(Point p) {
    return this.getWSAt(p.x,p.y);
  }
  
  /**
   * Return a list of working sets that have an edge at the given x,y Useful for
   * detecting resize events
   * 
   * @param x
   * @param y
   * @return the list of working sets
   */
  
  public List<WorkingSet> getWSEdgeAt (int x, int y) {
    return this.getWSAt(new Point(x, y));
  }
  
  public List<WorkingSet> getWSEdgeAt (Point pt) {
   // Point pt = new Point(x, y);
    List<WorkingSet> ret = new ArrayList<WorkingSet>();
    for (WorkingSet child : currentWS.getChildren()) {
      Rectangle bbw = (Rectangle) child.getBboxWide();
      Rectangle bbs = (Rectangle) child.getBboxSmall();
      if (bbw.contains(pt) && !bbs.contains(pt)) {
        ret.add(child);
      }
    }
    return ret;
  }

  /**
   * get a list of drae.j.VisualElement.El objects that are FULLY inside of the
   * rectangle
   * 
   * @param dragRectDescaled
   *          the rectangle
   * @return
   * 
   */

  @SuppressWarnings("unchecked")
  public List<El> getElsIn(int page, Rectangle2D dragRectDescaled) {

    List<El> ret = new ArrayList<El>();
    VPage pg = ((List<VPage>) this.vDocument.getItems()).get(page - 1);
    List<El> els = (List<El>) pg.getItems();
    for (El el : els) {
      BoundingBox bb = (BoundingBox) el.getBbox();
      if (dragRectDescaled.contains(new Rectangle(bb.getBounds()))) {
        ret.add(el);
      }
    }
    return ret;
  }

  public int getOffsetX() {
    return offsetX;
  }

  public void setOffsetX(int offsetX) {
    this.offsetX = offsetX;
  }

  public int getOffsetY() {
    return offsetY;
  }

  public void setOffsetY(int offsetY) {
    this.offsetY = offsetY;
  }

  /*
   * public List<El> getSelected(){ return selectedEls; }
   */
  
  /**
   * Create a new working set from the currently-selected Visual Elements.
   * The new working set is a child of the current working set
   * 
   * @return The newly created working set
   */
  public WorkingSet createWSFromSel() {
    WorkingSet newWS = currentWS.createChild();
    for (Object el : selectedEls) {
      if (el instanceof El)
        newWS.addItem((El) el);
    }
    return newWS;
  }

  /**
   * Create a new working set from the given Visual Element array.
   * The new working set is a child of the current working set
   * 
   * @return The newly created working set
   */
  public WorkingSet createWSFromSel (Object[] selectedEls) {
    WorkingSet newWS = currentWS.createChild();
    for (Object el : selectedEls) {
      if (el instanceof El)
        newWS.addItem((El) el);
    }
    return newWS;
  }

  /**
   * Delete the current working set. Also sets the current working set to be the
   * parent of the old working set.
   */
  public void deleteCurrentWS() {
    WorkingSet oldWS = currentWS;
    currentWS = oldWS.getParent();
    currentWS.getChildren().remove(oldWS);
  }

  /**
   * Deletes the given working set and all of its children from the WorkingSet
   * tree. If the current working set would be deleted, then the current working
   * set switches to the parent of the deleted node.
   * 
   * @param ws
   *          the working set to be deleted
   */
  public void deleteWS(WorkingSet ws) {
    // check if null
    if (ws == null)
      return;
    // check if we're trying to delete a page
    if (ws.getParent() == headWS)
      return;
    // check if we're deleting something above the current workingset
    if (currentWS == ws || currentWS.hasAncestor(ws)) {
      currentWS = ws.getParent();
      currentWS.getChildren().remove(ws);
    } else { // just delete it
      ws.getParent().getChildren().remove(ws);
    }
  }

  public Rectangle2D getBBFromImg(String filename) {
    return this.imageBBMap.get(filename);
  }

  /**
   * Get access to the DataManagerView class associated with this instance of
   * DataManager
   * 
   * @return DataManagerView
   */
  //public DataManagerView getView() {
  //  return view;
 // }

  public WorkingSet getPageWS(int page) {
    if (page < 1 || page > getHeadWorkingSet().getChildren().size()) {
      return null;
    }
    return getHeadWorkingSet().getChildren().get(page - 1);
  }

  public BufferedImage getPageImage(int pageNumber) {
    if (pageNumber > pageImageList.length || pageNumber < 1) {
      System.out.println("Can't get page " + pageNumber + ". Must be between 1 and " + pageImageList.length + ".");
      return null;
    }
    if (pageImageStatus[pageNumber - 1] == -1) {
      try {
        loadImage(pageNumber - 1);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return defaultImage;
    } else if (pageImageStatus[pageNumber - 1] == 0) {
      return defaultImage;
    } else
      return pageImageList[pageNumber - 1];
  }

  public BufferedImage getPageIcon(int pageNumber) {
    int numPages = this.getSize();
    if (pageNumber > numPages || pageNumber < 1) {
      System.out.println("Can't get icon " + pageNumber + ". Must be between 1 and " + numPages + ".");
      return null;
    }
    if (pageImageStatus[pageNumber - 1] == -1) {
      try {
        loadImage(pageNumber - 1);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return defaultImage;
    } else if (pageImageStatus[pageNumber - 1] == 0) {
      return defaultImage;
    } else
      // Return default for now -- need to fix later.
      return defaultImage;
      //  return pageIconList[pageNumber - 1];
  }

  private void loadImage(final int pageNumber) throws IOException {
    pageImageStatus[pageNumber] = 0; // loading

    SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
      @Override
      protected BufferedImage doInBackground() throws Exception {
        if (Doc.useGhostscript()) {
          return ImageIO.read(Doc.getPageImageFor(getPdfFile(), pageNumber + 1));
        } else {
          PDDocument document = PDDocument.load(getPdfFile());
          // List<PDPage> pageList =
          // document.getDocumentCatalog().getAllPages();
          // PDPageTree pgtre.getPage(pageIndex)
          PDFRenderer renderer = new PDFRenderer(document);
          return renderer.renderImageWithDPI(pageNumber, 600);
        }
      }

      // Can safely update the GUI from this method.
      protected void done() {
        BufferedImage image;
        try {
          image = get();
          double aspect = (double) image.getWidth() / (double) image.getHeight();
          BufferedImage iconImage = new BufferedImage(128, (int) (128 / aspect), BufferedImage.TYPE_INT_RGB);
          Graphics2D g = iconImage.createGraphics();
          g.drawImage(image, 0, 0, 128, (int) (128 / aspect), null);
          setImage(image, pageNumber);
   //       setIcon(iconImage, pageNumber);
          pageImageStatus[pageNumber] = 1; // loading
          setChanged();// Observable Pattern: inform gui that images have
                       // changed
          notifyObservers();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (ExecutionException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }

    };

    worker.execute();
  }

  public void setImage(BufferedImage image, int pageNum) {
    if (pageNum >= pageImageList.length || pageNum < 0) {
      System.out.println("Can't set page " + pageNum + ". Must be between 1 and " + pageImageList.length + ".");
      return;
    }
    pageImageList[pageNum] = image;

  }

 // public void setIcon(BufferedImage image, int pageNum) {
 //   int numPages = this.getSize();
 //   if (pageNum >= numPages || pageNum < 0) {
 //     System.out.println("Can't set icon " + pageNum + ". Must be between 1 and " + numPages + ".");
 //     return;
 //   }
 //   pageIconList[pageNum] = image;
//
//  }

  public void AnalyzeEvidence() {
    // TODO Auto-generated method stub

    // eg.loadGeneGazetteer(this.getPageWS(view.getCurrentPage()));

    LayerList ll = currentWS.getLayerList();
    for (String layerName : ll.keySet()) {
      if (layerName.contains("Labeled table")) {
        VTable table = (VTable) ll.getLayerByName(layerName).getRep().get(0);
        eg.gatherEvidence(table);

      }
    }

  }

  public void PreprocessDocument() {
    setPreprocessState(0);// underway
    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
      protected Void doInBackground() throws Exception {
        eg.loadGeneGazetteer(getHeadWorkingSet());
        return null;

      }

      protected void done() {

        setPreprocessState(1);
        setChanged();
        notifyObservers();
      }
    };
    worker.execute();

  }

  public static void main(String[] args) {

  }

  public int getPreprocessState() {
    return preprocessState;
  }

  public void setPreprocessState(int preprocessState) {
    this.preprocessState = preprocessState;
  }

  // TODO - Make this a static method operating in a functional way.
  //        Might also make it a WorkingSet method, rather than a DataManager
  //         one.
  
  public WorkingSet mergeSelection() {
    
    WorkingSet merged = null;
    
    List<WorkingSet> toMerge = new ArrayList<WorkingSet>();
    
    for (Object obj : getSelectedEls()) {
      
      if (obj instanceof WorkingSet) {
        toMerge.add((WorkingSet) obj);
      }
    
    }
    
    if (toMerge.size() > 1) {
      Rectangle combined = toMerge.get(0).getBbox();
      String name = toMerge.get(0).getName() + "_MERGE";
      Set<String> tags = new HashSet<String>();
      WorkingSet parent = toMerge.get(0).getParent();
    
      for (WorkingSet ws : toMerge) {
        combined.add(ws.getBbox());
        tags.addAll(ws.getTags());
        parent.getChildren().remove(ws);
      }
      
      merged = parent.createChild(name, combined);
      for (String tag : tags) {
        merged.addTag(tag);
      }
    }
    return merged;
  }

  public void reloadWorkingSets() {
    WorkingSet top = new WorkingSet(null, "document");
    top.setFilename(this.getHeadWorkingSet().getFilename());
    reloadWorkingSets(this.getHeadWorkingSet(), top);
    int page = 1; // KLUDGE - view.getCurrentPage();
    this.setHeadWorkingSet(top);
    currentWS = top.getChildren().get(page - 1);

  }

  private void setHeadWorkingSet(WorkingSet top) {
    headWS = top;
  }

  public void reloadWorkingSets(WorkingSet oldWS, WorkingSet newWS) {

    for (WorkingSet oldChild : oldWS.getChildren()) {
      WorkingSet newChild = newWS.createChild(oldChild.getName());
      for (El el : this.getElsIn(oldChild.getPage(), oldChild.getBbox())) {
        newChild.addItem(el);
      }
      for (Line2D line : oldChild.getSeparators()) {
        newChild.addSeparator(line);
      }
      newChild.setPage(oldChild.getPage());
      newChild.setTags(oldChild.getTags());
      reloadWorkingSets(oldChild, newChild);

    }

  }

  @SuppressWarnings("unchecked")
  public void addSeparator(int page, Line2D line) {
    // TODO Auto-generated method stub
    this.getPageWS(page).addSeparator(line);
    ((List<VPage>) getVDocument().getItems()).get(page - 1).splitAtSeparator(line);
    this.reloadWorkingSets();

  }

  public List<Line2D> getSeparators(int page) {
    return this.getPageWS(page).getSeparators();
  }

  public void commandLine(CommandLineValues cmd) {
    if (cmd.isHelp())
      return;
    if (!cmd.hasInput()) {
      return;
    }
    if (cmd.getFile().isDirectory()) {
      loadClojure();
      System.out.println("Converting all files in directory " + cmd.getFile());

      for (String subStr : cmd.getFile().list()) {
        subStr = cmd.getFile().getAbsolutePath() + "/" + subStr;
        File sub = new File(subStr);
        if (sub.exists() && sub.getName().endsWith(".pdf")) {
          processRepTablesInFile(sub, cmd.getOutFile());
        }
      }
    } else if (cmd.getFile().exists() && cmd.getFile().getName().endsWith(".pdf")) {
      loadClojure();
      processRepTablesInFile(cmd.getFile(), cmd.getOutFile());
    } else {
      System.out.println("No file found: " + cmd.getFile());
      Path currentRelativePath = Paths.get("");
      String s = currentRelativePath.toAbsolutePath().toString();
      System.out.println("Use absolute path or relative path from:");
      System.out.println(s);
      printHelp();
    }

  }

  private void processRepTablesInFile(File pdf, File out) {
    System.out.println("Processing file " + pdf);

    int ext = pdf.getAbsolutePath().lastIndexOf('.');
    String fileStr = pdf.getAbsolutePath().substring(0, ext);
    File jsonFile = new File(fileStr + ".json");
    if (!jsonFile.exists()) {
      // System.out.println("No JSON file for " +pdf);
      return;
    }
    System.out.println("Converting file " + pdf);
    this.setPdfFile(pdf, true);
    Doc.restoreWSfromOverlay(this, jsonFile);
    for (WorkingSet pg : this.getHeadWorkingSet().getChildren()) {
      for (WorkingSet table : pg.getChildrenWithTag("TABLE")) {
        @SuppressWarnings("unchecked")
        List<Layer> layers = (List<Layer>) Table.applyLayerProducer("simple-table", table);
        System.out.println("Created " + layers.size() + " layers");
        PrintWriter writer;
        try {
          if (out == null) {
            out = new File(fileStr + "." + table.getName() + ".json");
          }
          writer = new PrintWriter(out, "UTF-8");
          System.out.println("Writing to " + out);
          for (Layer layer : layers) {
            writer.println(Table.layerRepToJSON(layer.getRep()));
          }
          writer.close();
        } catch (FileNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }
    }
    // for(Entry prod : (List<Entry>)Table.allLayerProducers()){
    // System.out.println(prod.key + " " + prod.doc);
    // }

  }

  private void printHelp() {
    System.out.println("Big Mechanism Table Extraction Interface");
    System.out.println("Usage: java -jar draegui.jar <options>");
    System.out.println("java -jar draegui.jar                    launch GUI.");
    System.out.println("java -jar draegui.jar -h                 print this message");
    System.out.println("java -jar draegui.jar <filename.pdf>     save rep info");
    System.out.println("java -jar draegui.jar <directory>        save rep info for all valid pdfs");
  }

  private void loadClojure() {
    System.out.println("loading clojure core");
    IFn require = Clojure.var("clojure.core", "require");
    require.invoke(Clojure.read("drae.core"));
    IFn populateFn = Clojure.var("drae.core", "populate-gui-tables");
    populateFn.invoke();
  }
}
