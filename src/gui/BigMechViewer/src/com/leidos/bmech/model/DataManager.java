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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import com.leidos.bmech.analysis.EvidenceGatherer;
import com.leidos.bmech.view.DataManagerView;

import dray.j.BoundingBox;
import dray.j.Doc;
import dray.j.Producer.Table;
import dray.j.VisualElement.El;
import dray.j.VisualElement.VDocument;
import dray.j.VisualElement.VImage;
import dray.j.VisualElement.VPage;
import dray.j.VisualElement.VTable;
import dray.j.VisualElement.VText;

/**
 * This class is responsible for managing all of the data, as well as
 * interfacing with the external code (e.g. Clojure)
 * 
 * @author powelldan
 *
 */
public class DataManager extends Observable {

  private File                     pdfFile;
  private VDocument                vDocument;
  private BufferedImage[]          pageIconList;
  private BufferedImage[]          pageImageList;
  private int[]                    pageImageStatus;
  private int                      offsetX;
  private int                      offsetY;
  private WorkingSet               headWS;
  private Map<String, Rectangle2D> imageBBMap;
  private DataManagerView          view;
  BufferedImage                    defaultImage;
  EvidenceGatherer                 eg;
  int                              preprocessState; // -1: not started; 0:
                                                    // underway; 1: done

  public DataManager() {
    super();
    pageIconList = new BufferedImage[0];
    pageImageList = new BufferedImage[0];
    pageImageStatus = new int[0];
    view = new DataManagerView(this);
    imageBBMap = new HashMap<String, Rectangle2D>();
    defaultImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    eg = new EvidenceGatherer();
    preprocessState = -1;// not started
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
   * initialize Datamanager with data from pdf. Don't load images
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

  public void setView(DataManagerView v) {
    view = v;
  }

  public void loadFromVDocument(VDocument doc) {
    // call clojure to populate the VDocument
    vDocument = doc;
    headWS = new WorkingSet(null, "document");
    headWS.setFilename((String) doc.getFilename());
    view.setCurrentWS(headWS);
    @SuppressWarnings("rawtypes")
    List vPages = (List) vDocument.getItems();
    pageIconList = new BufferedImage[vPages.size()];
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
    view.setCurrentWS(headWS);
    @SuppressWarnings("rawtypes")
    List vPages = (List) vDocument.getItems();
    pageImageList = new BufferedImage[vPages.size()];
    pageIconList = new BufferedImage[vPages.size()];
    pageImageStatus = new int[vPages.size()];
    for (int i = 0; i < vPages.size(); i++) {
      pageImageStatus[i] = -1;
      pageImageList[i] = defaultImage;
      pageIconList[i] = defaultImage;
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
      if (items.get(j) instanceof dray.j.VisualElement.El) {
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
    if (view.getCurrentWS() == null)
      return null;
    return view.getCurrentWS().getLayerList();
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

  public BufferedImage[] getPageIconList() {
    return pageIconList;
  }

  public void setPageIconList(BufferedImage[] pageIconList) {
    this.pageIconList = pageIconList;
  }

  @SuppressWarnings("unused")
  private void runBEE() {
    /*
     * String dataSubDir =
     * FilenameUtils.removeExtension(pdfFile.getAbsolutePath()) + ".xml_data";
     * System.out.println(dataSubDir);
     */
  }

  /**
   * Get a reference to the dray.j.VDocument instance currently being used by
   * the GUI
   * 
   * @return the current dray.j.VDocument instance
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
    
    for (El el : view.getCurrentWS().getItems()) {
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
    for (WorkingSet child : view.getCurrentWS().getChildren()) {
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
    for (WorkingSet child : view.getCurrentWS().getChildren()) {
      Rectangle bbw = (Rectangle) child.getBboxWide();
      Rectangle bbs = (Rectangle) child.getBboxSmall();
      if (bbw.contains(pt) && !bbs.contains(pt)) {
        ret.add(child);
      }
    }
    return ret;
  }

  /**
   * get a list of dray.j.VisualElement.El objects that are FULLY inside of the
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
   * creates a new working set from the selected Visual Elements the new working
   * set is a child of the current working set
   * 
   * @return the newly created working set
   */
  public WorkingSet createWSFromSel() {
    WorkingSet newWS = view.getCurrentWS().createChild();
    for (Object el : view.getSelected()) {
      if (el instanceof El)
        newWS.addItem((El) el);
    }
    return newWS;

  }

  /**
   * Delete the current working set. also sets the current working set to be the
   * parent of the old working set
   */
  public void deleteCurrentWS() {
    WorkingSet oldWS = view.getCurrentWS();
    view.setCurrentWS(oldWS.getParent());
    view.getCurrentWS().getChildren().remove(oldWS);
  }

  /**
   * Deletes the given working set and all of its children from the WorkingSet
   * tree. If the current working set would be deleted, then the current working
   * set switches to the parent of the deleted node.
   * 
   * @param victim
   *          -- the working set to be deleted
   */
  public void deleteWS(WorkingSet victim) {
    // check if null
    if (victim == null)
      return;
    // check if we're trying to delete a page
    if (victim.getParent() == headWS)
      return;
    // check if we're deleting something above the current workingset
    if (view.getCurrentWS() == victim || view.getCurrentWS().hasAncestor(victim)) {
      view.setCurrentWS(victim.getParent());
      view.getCurrentWS().getChildren().remove(victim);
    } else { // just delete it
      victim.getParent().getChildren().remove(victim);
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
  public DataManagerView getView() {
    return view;
  }

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
    if (pageNumber > pageIconList.length || pageNumber < 1) {
      System.out.println("Can't get icon " + pageNumber + ". Must be between 1 and " + pageIconList.length + ".");
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
      return pageIconList[pageNumber - 1];
  }

  private void loadImage(final int pageNumber) throws IOException {
    pageImageStatus[pageNumber] = 0; // loading

    System.out.println("Entering loadImage for page number " + pageNumber);
    
    SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
      
      @Override
      protected BufferedImage doInBackground() throws Exception {
        System.out.println("-- Running the image loading in background for page " + pageNumber);
        if (Doc.useGhostscript()) {
          System.out.println("Using Ghostscript");
          return ImageIO.read(Doc.getPageImageFor(getPdfFile(), pageNumber + 1));
        } else {
          System.out.println("0. Loading the PDF file.");
          PDDocument document = PDDocument.load(getPdfFile());
          // List<PDPage> pageList =
          // document.getDocumentCatalog().getAllPages();
          // PDPageTree pgtre.getPage(pageIndex)
          PDFRenderer renderer = new PDFRenderer(document);
          System.out.println("2. About to render the page.");
          return renderer.renderImageWithDPI(pageNumber, 600);
          /*
           * ImageFilter filter = new RGBImageFilter() { int transparentColor =
           * Color.white.getRGB() | 0xFF000000;
           * 
           * public final int filterRGB(int x, int y, int rgb) { if ((rgb |
           * 0xFF000000) == transparentColor) { return 0x00FFFFFF & rgb; } else
           * { return rgb; } } };
           * 
           * ImageProducer filteredImgProd = new
           * FilteredImageSource(image.getSource(), filter); Image
           * transparentImg =
           * Toolkit.getDefaultToolkit().createImage(filteredImgProd);
           * BufferedImage ret = new
           * BufferedImage(transparentImg.getWidth(null),
           * transparentImg.getHeight(null), BufferedImage.TYPE_INT_ARGB);
           * Graphics2D bGr = ret.createGraphics();
           * bGr.drawImage(transparentImg, 0, 0, null); bGr.dispose();
           * 
           * return ret;
           */

          // return renderer.renderImage(pageNumber);
          // return document.getPage(pageNumber).convertToImage();
        }
      }

      // Can safely update the GUI from this method.
      protected void done() {
      //  System.out.println("Calling DataManager.done() for page " + pageNumber);
      //  System.out.println("Value of view is "+view.toString());
        BufferedImage image;
        try {
       //   System.out.println("Made it to 0.");
          // Let's try to check if the computation was consider done or was cancelled.
       //   System.out.println("IsDone: " + isDone() + " isCancelled: " + isCancelled());
          if (isCancelled()) return; // Abort if the computation was cancelled.
       //   System.out.println("Made it to 0.3");
          image = get();
       //   System.out.println("Made it to 0.5.");
          double aspect = (double) image.getWidth() / (double) image.getHeight();
       //   System.out.println("Made it to 1.");
          BufferedImage iconImage = new BufferedImage(128, (int) (128 / aspect), BufferedImage.TYPE_INT_RGB);
       //   System.out.println("Made it to 2.");
          Graphics2D g = iconImage.createGraphics();
      //    System.out.println("Made it to 3.");
          g.drawImage(image, 0, 0, 128, (int) (128 / aspect), null);
          setImage(image, pageNumber);
          setIcon(iconImage, pageNumber);
      //    System.out.println("Made it to 4.");
          pageImageStatus[pageNumber] = 1; // loading
          setChanged();// Observable Pattern: inform gui that images have
                       // changed
      //    System.out.println("Made it to 5.");
          notifyObservers();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          System.out.println("Interrupted exception in DataManager.done()");
          e.printStackTrace();
        } catch (ExecutionException e) {
          // TODO Auto-generated catch block
          System.out.println("Execution exception in DataManager.done()");
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

  public void setIcon(BufferedImage image, int pageNum) {
    if (pageNum >= pageIconList.length || pageNum < 0) {
      System.out.println("Can't set icon " + pageNum + ". Must be between 1 and " + pageIconList.length + ".");
      return;
    }
    pageIconList[pageNum] = image;

  }

  public void AnalyzeEvidence() {
    // TODO Auto-generated method stub

    // eg.loadGeneGazetteer(this.getPageWS(view.getCurrentPage()));

    LayerList ll = view.getCurrentWS().getLayerList();
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

  public WorkingSet mergeSelection() {
    WorkingSet merged = null;
    List<WorkingSet> toMerge = new ArrayList<WorkingSet>();
    for (Object obj : view.getSelected()) {
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
    int page = view.getCurrentPage();
    this.setHeadWorkingSet(top);
    this.view.setCurrentWS(top.getChildren().get(page - 1));

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
    System.out.println("Usage: java -jar draygui.jar <options>");
    System.out.println("java -jar draygui.jar                    launch GUI.");
    System.out.println("java -jar draygui.jar -h                 print this message");
    System.out.println("java -jar draygui.jar <filename.pdf>     save rep info");
    System.out.println("java -jar draygui.jar <directory>        save rep info for all valid pdfs");
  }

  private void loadClojure() {
    System.out.println("loading clojure core");
    IFn require = Clojure.var("clojure.core", "require");
    require.invoke(Clojure.read("dray.core"));
    IFn populateFn = Clojure.var("dray.core", "populate-gui-tables");
    populateFn.invoke();
  }
}
