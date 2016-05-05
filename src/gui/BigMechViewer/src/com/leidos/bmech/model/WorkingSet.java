package com.leidos.bmech.model;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

//import com.leidos.bmech.gui.Task;
import com.leidos.bmech.gui.UtiliBuddy;
//import com.leidos.bmech.gui.Task.TaskType;

import dray.j.BoundingBox;
import dray.j.VisualElement.El;
import dray.j.VisualElement.VText;

/**
 * WorkingSet is a rectangular subsection of a pdf document
 * 
 * @author powelldan
 *
 */
public class WorkingSet {

  // FIELDS

  private WorkingSet       parent;
  private List<WorkingSet> children;
  private LayerList        layerList;
  private Rectangle        bbox;
  private String           name;
  private int              page;
  private String           filename;
  private List<String>     tags;
  private List<Line2D>     separators;

  // CONSTRUCTOR

  /**
   * Create a WorkingSet
   * 
   * @param p
   *          the parent of this working set
   * @param n
   *          the name of this working set
   */
  public WorkingSet(WorkingSet p, String n) {

    parent = p;
    children = new ArrayList<WorkingSet>();
    layerList = new LayerList(this);
    separators = new ArrayList<Line2D>();
    bbox = new Rectangle();
    tags = new ArrayList<String>();
    name = n;

    if (p != null) {
      page = parent.getPage();
      filename = parent.getFilename();
    } else {
      page = -1;
      filename = "";
    }
  }

  // ACCESSORS

  public List<WorkingSet> getChildren() {
    return children;
  }

  public WorkingSet getParent() {
    return parent;
  }

  public List<El> getItems() {
    return layerList.getBase().getItems();
  }

  public Rectangle getBbox() {
    return bbox;
  }

  public String getName() {
    return name;
  }

  public int getPage() {
    return page;
  }

  public LayerList getLayerList() {
    return layerList;
  }

  public String getFilename() {
    return filename;
  }

  public List<String> getTags() {
    return tags;
  }

  // SETTERS

  public void setName(String name) {
    this.name = name;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public void setTags(List<String> val) {
    this.tags = val;
  }
  
  public boolean isAutoTableReady() {
    return true;//this.getChildrenWithTag(TypeTag.COLUMN).size() > 1;
}
  
  /**
   * add an item to the working set and expand the rectangle to fit this item
   * 
   * @param el
   *          the item to add
   */
  public void addItem(El el) {
    // check if item's bbox should expand the WS bbox

    BoundingBox bb2 = (BoundingBox) el.getBbox();

    if (layerList.getBase().isEmpty()) { // if empty, WS bbox is items bbox
      bbox = new Rectangle(bb2.getBounds());
    } else if (!bbox.contains(bb2)) { // else, expand if necessary
      bbox.add(bb2);
    }
    // if this is a page, add everything to the base layer
    // since it wont be added in the for loop
    if (getParent() != null && getParent().getParent() == null)
      layerList.getBase().add(el);

    // copy the layer information from the parent to the child
    for (Layer layer : getParent().getLayerList().values()) {
      if (layer.containsEl(el)) {
        layerList.addElementToLayer(layer.getName(), el);
      }
    }
  }

  /**
   * create a working set as a child to this one
   * 
   * @return the created working set
   */
  public WorkingSet createChild() {
    return createChild(this.getName() + "." + this.getChildren().size());
  }

  /**
   * create a working set as a child to this one
   * 
   * @param name
   *          the name of the working set
   * @return the created working set
   */
  public WorkingSet createChild(String name) {
    WorkingSet ws = new WorkingSet(this, name);
    getChildren().add(ws);
    return ws;
  }

  /**
   * Creates a new working set and adds the items inside of rect from the parent
   * into the child.
   * 
   * @param name
   *          The name of the working set
   * @param rect
   *          The bounding box of items to add to the new WS
   * @return the working set that was just created
   */
  public WorkingSet createChild(String name, Rectangle2D rect) {
    WorkingSet ws = new WorkingSet(this, name);
    getChildren().add(ws);
    for (El el : getItems()) {
      if (rect.contains((BoundingBox) el.getBbox())) {
        ws.addItem(el);
      }
    }
    return ws;
  }

  /**
   * takes a working set and adds it to the calling workingset as a child. If
   * there already is a child that has the same dimensions, then it merges the
   * children together
   * 
   * @param added
   *          the WorkingSet to merge
   * @return the added workingset
   */
  public WorkingSet mergeChild(WorkingSet added) {
    boolean needToAdd = true;

    for (WorkingSet child : this.getChildren()) {
      // compare the new working set to each of the existing children
      if (child.equalsIgnoreChildren(added)) {
        // if the new working set matches one of the existing ones,
        // then merge their children
        for (WorkingSet addedChild : added.getChildren()) {
          child.mergeChild(addedChild);
        }
        // set this flag since we merged and dont need to add.
        needToAdd = false;
        break; // we are merging
      }
    }
    if (needToAdd) {
      // the workingset we wanted to add wasnt merged with an existing child, so
      // we need to add it here
      children.add(added);
    }

    return added;
  }

  /**
   * test whether two working sets are equal, based solely on size and pdf
   * origin
   * 
   * @param obj
   *          the working set to compare to
   * @return
   */
  public boolean equalsIgnoreChildren(Object obj) {
    if (obj instanceof WorkingSet) {
      WorkingSet ws = (WorkingSet) obj;
      if (ws.bbox.equals(this.bbox) && this.getFilename().equals(ws.getFilename())) {
        return true;
      } else
        return false;
    } else
      return false;
  }

  /**
   * test to see if the param WorkingSet is among the ancestors of this
   * workingset
   * 
   * @param potentialAncestor
   * @return true if ancestor
   */
  public boolean hasAncestor(WorkingSet potentialAncestor) {
    WorkingSet ancestor = getParent();
    while (ancestor != null) {
      if (ancestor == potentialAncestor) {
        return true; // we found it!
      }
      ancestor = ancestor.getParent();
    }
    return false; // got to the top without finding it
  }

  public String toString() {
    return getName();
  }

  /*
   * addTag - Add a single tag to this working set. 
   *          Takes the first token of the string and lowercases it to
   *          create canonical tag.
   * 
   * @param   tag 
   *          The token as a String.
   */
  public void addTag(String tag) {
	StringTokenizer tokenizer = new StringTokenizer(tag);
	String cleanTag = tokenizer.nextToken().toLowerCase();
    if (!tags.contains(cleanTag)) {
      tags.add(cleanTag);
    }
  }

  public void removeTag(String tag) {
    tags.remove(tag.toLowerCase());
  }

  public boolean hasTag(String tag) {
    return tags.contains(tag.toLowerCase());
  }

  public boolean isPageLevel() {
    if (this.getParent() == null) {
      return false;
    } else if (this.getParent().getParent() == null) {
      return true;
    } else
      return false;
  }
  
  public boolean isLeaf () {
	  return this.getChildren().isEmpty();
  }

  public boolean isDeletable() {
    // check if top level OR if page level
    if (this.getParent() == null || this.getParent().getParent() == null) {
      return false;
    } else
      return true;
  }

  public List<WorkingSet> getChildrenWithTag(String tag) {
    List<WorkingSet> ret = new ArrayList<WorkingSet>();
    for (WorkingSet child : getChildren()) {
      if (child.hasTag(tag)) {
        ret.add(child);
      }
    }
    return ret;
  }

  public List<WorkingSet> getChildrenWithTag(TypeTag tag) {
    return getChildrenWithTag(tag.name());
  }

  public static String cleanTag (String inputTag) {
	  StringTokenizer tokenizer = new StringTokenizer(inputTag);
	  return tokenizer.nextToken();
  }
  
  public static Set<TypeTag> getSuggestedTags(List<String> tagList) {
    Set<TypeTag> tags = new HashSet<TypeTag>();
    if (tagList == null || tagList.isEmpty()) {
      tags.add(TypeTag.TABLE);
      tags.add(TypeTag.FIGURE);
    }
    for (String tagStr : tagList) {
      TypeTag tag = TypeTag.valueOf(cleanTag(tagStr).toUpperCase());
      switch (tag) {
      case TABLE:
        tags.add(TypeTag.COLUMN);
        tags.add(TypeTag.ROW);
        tags.add(TypeTag.HEADER_ROW);
        tags.add(TypeTag.COLUMN);
        tags.add(TypeTag.CAPTION);
        tags.add(TypeTag.IGNORE);
        tags.add(TypeTag.MERGE);
        break;
      case FIGURE:
        tags.add(TypeTag.CAPTION);
        break;
      case COLUMN:
      case ROW:
        tags.add(TypeTag.HEADER);
        tags.add(TypeTag.MERGE);
        break;
      default:
        break;
      }
    }
    return tags;
  }

  private List<Rectangle> getItemBBoxes() {
    List<Rectangle> ret = new ArrayList<Rectangle>();
    // List<El> els = new ArrayList<El>(this.getItems());
    for (El el : this.getItems()) {
      ret.add(0, ((BoundingBox) el.getBbox()).getBounds());
    }
    return ret;
  }

  /**
   * Used for column detection.
   * 
   * @param inputRectangleList - a list of rectangle objects.
   * @return A list of rectangles organized into potential columns.
   */
  public List<Rectangle> groupHorizUniqueSections(List<Rectangle> inputRectangleList) {
    
    // Copy rectangle list to create initial set of rectangles.
    List<Rectangle> rectangleList = new ArrayList<Rectangle>(inputRectangleList);
    int consecutiveRejections = 0;
    
    while (consecutiveRejections < rectangleList.size()) {
      
      Rectangle topRectangle = rectangleList.remove(0);  // Pop rectangle off list.
     
      //Rectangle testColumn = new Rectangle(topRectangle);  // Create test column from rectangle.
      //testColumn.grow(0, 1000);
      Rectangle testColumn = expandedRectangle(topRectangle, 0, 1000); // Expand along y axis.
      
      // Collect all rectangles that intersect the candidate column.

      List<Rectangle> columnCandidates = 
          rectangleList.stream()
                       .filter(candidate -> candidate.intersects(testColumn))
                       .collect(Collectors.toList());
                                              

      boolean mergedSomething = false;

      // Attempt to combine each of the candidate item bboxes
      // with the column candidate, and add to the list
      // of rectangles if you succeed. 
      
      for (Rectangle candidate : columnCandidates) {
        
        rectangleList.remove(candidate);
        
        // Create bounding box of current top rectangle and candidate.
//        Rectangle combinedRectangle = new Rectangle(topRectangle);
//        combinedRectangle.add(candidate);
        Rectangle combinedRectangle = boundingRectangle(topRectangle, candidate);
        
        //Rectangle combinedRectangleInner = new Rectangle(combinedRectangle);
        //combinedRectangleInner.grow(-1, -1);
        Rectangle combinedRectangleInner = expandedRectangle(combinedRectangle, -1, -1);
            
        // Reject if column intersects any other item bounding box.
        boolean rejected = rectangleList.stream()
                                        .anyMatch(otherRectangle -> combinedRectangleInner.intersects(otherRectangle));
        
        if (rejected) {
          
          // Rejected - add candidate to end of rectangle list.
          rectangleList.add(candidate);
          
        } else {
  
          // Add new combined rectangle to end of rectangle list.
          rectangleList.add(combinedRectangle);
          mergedSomething = true;
  
        }
        
      } // endFor(candidate).
      
      // If we merged something, zero out rejection counter.
      // If we didn't, then up rejections and add first rectangle
      //  to the end of the rectangle list.
      if (mergedSomething) {
        consecutiveRejections = 0;
      } else {
        consecutiveRejections++;
        rectangleList.add(topRectangle);       
      }
    }
    
    return rectangleList;
  }

  /**
   * 
   * @param inputRectangle - The original rectangle
   * @param x - Amount to grow x
   * @param y - Amount to grow y
   * @return New rectangle with the expanded dimensions.
   */
  public static Rectangle expandedRectangle (Rectangle inputRectangle, int x, int y) {
    Rectangle result = new Rectangle(inputRectangle);
    result.grow(x, y);
    return result;   
  }
  
  private static Rectangle boundingRectangle (Rectangle rectangle1, Rectangle rectangle2) {
    Rectangle result = new Rectangle(rectangle1);
    result.add(rectangle2);
    return result;
  }
    
  
  /**
   * determineColumns - Find likely columns from a set of item bounding boxes.
   * 
   * @param bboxList - A list of Rectangles
   * @return A list of columns from that bbox list.
   */
  public List<Rectangle> determineColumns(List<Rectangle> bboxList) {
    
    List<Rectangle> columnList = new ArrayList<Rectangle>();
    // the highest number of columns that share space in the y dimension
    int maxCollisions = 0;
    
    for (Rectangle bbox : bboxList) {
      
      Rectangle candidateRow = expandedRectangle(bbox, 1000, 0); // Expand along x axis.
      
      // Count the number of intersections with other rectangles.
      int intersectionCount = 
          (int) bboxList.stream()
                        .filter(otherBbox -> candidateRow.intersects(otherBbox))
                        .count();
      
      maxCollisions = Math.max(intersectionCount, maxCollisions);
    }
    

    for (Rectangle bbox : bboxList) {
      
      Rectangle horizTest = expandedRectangle(bbox, 1000, 0); // Expand along x axis.
      long collisions = bboxList.stream() 
                                .filter(otherBbox -> horizTest.intersects(otherBbox))
                                .count();
      if (collisions > 2) {
        columnList.add(bbox);
      }
    }
    
    return columnList;
  }

  @SuppressWarnings("rawtypes")
  
  public Rectangle determineHeaderRow(List<Rectangle> columns) {

    ArrayList<Double> headerLines = new ArrayList<Double>();

    for (Rectangle colBB : columns) {
      
      List<El> els = this.getElsIn(colBB);
      Map lastStyle = new HashMap();
      int numFontChange = 0;
      
      for (El el : els) {

        // get number of and location of font change

        if (el instanceof VText) {
          VText txt = (VText) el;
          if (!((Map) txt.style).equals(lastStyle)) {
            lastStyle = (Map) txt.style;
            numFontChange++;
            if (numFontChange == 2) {
              headerLines.add(((BoundingBox) el.getBbox()).getMinY());
            }
          }

        }
      }
    }
    
    Double[] a = new Double[0];
    // determine the line using the most common first font-change
    double trueHeaderLine = UtiliBuddy.mode(headerLines.toArray(a));
    // second pass -- go through again and pull out everything above the line
    Rectangle headerRow = null;
    boolean first = true;
    for (Rectangle colBB : columns) {
      List<El> els = this.getElsIn(colBB);
      // System.out.println("column els: " + els);
      // identify headers based on font change
      for (El el : els) {
        //System.out.println("  -" + el);
        BoundingBox bb = (BoundingBox) el.getBbox();
        if (bb.getMinY() < trueHeaderLine - 1) {
          if (first) {
            headerRow = new Rectangle(bb.getBounds());
            first = false;
          } else {
            headerRow.add(bb);
          }
        }
      }
    }
    return headerRow;
  }

  public List<WorkingSet> AutoCols() {
    
    List<WorkingSet> ret = new ArrayList<WorkingSet>();
    List<Rectangle> bbs = getItemBBoxes();
    bbs = groupHorizUniqueSections(bbs);
    for (Rectangle r : bbs) {
      List<El> x1 = this.getElsIn(r);
      System.out.println("HUnique region: " + x1);
    }
    List<Rectangle> columns = determineColumns(bbs);
    for (Rectangle r : columns) {
      List<El> x1 = this.getElsIn(r);
      System.out.println("column: " + x1);
    }
    Rectangle headerRow = determineHeaderRow(columns);

    System.out.println("Final: ");
    
    for (Rectangle r : columns) {

      System.out.println("  -" + this.getElsIn(r));
      // create and add columns now
      WorkingSet child = this.createChild("AUTO_COL", r);
      child.setName("COLUMN" + (this.getChildrenWithTag("column").size() + 1));
      child.addTag("column".toLowerCase());
      ret.add(child);
    }
    
    if (headerRow != null) {
      List<El> els = this.getElsIn(headerRow);
      System.out.println("header row els: " + els);
      // create and add header_row now
      WorkingSet headerRowWS = this.createChild("AUTO_HEADER_ROW", headerRow);
      headerRowWS.setName("HEADER_ROW" + (this.getChildrenWithTag("header_row").size() + 1));
      headerRowWS.addTag("header_row".toLowerCase());
      ret.add(headerRowWS);
    }
    this.normalize();

    return ret;
  }

  public static Rectangle getClosest(Rectangle rect, List<Rectangle> others) {
    if (others.size() == 0)
      return null;
    Rectangle closest = others.get(0);
    double bestDistance = 9999999;
    for (Rectangle other : others) {
      double dist = bestDistance;
      // bottom left to top left
      dist = Math.abs(rect.getMinY() - other.getMinY());
      if (dist < bestDistance) {
        closest = other;
        bestDistance = dist;
      }
    }
    return closest;
  }

  public void normalize() {

    List<WorkingSet> headerRows = new ArrayList<WorkingSet>(this.getChildrenWithTag(TypeTag.HEADER_ROW));
    for (WorkingSet headerRow : headerRows) {
      for (El el : headerRow.getItems()) {
        WorkingSet headerWS = this.createChild();
        headerWS.setName("HEADER" + (this.getChildrenWithTag("header").size() + 1));
        headerWS.addTag("header".toLowerCase());
        headerWS.addItem(el);
      }
    }

    boolean shuffled = true;
    while (shuffled) {
      shuffled = false;
      List<WorkingSet> sortedByArea = new ArrayList<WorkingSet>(getChildrenWithTag("column"));
      Collections.sort(sortedByArea, WorkingSet.areaComparator);
      for (WorkingSet child : sortedByArea) {
        // 1st, normalize the child recursively
        child.normalize();
        System.out.println("normalizing " + this);
        // 2nd, if any sibling working sets are entirely within this child,
        // move it into this one

        for (WorkingSet sibling : getChildren()) {

          if (sibling != child) {
            System.out.println("comparing " + child + " w " + sibling);
            if (child.getBbox().contains(sibling.getBbox())) {
              System.out.println(child + " contains " + sibling);
              System.out.println(this.getChildren());
              this.getChildren().remove(sibling);
              System.out.println(this.getChildren());
              // dont push the child down if it's already there
              // already there means same items and same tags
              if (!child.containsChild(child)) {
                // System.out.println("moving "+sibling +" into "+child);
                child.getChildren().add(sibling);
                System.out.println(this.getChildren());
              } else
                System.out.println(child + " is already there");
              shuffled = true;
              // break out from the sibling for loop and
              // try again
              break;
            }

          }
        }
        if (shuffled)
          break;
      }
    }
  }

  @SuppressWarnings("rawtypes")
  public List<WorkingSet> AutoCols2() {
    List<WorkingSet> ret = new ArrayList<WorkingSet>();
    List<El> candidates = new ArrayList<El>(this.getItems());
    // map from x coord to list of elements near that x coord
    Map<Double, ArrayList<El>> colItems = new HashMap<Double, ArrayList<El>>();
    for (El candidate : candidates) {
      BoundingBox bbox = (BoundingBox) candidate.getBbox();
      // round to the nearest 10
      double top = round(bbox.getMinX() / 10, 0) * 10;

      /// create a new 'row' if there isnt one
      if (!colItems.containsKey(top)) {
        colItems.put(top, new ArrayList<El>());
      }
      colItems.get(top).add(candidate);
    }
    ArrayList<Double> headerLines = new ArrayList<Double>();
    ArrayList<Double> tooFewItems = new ArrayList<Double>();
    for (Double colX : colItems.keySet()) {
      System.out.println("col at " + colX);
      ArrayList<El> col = colItems.get(colX);
      // remove anything where there's only one item
      if (col.size() <= 1) {
        System.out.println("dropping 1 element column");
        tooFewItems.add(colX);
      } else {

        // remove too-longs
        // find the left edge of the column to the right of this one
        Double xOfNextCol = 99999.0;
        for (Double x : colItems.keySet()) {
          if (x > colX) {
            xOfNextCol = Math.min(xOfNextCol, x);
          }
        }
        System.out.println("next column: " + xOfNextCol);
        // remove any els from this column who extend past that
        ArrayList<El> toRemove = new ArrayList<El>();
        for (El el : col) {
          if (((BoundingBox) el.getBbox()).getMaxX() > xOfNextCol) {
            System.out.println("removing " + el + ": " + ((BoundingBox) el.getBbox()).getMaxX());
            toRemove.add(el);
          }
        }
        col.removeAll(toRemove);
        toRemove.clear();

        // label headers
        System.out.println("finding headers...");
        Map lastStyle = new HashMap();
        int numFontChange = 0;

        for (El el : col) {
          System.out.println(el);
          // get number of and location of font change

          if (el instanceof VText) {
            VText txt = (VText) el;
            System.out.println(((Map) txt.style).entrySet());
            if (!((Map) txt.style).equals(lastStyle)) {
              System.out.println("Style changed");
              lastStyle = (Map) txt.style;
              numFontChange++;
              if (numFontChange == 2) {
                headerLines.add(((BoundingBox) el.getBbox()).getMinY());
              }
            }

          }

        }
      }

    }
    for (Double colX : tooFewItems) {
      colItems.remove(colX);
    }
    // done with each column, ready to determine header row
    Double[] a = new Double[0];
    double trueHeaderLine = UtiliBuddy.mode(headerLines.toArray(a));

    // second pass
    ArrayList<El> headerRow = new ArrayList<El>();
    for (Double colX : colItems.keySet()) {
      ArrayList<El> data = new ArrayList<El>();
      ArrayList<El> col = colItems.get(colX);
      // identify headers based on font change
      for (El el : col) {
        BoundingBox bb = (BoundingBox) el.getBbox();
        if (bb.getMinY() < trueHeaderLine - 1) {
          headerRow.add(el);
        } else {
          data.add(el);
        }
      }

      // remove statistical anomalies
      double stddevThreshold = 3.0;
      double[] widths = new double[data.size()];
      for (int i = 0; i < widths.length; ++i) {
        widths[i] += ((BoundingBox) data.get(i).getBbox()).width;
      }
      double widthSD = UtiliBuddy.stddev(widths);
      double widthMean = UtiliBuddy.mean(widths);
      System.out.println("SD|mean: " + widthSD + " " + widthMean);
      ArrayList<El> toRemove = new ArrayList<El>();
      for (El el : data) {

        if (Math.abs(((BoundingBox) el.getBbox()).width - widthMean) > stddevThreshold * widthSD) {
          toRemove.add(el);
        }
      }
      colItems.get(colX).removeAll(toRemove);

      // create and add columns now
      WorkingSet child = this.createChild("AUTO_COL");
      child.setName("COLUMN" + (this.getChildrenWithTag("column").size() + 1));
      child.addTag("column".toLowerCase());
      ret.add(child);
      for (El el : colItems.get(colX)) {
        child.addItem(el);
      }
    }

    // create and add header_row now
    WorkingSet headerRowWS = this.createChild("AUTO_HEADER_ROW");
    headerRowWS.setName("HEADER_ROW" + (this.getChildrenWithTag("header_row").size() + 1));
    headerRowWS.addTag("header_row".toLowerCase());
    ret.add(headerRowWS);
    for (El el : headerRow) {
      headerRowWS.addItem(el);
    }

    return ret;
  }

  public List<WorkingSet> AutoRows() {
    List<WorkingSet> ret = new ArrayList<WorkingSet>();
    // List<WorkingSet> rows = this.getChildrenWithTag(TypeTag.ROW);
    List<WorkingSet> cols = this.getChildrenWithTag(TypeTag.COLUMN);
    List<WorkingSet> headerRows = this.getChildrenWithTag(TypeTag.HEADER_ROW);
    Set<El> headerEls = new HashSet<El>();
    for (WorkingSet headerRow : headerRows) {
      headerEls.addAll(headerRow.getItems());
    }
    Map<Double, ArrayList<El>> newRows = new HashMap<Double, ArrayList<El>>();
    for (WorkingSet column : cols) {
      for (El item : column.getItems()) {
        // ignore header items for row generation...
        if (!headerEls.contains(item)) {
          BoundingBox bbox = (BoundingBox) item.getBbox();
          double top = round(bbox.getMinY() / 10, 0) * 10;
          /// create a new 'row' if there isnt one
          if (!newRows.containsKey(top)) {
            newRows.put(top, new ArrayList<El>());
          }
          newRows.get(top).add(item);
        }
      }
    }

    for (Double rowTop : newRows.keySet()) {
      WorkingSet child = this.createChild("AUTO_ROW");
      ret.add(child);
      for (El el : newRows.get(rowTop)) {
        child.addItem(el);
      }
    }

    return ret;
  }

 /*
  * doAutoTableWS - Attempt to auto-detect the rows and columns in 
  *                 a Table working set.
  *                 
  * @parm currentWS - The table working set to examine.
  * 
  * @returns A list of new column and row working sets.
  */
 public List<WorkingSet> doAutoTableWS () {
	  
	 WorkingSet tableWS = this;
	 
	 List<WorkingSet> autoCols = tableWS.AutoCols();
     
     List<WorkingSet> autoRows = tableWS.AutoRows();
     
     List<WorkingSet> newWorkingSets = new ArrayList<WorkingSet>();
     newWorkingSets.addAll(autoCols);
     newWorkingSets.addAll(autoRows);
     
     return newWorkingSets;
     
//	 for (WorkingSet colWS : autoCols) {
//	      //appendToLog("Creating new working set with tag: " + "column");
//	      insertWS(colWS);
//	      //taskHistory.add(new Task(TaskType.ADD_WS, colWS));
//	    }

//	    List<WorkingSet> autoRows = currentWS.AutoRows();
//	    for (WorkingSet rowWS : autoRows) {
//	      //appendToLog("Creating new working set with tag: " + "row");
//	      rowWS.setName("ROW" + (currentWS.getChildrenWithTag("row").size() + 1));
//	      rowWS.addTag("row".toLowerCase());
//	      insertWS(rowWS);
//	      //taskHistory.add(new Task(TaskType.ADD_WS, rowWS));
//	    }
	    
 }

  public static double round(double value, int places) {
    if (places < 0)
      throw new IllegalArgumentException();

    BigDecimal bd = new BigDecimal(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);

    return bd.doubleValue();
  }

  public boolean isReady() {
    return true;// this.getChildrenWithTag(TypeTag.COLUMN).size() > 1;
  }

  public Rectangle getBboxSmall() {
    Rectangle ret = new Rectangle(bbox);
    ret.grow(-1, -1);
    return ret;
  }

  public Rectangle getBboxWide() {
    Rectangle ret = new Rectangle(bbox);
    ret.grow(1, 1);
    return ret;
  }

  public int isHorizontalEdge(int x) {
    // TODO Auto-generated method stub
    if (Math.abs(x - bbox.getMinX()) <= 1) {
      return -1;
    } else if (Math.abs(x - bbox.getMaxX()) <= 1) {
      return 1;
    } else
      return 0;
  }

  public int isVerticalEdge(int y) {
    // TODO Auto-generated method stub
    if (Math.abs(y - bbox.getMinY()) <= 1) {
      return -1;
    } else if (Math.abs(y - bbox.getMaxY()) <= 1) {
      return 1;
    } else
      return 0;

  }

  public void resize(Rectangle rect) {
    List<El> newEls = new ArrayList<El>(getParent().getElsIn(rect));
    List<El> lostEls = new ArrayList<El>(getItems());
    lostEls.removeAll(newEls);
    newEls.removeAll(getItems());
    // first check to see if we lost any items
    Set<WorkingSet> wsToRemove = new HashSet<WorkingSet>();
    if (lostEls.size() > 0) {
      for (WorkingSet child : getChildren()) {
        for (El lostEl : lostEls) {
          if (child.getItems().contains(lostEl)) {
            wsToRemove.add(child);
          }
        }
      }
    }
    this.getChildren().removeAll(wsToRemove);

    for (El el : newEls) {
      this.addItem(el);
    }
    for (El el : lostEls) {
      this.getItems().remove(el);
    }

    this.bbox = new Rectangle(rect);

  }

  /**
   * get a list of dray.j.VisualElement.El objects that are FULLY inside of the
   * rectangle
   * 
   * @param dragRectDescaled
   *          the rectangle
   * @return
   */
  public List<El> getElsIn(Rectangle2D dragRectDescaled) {
    dragRectDescaled.setRect((Math.min(dragRectDescaled.getMinX(), dragRectDescaled.getMaxX())),
        (Math.min(dragRectDescaled.getMinY(), dragRectDescaled.getMaxY())), Math.abs(dragRectDescaled.getWidth()),
        Math.abs(dragRectDescaled.getHeight()));
    List<El> ret = new ArrayList<El>();
    for (El el : getItems()) {
      BoundingBox bb = (BoundingBox) el.getBbox();
      if (dragRectDescaled.contains(new Rectangle(bb.getBounds()))) {
        ret.add(el);
      }
    }
    return ret;
  }

  public Color getColor() {
    if (hasTag(TypeTag.COLUMN.name())) {
      return new Color(255, 69, 0);
    } else if (hasTag(TypeTag.ROW.name())) {
      return new Color(199, 21, 133);
    } else
      return new Color(25, 25, 112);

  }

  public static Comparator<WorkingSet> areaComparator = new Comparator<WorkingSet>() {

    public int compare(WorkingSet ws1, WorkingSet ws2) {
      // ascending order
      double thisArea = ws1.getBbox().getWidth() * ws1.getBbox().getHeight();
      double otherArea = ws2.getBbox().getWidth() * ws2.getBbox().getHeight();
      return Double.compare(thisArea, otherArea);

      // descending order
      // return StudentName2.compareTo(StudentName1);
    }
  };

  public boolean equals(Object obj) {
    if (obj instanceof WorkingSet) {
      WorkingSet objWS = (WorkingSet) obj;
      // compare items
      return this.getItems().equals(objWS.getItems()) && this.getTags().equals(objWS.getTags());

    }
    return false;
  }

  public boolean containsChild(WorkingSet other) {
    for (WorkingSet child : this.getChildren()) {
      if (child.equals(other)) {
        return true;
      }
    }
    return false;
  }

  public void addSeparator(Line2D line) {
    // TODO Auto-generated method stub
    this.separators.add(line);
  }

  public List<Line2D> getSeparators() {
    return new ArrayList<Line2D>(separators);
  }
}
