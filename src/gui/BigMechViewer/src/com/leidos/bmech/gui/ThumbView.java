/**
 * @@@ Thumbview is an attempt to separate out the thumbnails
 *     pane from the rest of the GUI, partly for modularity
 *     (it will be a view on the DataManager model), but also
 *     to simplify the other code.
 *     
 * TODO: Cache the image renders in an array.
 *     
 */
package com.leidos.bmech.gui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.leidos.bmech.model.DataManager;

import drae.j.VisualElement.VPage;

/**
 * @author fergusonrw
 *
 */
@SuppressWarnings("serial")

public class  ThumbView  extends JList<VPage> 
                         implements Observer
{

  BufferedImage[]     thumbnails;    // Rendered thumbnails.
  
  PDFRenderer         renderer;      // Renderer for the document pages.
  
  DataManager         dataManager;   // The model
  
  float               thumbWidth;    // Current width for thumbs.
  
  float               thumbScale; 
  
 // ThumbView           pThis;       // Self-pointer. 
  
  // Constructor 
  
  public ThumbView(DataManager dm) {
    
    
    // Store DataManager as model.
    super(dm);        
    
    dataManager = dm;
    
    this.setModel(dm); 
    
    
    thumbWidth = 10.0f; // 2 inches.
    
    thumbScale = thumbWidth / 72.0f;   // 1.0 = 72 DPI.
    
    // Set up array to store thumbnails as needed.
    int numPages = dm.getSize(); 
    thumbnails = new BufferedImage[numPages];
    
    // Set up PDF page renderer, and render routine for JList cell.
    renderer = makePDFRenderer(dm);
    this.setCellRenderer(new ImageListRenderer());
    
    // Set up listening on DataManager.
    dm.addObserver(this);
    
    // Add pointer to self.
    //pThis = this;
    
    // Add mouse listener
    addMouseListener(
        
     new MouseAdapter() {
   
      public void mouseClicked(MouseEvent e) {
         handleMouseClicked(e);
      };
     }
     );
    
  }
  

  // Getters and Setters
  
  /**
   * Return the thumbnail for the given page number. If the page has been
   * previously rendered, returns that thumbnail. If not, renders it and returns it.
   * 
   * @param pageNum
   */
  public BufferedImage getThumbnail (int pageNum) {
    
    if (thumbnails[pageNum] == null) {
      thumbnails[pageNum] = this.renderThumb(pageNum);
    };
    return thumbnails[pageNum];
    
  }
  
  private BufferedImage renderThumb (int pageNum) {

    try {
    //  return renderer.renderImageWithDPI(pageNum, 30);
      return renderer.renderImage(pageNum,thumbScale);
        } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    
  }
  
  // Create the initial default thumbnail.
  public DataManager getDataManager () {
    return dataManager;
  }
  
  // thumbWidth
  public float getThumbWidth () { return thumbWidth; }
  
  public void setThumbWidth (float newWidth) {
    thumbWidth = newWidth;
    thumbScale = thumbWidth / 72.0f;   // 1.0 = 72 DPI.
    
    int numPages = dataManager.getSize(); // Clear rendered thumbs.
    thumbnails = new BufferedImage[numPages];
   
    revalidate();
    repaint();
    
    }

  private static PDFRenderer makePDFRenderer (DataManager dm) {
    PDDocument pddoc = null;
    try {
      pddoc = PDDocument.load(dm.getPdfFile());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new PDFRenderer(pddoc);    
  }
  
  public class ImageListRenderer extends DefaultListCellRenderer {
    
   @SuppressWarnings("rawtypes")
   @Override
  public Component getListCellRendererComponent (JList jlist, Object value, int index, 
                                                  boolean isSelected, boolean cellHasFocus) {
     
     if (jlist instanceof ThumbView) {
        ThumbView tv = (ThumbView) jlist;
        BufferedImage image = tv.getThumbnail(index);
        int pageNum = index + 1;
        this.setIcon(new ImageIcon(image));
        this.setText("Page " + pageNum);
        this.setHorizontalTextPosition(SwingConstants.CENTER);
        this.setVerticalTextPosition(SwingConstants.BOTTOM);
        if (isSelected) {
          setBackground(Color.BLUE);
        } else {
          setBackground(Color.WHITE);
        }
     }; 
     
     return this;   
   } 
  }
  
  /**
   * Pop this up in a single panel view for debugging.
   */
  public Frame popup () {
 
    // System.out.println("Generating the popup for " + this.toString());
    //List<VPage> pageList = this.getDataManager().getPages();
    //int numPages = pageList.size();
    //VPage[] pageArray = new VPage[numPages];
    //pageArray = pageList.toArray(pageArray);


    JScrollPane thumbScrollPane = new JScrollPane(this,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                                       ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    
    JPanel thumbnailPanel = new JPanel(new BorderLayout());
    thumbnailPanel.add(thumbScrollPane, BorderLayout.CENTER);
    
    JFrame frame = new JFrame();
    frame.add(thumbnailPanel);
    frame.setTitle("Test Thumbview");
    frame.pack();
    frame.setVisible(true);
    
    return frame;
    
  }

  /**
   * Update this view when the DataManager changes
   */
  @Override
  public void update(Observable ob, Object arg) {
    
//    System.out.println("Called the update function with "+ob.toString());
    DataManager dm = (DataManager) ob;
    int currentPageNum = dm.getCurrentPage();
    this.ensureIndexIsVisible(currentPageNum);
    this.setSelectedIndex(currentPageNum);
  }
  
  // EVENT HANDLERS
  
  public static void handleMouseClicked (MouseEvent e) {
    
    Point clickLoc = e.getPoint();
    int clickCount = e.getClickCount();
    ThumbView tv = (ThumbView) e.getComponent();
    
    if (clickCount == 1) {
      int index = tv.locationToIndex(clickLoc);
    //  System.out.println("Clicked on item " + index);
      System.out.println("The component for the mouse click is " + e.getComponent());
      tv.getDataManager().setCurrentPage(index);
    }
    
  }
  
}
