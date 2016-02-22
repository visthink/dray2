/**
 * @@@ Thumbview is an attempt to separate out the thumbnails
 *     pane from the rest of the GUI, partly for modularity
 *     (it will be a view on the DataManager model), but also
 *     to simplify the other code.
 *     
 * TODO: 
 *     
 */
package com.leidos.bmech.gui;

import java.util.List;
//import java.util.stream;

import java.awt.image.BufferedImage;

import javax.swing.JList;

import com.leidos.bmech.model.DataManager;
import drae.j.VisualElement.*;

/**
 * @author fergusonrw
 *
 */
public class ThumbView extends JList<VPage> {

  final BufferedImage defaultThumb = makeDefaultThumb();
  
  BufferedImage[] thumbnails;
  
  private static final long serialVersionUID = 7828027497787812437L;
   
  public ThumbView(DataManager model) {
    super();
    thumbnails = defaultThumbArray(model.getSize());
  }
  
  // Create the initial default thumbnail.
  static private BufferedImage makeDefaultThumb() {
    return new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
  }
  
  // Return an array of default thumbnails.
  private BufferedImage[] defaultThumbArray(int size) {
    
    BufferedImage[] result = new BufferedImage[size];
    
    for (int i=0; i<size; i++) {
      result[i] = defaultThumb;
    };
    return result;
  }
  
  public void setThumbnail (BufferedImage image, int pageNum) {
    thumbnails[pageNum] = image;
  }
   
  // Need to finish once we figure out rendering.
  static public BufferedImage generateThumbnail(VPage vpage) {
    
    // Need to do this properly, but return default thumb for now.
    return makeDefaultThumb();
    
  }
}
