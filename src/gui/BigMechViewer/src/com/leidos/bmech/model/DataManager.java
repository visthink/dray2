/**
 * The DataManager class manages the working sets, analyses, and selected items in a 
 * given VDcoument. 
 * 
 * @author Daniel Powell
 * @author Ron Ferguson
 */
package com.leidos.bmech.model;

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

import javax.swing.SwingWorker;
import javax.swing.event.ListDataListener;
import javax.swing.ListModel;

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
  private WorkingSet               headWS;          // Top working set (of all working sets).
  public  WorkingSet               currentWS;       // Current working set.
  public  int                      currentPage;     // Current page.
  public  List<El>                 selectedEls;     // Currently selected elements.

 // private Map<String, Rectangle2D> imageBBMap;
  BufferedImage                    defaultImage;
  EvidenceGatherer                 eg;
 // int                              preprocessState; // -1: not started; 0:
                                                    // underway; 1: done
  List<ListDataListener> listeners;
  
  public DataManager() {
    
    super();
    
  //  imageBBMap = new HashMap<String, Rectangle2D>();
    defaultImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    eg = new EvidenceGatherer();
 //   preprocessState = -1;// not started
    listeners = new ArrayList<ListDataListener>();
    currentPage = 0;
    
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
    setChanged();
    notifyObservers();
  }
  
  public int getCurrentPage () {
    return currentPage;
  }
  
  public void setCurrentPage (int p) {
   
   // System.out.print("Setting the page to "+p);
    int numPages = getSize();
    
    if ((p >= 0) && (p <= numPages)) { // Check bounds
       currentPage = p;
       setChanged();
       notifyObservers();
    } else {
      System.out.println("Could not set the current page.");
    }
    
  }
  
  // Interface ModelList methods
  // -----------------------------------------------------------
  public VPage getElementAt(int pagenum) {
    return this.getPages().get(pagenum);
  }
  
  public int getSize() {
    return this.getPages().size();
  }
  
  public WorkingSet getCurrentWS () {
     return currentWS;
  }
  
  public void setCurrentWS (WorkingSet ws) {
    currentWS = ws;
    setChanged();
    notifyObservers();
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
    vDocument = doc;
    headWS = new WorkingSet(null, "document");
  //  headWS.setFilename((String) doc.getFilename());
    currentWS = headWS;
    @SuppressWarnings("rawtypes")
    List vPages = (List) vDocument.getItems();
   
    for (int i = 0; i < vPages.size(); i++) {
      createPageWS(i);
    }

  }

  /**
   * pdfFilenameCheck
   * @param file
   * @return true if file passes check.
   */
  private boolean pdfFileCheck (File file) {
    if (file == null) {
      System.err.println("ERROR: PDF filename is null.");
      return false;
    } else if (!file.exists()) {
      System.err.println("ERROR: PDF filename " + file + "doesn't exist.");
      return false;
    } else {
      return true;
    }
  }
  
  /**
   * load the page images from pdf and load the vdocument data from a closure
   * call. param lazyLoadImages -- set to true if images should only be loaded
   * when they are used
   */
  public void loadFromPdf(boolean lazyLoadImages) {
    
 //   setPreprocessState(-1);

    if (!pdfFileCheck(pdfFile)) { return; }
    
    System.out.println(pdfFile);
    
    vDocument = (VDocument) Doc.getVDocument(pdfFile);
    
    headWS = new WorkingSet(null, "document");
  //  try {
  //    headWS.setFilename(pdfFile.getCanonicalPath().replace('\\', '/'));
  //  } 
  //  catch (IOException e1) {
  //    e1.printStackTrace();
  //  } 
    
    currentWS = headWS;
    
    @SuppressWarnings("rawtypes")
    
    List vPages = (List) vDocument.getItems();
   
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
     //     imageBBMap.put(path.getName(), (BoundingBox) image.getBbox());
        }

      }
    }

    pageWS.setPage(pageIndex + 1);
  }

  public LayerList getLayerList() {
    if (currentWS == null) {
      return null;
    } else {
      return currentWS.getLayerList();
    }
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

    if ((ws != null) && (ws.getParent() != headWS)) {

      // check if we're deleting something above the current workingset
      if (currentWS == ws || currentWS.hasAncestor(ws)) {
        currentWS = ws.getParent();
        currentWS.getChildren().remove(ws);
      } else { // just delete it
        ws.getParent().getChildren().remove(ws);
      }
    }
  }


  public WorkingSet getPageWS(int page) {
    List<WorkingSet> pageWSets = getHeadWorkingSet().getChildren();
    if (page < 1 || page > pageWSets.size()) {
      return null;
    }
    return pageWSets.get(page);
  }





  public static void main(String[] args) {

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
   // top.setFilename(this.getHeadWorkingSet().getFilename());
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
    
    this.getPageWS(page).addSeparator(line);
    ((List<VPage>) getVDocument().getItems()).get(page - 1).splitAtSeparator(line);
    this.reloadWorkingSets();
    
    setChanged();
    notifyObservers();

  }

  public List<Line2D> getSeparators(int page) {
    return this.getPageWS(page).getSeparators();
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


  }


}
