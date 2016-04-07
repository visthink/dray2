package com.leidos.bmech.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
//import java.awt.RenderingHints;
import java.awt.Stroke;
//import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
//import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.leidos.bmech.model.Layer;
import com.leidos.bmech.model.WorkingSet;
import com.leidos.bmech.model.DataManager;
//import com.leidos.bmech.view.DataManagerView;

import drae.j.BoundingBox;
import drae.j.VisualElement.*;

/**
 * the drawing canvas used to draw the document and serve as the most used
 * interface with the user. Gets all of the data to be drawn from the
 * DataManager
 * 
 * @author powelldan
 *
 */

public class DocumentCanvas extends JPanel 
                            implements Observer {
  private static final long serialVersionUID = 1L;

  // These are set during construction, and then unchanged.
 // final private ViewerApp           mainApp;
  final private DocumentCanvas      pThis;
  final private DataManager         dataManager;
  //final private DataManagerView     view;
  
  // List<DocumentElement>     currentlySelected;
  
 // DocumentElement           lastSelected;
 // El                        lastSelectedEl;
  
  private float             scale;         // Current scale.
  boolean                   mouseDown;
  Rectangle                 dragRect;      // Appears to be the current dragging rectangle.
  Point                     rightDragLast;
  boolean                   dragged;
  
  WorkingSet                lastWS;
  WorkingSet                resizingWS;
  
  int                       hResize;
  int                       vResize;
  Point                     offset = new Point(0,0);
  
  final static float        minScale         = 0.2f;
  final static float        maxScale         = 4.0f;
  final static int          buffer           = 20;

//  private boolean           retainViewZoom;
  
  public boolean            isSplitModeEnabled = false;
  public boolean            isQuickTagEnabled = false;
  
  public String             quickTag = "";
  
  BufferedImage[]           pageImages;  // Rendered pages
  
  PDFRenderer               renderer;    // Renderer for the page images.

  int                       currentPage; // Currently depicted page.
  
  boolean skipOld = true; // SKIP THE OLD CODE FOR NOW.
  
  
  /**
   * initialize variables and add mouse listeners
   * 
   * @param parent
   */
  //public DocumentCanvas (ViewerApp parent) {
 
 
 public DocumentCanvas (DataManager dm) {
   
    setBackground(Color.WHITE);
    pThis = this;
//    retainViewZoom    = false; // Not used.
    mouseDown         = false;
    scale             = 1.0f;
    dragged           = false;
   
    dataManager = dm;
 
    pageImages = new BufferedImage[dataManager.getSize()];
    
    renderer = makePDFRenderer();
     
    //currentlySelected = new ArrayList<DocumentElement>();
    
    addMouseListener(new MouseAdapter() {

      public void mouseClicked(MouseEvent e) { 
        handleMouseClicked(pThis,e);
      }
      
      public void mousePressed(MouseEvent e) {
        handleMousePressed(pThis, e);
      }
      
      public void mouseReleased(MouseEvent e) {
        handleMouseReleased(pThis, e);
        }
      
      }
    );
      
    addMouseWheelListener(new MouseWheelListener() {

      public void mouseWheelMoved(MouseWheelEvent e) {

        if (dataManager.getVDocument() != null) {
          if (e.getWheelRotation() < 0) { zoomIn(); } else { zoomOut(); }
        }
      } 
    });

    addMouseMotionListener(new MouseMotionListener() {

      
      @Override
      public void mouseDragged(MouseEvent e) {        
        handleMouseDragged(pThis, e);
      };

      @Override
      public void mouseMoved(MouseEvent e) {
        handleMouseMoved(pThis, e);
      };
    }
    );
    
    dm.addObserver(this);
      
 } // end mouseMotionListener 
      
//Method for Observer
  @Override
  public void update (Observable ob, Object arg) {
  //TODO Fill in what the update does
  // Always a DataManager.
    System.out.println("Calling the update function with obj " + ob.toString()
                        //+ " and arg " + arg.toString()
                        );
    DataManager dm = (DataManager) ob;
    int currentDMPage = dm.getCurrentPage();
 //   if (currentDMPage != this.currentPage) {
 //     this.drawPage(g2d, currentDMPage, maxScale);
 //   }
    if (currentDMPage != this.currentPage) {
      this.repaint();
    }
  }
  
  public void enableSplitMode() { isSplitModeEnabled = true; }
  
  public void disableSplitMode() { isSplitModeEnabled = false; }
  
  public void enableQuickTag() { isQuickTagEnabled = true; }
  
  public void disableQuickTag() { isQuickTagEnabled = false; }
  
  public String getQuickTag () { return quickTag; }
  
  public void setQuickTag (String tag) { quickTag = tag; }
  
  public DataManager getDataManager () {return dataManager; };
  
  public float getScale () { return scale; };
  
  public void setScale (float newScale) { scale = newScale; };
  
  // Methods for page rendering
  public BufferedImage getPageImage (int pageNum) {
    if (pageImages[pageNum] == null) {
      pageImages[pageNum] = this.renderPage(pageNum);
    };
    return pageImages[pageNum];
  }
  
  public void drawPage (Graphics2D g2d, int pageNum, float scale) {
    try {
      System.out.println("Drawing page in drawPage");
      this.renderer.renderPageToGraphics(pageNum, g2d, scale);
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.currentPage = pageNum;
  }
  
  public BufferedImage renderPage (int pageNum) {
    
    try {
      return renderer.renderImageWithDPI(pageNum, 600);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
  
  private PDFRenderer makePDFRenderer () {
    PDDocument pddoc = null;
    try {
      pddoc = PDDocument.load(dataManager.getPdfFile());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new PDFRenderer(pddoc);    
  }
  
  
  /**
   * Notify the DataManager that a rectangle has been drawn on
   * the canvas. Called by the DocumentCanvas object
   * @param dragRectDescaled the rectangle that was drawn on the canvas
   */
  public void rectangleDrawn(Rectangle2D dragRectDescaled) {
    
    List<El> els = dataManager.getElsIn(dataManager.getCurrentPage(), dragRectDescaled);

      if(!els.isEmpty()){
      //    List<El> selectedEls = dataManager.getSelectedEls();
      //    selectedEls.clear();
     //     selectedEls.addAll(els);
        dataManager.setSelectedEls(els);
        //selectedChanged();
        if (isQuickTagEnabled) {
        //TODO (getQuickTag());
        }
        repaint();
      }
  }
  

  public void rectangleDrawn(WorkingSet ws, Rectangle rect) {
      // TODO  Needs a lot of work, actually.rwf 2/27
      ws.resize(rect);
      // this.workingSetTree.reload();
      // viewWSUpdated();
  }

  public void drawCurrentDragRect (Graphics2D g2) {
    if (dragRect != null) {
      g2.setColor(Color.GREEN);
      g2.drawRect((int) (Math.min(dragRect.getMinX(), dragRect.getMaxX()) * scale),
          (int) (Math.min(dragRect.getMinY(), dragRect.getMaxY()) * scale), (int) (Math.abs(dragRect.width) * scale),
          (int) (Math.abs(dragRect.height) * scale));
    }      
  }
  
  //@Override
  //public void paintComponent(Graphics g) {
  //  Graphics2D g2;
  //  g2 = (Graphics2D) g;
 //   super.paintComponent(g);
  //  paintUsingVDocument(g);

//    drawCurrentDragRect(g2);
    // draw up button
   // g2.setColor(Color.DARK_GRAY);
    // g2.drawRect(2, 2, 50, 15);
  //  g2.drawString("[^]", 5, 5);

 // }

  @Override
  public void paintComponent (Graphics g) {
   // System.out.println("Calling paintComponent with " + g);
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g; // Ensure G2D.
    DataManager dm = this.getDataManager();
    int pageNum = dm.getCurrentPage();
    this.drawPage(g2d, pageNum, scale);
    // draw rectangles around child WSs
    drawChildWSRecursive(g2d, dm.getCurrentWS(), 1);
  }
  
  
  /**
   * Draw the VDocument objects on the canvas
   * 
   * @param g
   */
  public void paintUsingVDocument(Graphics g) {
   
    Graphics2D g2 = (Graphics2D) g;
    if (noDocumentLoaded()) {
      g2.drawString("No Document Loaded", 200, 200);
      return;
    }
    
  //  int x = offset.x; // Not used.
  //  int y = offset.y;
    
    // check if working set has changed so we can refocus if needed
   // if (dataManager.getCurrentWS() != lastWS) {
   //   lastWS = dataManager.getCurrentWS();
   //   if (!retainViewZoom) {
   //     scale = (float) Math.min(getVisibleRect().getWidth() / (lastWS.getBbox().getWidth() + scale * buffer * 2),
   //         getVisibleRect().getHeight() / (lastWS.getBbox().getHeight() + scale * buffer * 2));
   //     this.scrollRectToVisible(new Rectangle());
   //   }
   //   dragRect = null;
   //   retainViewZoom = false;
   // }
    
  //  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int pageNum = dataManager.getCurrentPage();
 //   @SuppressWarnings("unchecked")
 //   List<VPage> pages = (List<VPage>) dataManager.getVDocument().getItems();
 //   VPage page = (VPage) pages.get(pageNum); // not used
//    BoundingBox pagebb = (BoundingBox) page.getBbox(); // Not used.
  //  BufferedImage pageImage = dataManager.getPageImage(dataManager.getCurrentPage());
 //   double factor = (double) pageImage.getWidth() / pagebb.getWidth();
    
    // draw rectangles around child WSs
    drawChildWSRecursive(g2, dataManager.getPageWS(pageNum), 1);

    // draw image
//   for (El el : dataManager.getCurrentWS().getItems()) {
//     
//     BoundingBox bb = (BoundingBox) el.getBbox();

//      g2.drawImage(pageImage, 
//          (int) ((bb.getMinX() - offset.x) * scale), 
//          (int) ((bb.getMinY() - offset.y) * scale),
//          (int) ((bb.getMaxX() - offset.x) * scale), 
//          (int) ((bb.getMaxY() - offset.y) * scale), 
//          (int) (bb.x * factor) + x,
//          (int) (bb.y * factor) + y, 
//          (int) ((bb.x + bb.width) * factor) + x, 
//          (int) ((bb.y + bb.height) * factor) + y,
//          null);
//    }
    
    // draw working set and El boxes
    if (dataManager.getSelectedEls() != null) {
      g2.setColor(new Color(0f, 1f, 0f, .3f));
      for (Object obj : dataManager.getSelectedEls()) {
        
        if (obj != null) {
          Rectangle bb = null;
          if (obj instanceof El) {
            bb = ((BoundingBox) ((El) obj).getBbox()).getBounds();
          } else if (obj instanceof WorkingSet) {
            bb = ((WorkingSet) obj).getBboxWide();
          }
          if (bb != null) {
            g2.fill(new Rectangle((int) ((bb.getMinX() - offset.x) * scale), (int) ((bb.getMinY() - offset.y) * scale),
                (int) (bb.getWidth() * scale), (int) (bb.getHeight() * scale)));
          }
        }
      }
    }

    for (Line2D sep : dataManager.getPageWS(dataManager.getCurrentPage()).getSeparators()) {

      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setColor(Color.RED);
      Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 3 }, 0);
      g2d.setStroke(dashed);
      g2d.drawLine((int) ((sep.getX1() - offset.x) * scale), (int) ((sep.getY1() - offset.y) * scale),
          (int) ((sep.getX2() - offset.x) * scale), (int) ((sep.getY2() - offset.y) * scale));
      // gets rid of the copy
      g2d.dispose();

    }

    // List<Layer> layers = mainApp.getDataManager().getLayerList().
    Rectangle2D imageBB = new Rectangle();
    for (Layer layer : dataManager.getLayerList().values()) {
      if (layer.isHighlight()) {
        g2.setColor(UtiliBuddy.makeTransparent(layer.getColor(), 0.1f));

        for (El el : layer.getItems()) {
          if (el != null) {
            BoundingBox bb = (BoundingBox) el.getBbox();
            g2.fill(new Rectangle((int) ((bb.getMinX() - offset.x) * scale), (int) ((bb.getMinY() - offset.y) * scale),
                (int) (bb.getWidth() * scale), (int) (bb.getHeight() * scale)));
          }
        }

        // System.out.println(layer.getRep());
        for (Map<String, Object> hm : layer.getRep()) {
          // System.out.println("Rep " + hm.keySet());
          if (hm.containsKey("File")) {
   //         imageBB = dataManager.getBBFromImg(new File((String) hm.get("File")).getName());
            double bee2pdfX = imageBB.getWidth() / ((Long) hm.get("imageWidth")).doubleValue();
            double bee2pdfY = imageBB.getHeight() / ((Long) hm.get("imageHeight")).doubleValue();
            if (hm.containsKey("Blobs")) {
              @SuppressWarnings("rawtypes")
              List blobs = (List) hm.get("Blobs");
              for (Object obj : blobs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> blob = (Map<String, Object>) obj;
                g2.setColor((Color) blob.get("color"));
                g2.setStroke(new BasicStroke(3));
                BoundingBox bb = (BoundingBox) blob.get("bbox");
                g2.draw(new Rectangle((int) ((bb.getMinX() * bee2pdfX - offset.x + imageBB.getX()) * scale),
                    (int) ((bb.getMinY() * bee2pdfY - offset.y + imageBB.getY()) * scale),
                    (int) (bb.getWidth() * scale * bee2pdfX), (int) (bb.getHeight() * scale * bee2pdfY)));
              }
            }
          }
        }
      }
    }
  }

  private void drawChildWSRecursive(Graphics2D g2, WorkingSet parent, int depth) {
    g2.setStroke(new BasicStroke(2));
    for (WorkingSet child : parent.getChildren()) {

      // first delve deeper so that top most WS are drawn over children
      drawChildWSRecursive(g2, child, depth + 1);

      g2.setColor(UtiliBuddy.makeTransparent(child.getColor(), (float) 1.0 / (depth + 1)));
      Rectangle bb = child.getBboxWide();
      g2.fill(new Rectangle((int) ((bb.getMinX() - offset.x) * scale), (int) ((bb.getMinY() - offset.y) * scale),
          (int) (bb.getWidth() * scale), (int) (bb.getHeight() * scale)));
      // only draw WS title for first level
      if (depth == 1) {
        g2.drawString(child.getName(), (int) ((bb.getMinX() - offset.x) * scale),
            (int) ((bb.getMinY() - offset.y) * scale) - 2);
      }
    }
  }

  public void createSeparator(Line2D line) {
    dataManager.addSeparator(dataManager.getCurrentPage(), line);
//   workingSetTree.reload();
//  viewWSUpdated();
  }

 // private Point offset() {
 //   Point p = lastWS.getBbox().getLocation();
 //   p.translate(-buffer, -buffer);
 //   return p;
 // }
  
  //private int offX() {
  //  return offset().x;
 // }

//  private int offY() {
//    return offset().y;
//  }

  private void setZoom(float zoom) {
    this.scale = zoom;
    @SuppressWarnings("rawtypes")
    List pages = (List) dataManager.getVDocument().getItems();
    VPage page = (VPage) pages.get(dataManager.getCurrentPage() - 1);
    Rectangle2D pagebb = (drae.j.BoundingBox) page.getBbox();
    pThis.setPreferredSize(scaleUp(new Dimension((int)pagebb.getWidth(), (int)pagebb.getHeight())));
    pThis.revalidate();
    repaint();
  }

  private void zoomIn() {
    setZoom(Math.min(scale + 0.1f, maxScale));
  }

  private void zoomOut() {
    setZoom(Math.max(scale - 0.1f, minScale));
  }

 
  /*
   * HELPER FUNCTIONS
   */
   
   /**
   * Scale a point to proper position on canvas given offset and scale.
   * 
   * @param p
   */
  private Point scaleToCanvas (Point p) {
    return addOffset(scaleDown(p));
  }
  
  private Point scaleDown (Point p) {
    return new Point((int)(p.x / scale), (int)(p.y / scale));
  }
  
//  @SuppressWarnings("unused")
  public Point scaleUp (Point p) {
    return new Point((int)(p.x * scale), (int)(p.y * scale));
  }
  
  public Dimension scaleUp (Dimension d) {
    return new Dimension((int)(d.width * scale), (int)(d.height * scale));
  }
  
  private Point addOffset (Point p) {
    return new Point(p.x + offset.x, p.y + offset.y);
  }
  
  @SuppressWarnings("unused")
  private Rectangle addOffset (Rectangle r) {
    Rectangle rnew = new Rectangle(r);
    rnew.translate(offset.x, offset.y);
    return rnew;
  }
  
  private static Rectangle normalizeRect (Rectangle r) {
    int minX = (int) Math.min(r.getMinX(), r.getMaxX());
    int minY = (int) Math.min(r.getMinY(), r.getMaxY());
    int h    = (int) Math.abs(r.getHeight());
    int w    = (int) Math.abs(r.getWidth());
    r.setRect(minX,minY,h,w);
    return r;
  }
  
  public boolean noDocumentLoaded () {
    return (dataManager.getVDocument() == null); 
  }
  
//** 
//* EVENT HANDLER CODE
//*
  
  public static void handleMouseClicked (DocumentCanvas cv, MouseEvent e) {
    
    if (cv.skipOld) {return;}; /// SKIP FOR NOW!
    
    DataManager dm = cv.getDataManager();
    Point clickLoc   = e.getPoint(); 
    int   clickCount = e.getClickCount();
    
    if (cv.noDocumentLoaded()) { return; }; // Exit if no doc loaded.
        
    El targetEl = dm.getElAt(cv.scaleToCanvas(clickLoc));
    
    List<WorkingSet> childrenClicked = dm.getWSAt(cv.scaleToCanvas(clickLoc));

    if (SwingUtilities.isLeftMouseButton(e)) {

      // if double click, drill down to child WS
      if (clickCount == 2) {
      
        // only drill down if unambiguous
        for (WorkingSet clicked : childrenClicked) {
          
          if (dm.getSelectedEls().contains(clicked)) {
            dm.setCurrentWS(childrenClicked.get(0));
           // mainApp.viewWSUpdated();
            break;
          }
        }
        
      } else if (clickCount == 1) {
      
        Object clickedObj = null;
        if (childrenClicked.size() > 0) {
          clickedObj = (El) childrenClicked.get(0);
        } else {
          dm.getSelectedEls().add(targetEl);
          clickedObj = (El) targetEl;
        }
        // KLUDGE
        clickedObj = (El) clickedObj;
        
        // if ctrl is pressed, don't clear the selection, just toggle the
        // clicked el
        
        List<El> selected = dm.getSelectedEls();
        if ((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK) {
          if (selected.contains(clickedObj)) {
            selected.remove(clickedObj);
          } else {
            selected.add((El)clickedObj);
          }
        } else {
          // clear selected and add the clicked el
          selected.clear();
          selected.add((El)clickedObj);
        }

       //TODO mainApp.selectedChanged();
       
    //    if (lastSelectedEl != null) {
    //   //TODO   mainApp.appendToLog(lastSelectedEl.toString());
    //    };
      }

    } else if (SwingUtilities.isRightMouseButton(e)) {
  //    boolean selectionClicked = false;
      // Object clickedObj = null;
      if (cv.dragRect != null) {
        Rectangle dragRectDescaled = new Rectangle(cv.dragRect);
        dragRectDescaled.translate(cv.offset.x,cv.offset.y);
//        Rectangle dragRectDescaled = new Rectangle((int) (dragRect.getX() + offX()),
//            (int) (dragRect.getY() + offY()), (int) (dragRect.getWidth()), (int) (dragRect.getHeight()));
   //     if (dragRectDescaled.contains(scaleToCanvas(e.getPoint()))) {
   //       selectionClicked = true;
  //      }
      }
  
      //TODO Need to put this code back eventually. Taken out due to mainApp.
  //    DocumentMenu menu;

  //    if (selectionClicked)
  //      menu = new DocumentMenu(mainApp, null, targetEl);
  //    else if (childrenClicked.size() > 0 && childrenClicked.get(0) instanceof WorkingSet) {
  //      menu = new DocumentMenu(mainApp, (WorkingSet) childrenClicked.get(0), targetEl);
  //    } else {
  //      menu = new DocumentMenu(mainApp, dataManager.getCurrentWS(), targetEl);
  //    }
  //    menu.show(e.getComponent(), e.getX(), e.getY());
    }
      
    cv.repaint();
  };

    
  public static void handleMousePressed(DocumentCanvas cv, MouseEvent e) {
    
    if (cv.skipOld) { return; }; // SKIP FOR NOW!
    
    
    DataManager dm = cv.getDataManager();
    
    // check if anything has been loaded
    System.out.format("Mouse pressed.\n");

    if (cv.noDocumentLoaded()) { return; }

    if (e.getButton() == MouseEvent.BUTTON1) {
      cv.mouseDown = true;
      // check to see if we should be resizing a working set boundary
      // these ints are the mouse point in ws space
      
   //   int wsX = (int) ((e.getX() / scale + offX()));
   //   int wsY = (int) ((e.getY() / scale + offY()));
      Point clickLoc = cv.scaleToCanvas(e.getPoint());

      List<WorkingSet> clickedWS = dm.getWSEdgeAt(clickLoc);

      if (clickedWS.size() > 0) {
        
        System.out.println(dm.getSelectedEls().contains(clickedWS.get(0)));

        if (dm.getSelectedEls().contains(clickedWS.get(0))) {
          cv.resizingWS = clickedWS.get(0);
          Rectangle tmp = new Rectangle(cv.resizingWS.getBbox());
          // checkToSee vertical vs Horizontal resize
          cv.hResize = cv.resizingWS.isHorizontalEdge(clickLoc.x);
          cv.vResize = cv.resizingWS.isVerticalEdge(clickLoc.y);

          cv.dragRect = tmp; // Offset bbox to get proper dragRect.
       //   dragRect.translate(-offset().x, -offset().y);
          cv.dragRect.translate(-cv.offset.x, -cv.offset.y);
          
//            dragRect = new Rectangle((int) (tmp.getX() - offX()), (int) (tmp.getY() - offY()), (int) (tmp.getWidth()),
//               (int) (tmp.getHeight()));
        }

      } else {
        Point mouseLoc = e.getPoint();
        cv.dragRect = new Rectangle(cv.scaleDown(mouseLoc));
        
      }
    } else if (SwingUtilities.isRightMouseButton(e)) {
      cv.rightDragLast = e.getLocationOnScreen();
      cv.setCursor(new Cursor(Cursor.MOVE_CURSOR));
    }
  }

  public static void handleMouseReleased(DocumentCanvas cv, MouseEvent e) {
    
    if (cv.skipOld) { return; };
    
    DataManager dm = cv.getDataManager();
    
    // check if anything has been loaded1
    if (cv.noDocumentLoaded()) { return; };
    
    if (e.getButton() == MouseEvent.BUTTON1) {
      
      if (cv.dragged) {
        
        // if ctrl not pressed, clear list

        // translate the rectangle from screen space to VDocument space
        Rectangle dragRectDescaled = new Rectangle(cv.dragRect);
        dragRectDescaled.translate(cv.offset.x, cv.offset.y);
//        Rectangle dragRectDescaled = new Rectangle((int) (dragRect.getX() + offX()),
//            (int) (dragRect.getY() + offY()), (int) (dragRect.getWidth()), (int) (dragRect.getHeight()));
        if (cv.resizingWS == null) {
          // create new WS
          if ((e.getModifiers() & ActionEvent.CTRL_MASK) != ActionEvent.CTRL_MASK) {
            dm.getSelectedEls().clear();

          }
          // normalize to eliminate possible negative height and width.
          dragRectDescaled = normalizeRect(dragRectDescaled);   
          
          if (cv.isSplitModeEnabled) {
          
            cv.createSeparator(new Line2D.Double(dragRectDescaled.getMinX(), dragRectDescaled.getMinY(),
                dragRectDescaled.getX(), dragRectDescaled.getMaxY()));
            // remember old viewport information, this will get lost when we
            // reload the workingsets based on the split
  //          cv.retainViewZoom = true; // not used.
            cv.repaint();

          } else {
            
            cv.rectangleDrawn(dragRectDescaled);
         
          }
        } else {
          
          // resize WS using rectangle
          cv.rectangleDrawn(cv.resizingWS, dragRectDescaled);
        }
      }
      cv.dragged = false;
      cv.mouseDown = false;
      cv.resizingWS = null;
      
    } else if (SwingUtilities.isRightMouseButton(e)) {
      
      cv.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); 
    }
  }

  
  public static void handleMouseDragged(DocumentCanvas cv, MouseEvent e) {
    
    if (cv.skipOld) { return; };
    
    Rectangle dragRect = cv.dragRect;
    float scale = cv.scale;
    boolean isSplitModeEnabled = cv.isSplitModeEnabled;
    
    if (cv.noDocumentLoaded()) { return;}
    
    // adjust the width of the rectangle and scale
    if (SwingUtilities.isLeftMouseButton(e)) {
      
      cv.dragged = true;
      // check if we are resizing a WS
      
      if (cv.resizingWS != null) {
        
        double x = dragRect.getMinX();
        double y = dragRect.getMinY();
        double h = dragRect.getHeight();
        double w = dragRect.getWidth();
        
        
        if (cv.hResize == 1) {
          w = (e.getX() / scale - x);
        } 
        if (cv.hResize == -1) {
          w = dragRect.getMaxX() - (e.getX() / scale);
          x = (e.getX() / scale);
        }
        if (cv.vResize == 1) {
          h = (e.getY() / scale - y);
        }
        if (cv.vResize == -1) {
          h = dragRect.getMaxY() - (e.getY() / scale);
          y = (e.getY() / scale);
        }
        
        // dont allow dragging inverse rectangle
        if (w > 0 && h > 0) {
          dragRect.setFrame(x, y, w, h);
        }

      } else if (isSplitModeEnabled) {
        // split line
        dragRect.setSize(0, (int) ((e.getY() / scale - dragRect.y)));

      } else {// regular drag
        dragRect.setSize((int) ((e.getX() / scale - dragRect.x)), (int) ((e.getY() / scale - dragRect.y)));
      }
    } else if (SwingUtilities.isRightMouseButton(e)) {
      JViewport viewPort = (JViewport) cv.getParent();
      double deltaX = cv.rightDragLast.getX() - e.getLocationOnScreen().getX();
      double deltaY = cv.rightDragLast.getY() - e.getLocationOnScreen().getY();
      Point vpp = viewPort.getViewPosition();
      vpp.translate((int) deltaX, (int) deltaY);
      cv.scrollRectToVisible(new Rectangle(vpp, viewPort.getSize()));
      cv.rightDragLast = e.getLocationOnScreen();
    }
    cv.repaint();
  }

  public static void handleMouseMoved (DocumentCanvas cv, MouseEvent e) {

    if (cv.skipOld) { return; };
    
    if (cv.lastWS == null) {
      return;
    }
    
    DataManager dm = cv.getDataManager();
    Point mouseLoc = cv.scaleToCanvas(e.getPoint());
    
    List<WorkingSet> clickedWS = dm.getWSEdgeAt(mouseLoc);

    if (clickedWS.size() > 0) {
      
      if (dm.getSelectedEls().contains(clickedWS.get(0))) {
        WorkingSet ws = clickedWS.get(0);
        int h = ws.isHorizontalEdge(mouseLoc.x);
        int v = ws.isVerticalEdge(mouseLoc.y);
        int cursorType = Cursor.DEFAULT_CURSOR;
        if (h != 0 && v == 0) {
          cursorType = Cursor.E_RESIZE_CURSOR;
        } else if (h == 0 && v != 0) {
          cursorType = Cursor.N_RESIZE_CURSOR;
        } else if ((h == 1 && v == 1) || (h == -1 && v == -1)) {
          cursorType = Cursor.SE_RESIZE_CURSOR;
        } else if ((h == 1 && v == -1) || (h == -1 && v == 1)) {
          cursorType = Cursor.NE_RESIZE_CURSOR;
        };
        cv.setCursor(new Cursor(cursorType));
        
      }
    }
  } // end mouseMoved

  //**
  //* Create a popup of this DocumentCanvas.
  //* Based on canvas-popup2 in Clojure.
  //*
  public JFrame popup () {
    
    DataManager dm = this.getDataManager();
    String filename = dm.getPdfFile().getName();
    JFrame frame = new JFrame(filename);
    frame.add(this);
    frame.setContentPane(this);
    frame.setPreferredSize(new Dimension(400,300));
    frame.setSize(new Dimension(400,300));
//    frame.pack();
    frame.setVisible(true);
    return frame;
    
  }
  
 
    
}
