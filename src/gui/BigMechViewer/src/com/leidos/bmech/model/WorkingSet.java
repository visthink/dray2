package com.leidos.bmech.model;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.leidos.bmech.gui.UtiliBuddy;
import drae.j.BoundingBox;
import drae.j.VisualElement.El;
import drae.j.VisualElement.VText;

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
  private HashSet<String>  tags;
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
    tags = new HashSet<String>();
    name = n;

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

  public List<String> getTags() {
    return tags.stream().collect(Collectors.toList());
  }

  // SETTERS

  public void setName(String name) {
    this.name = name;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public void setTags(List<String> taglist) {
    this.tags = new HashSet<String>(taglist);
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
    if (this.isPageLevel()) {
      layerList.getBase().add(el);
    }

    
    // copy the layer information from the parent to the child
    Collection<Layer> layers = getParent().getLayerList().values();
    layers.stream()
          .filter(layer -> layer.containsEl(el))
          .forEach(layer -> layerList.addElementToLayer(layer.getName(), el));
  }

  // Add all of these items
  public void addItems(List<El> items) {
    for (El item : items) {
      this.addItem(item);
    }
  }

  public void addItems2(List<El> items) {
    items.stream().forEach(i -> this.addItem(i));
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
    getItems().stream()
              .filter(el -> rect.contains((BoundingBox) el.getBbox()))
              .forEach(el -> ws.addItem(el));
    return ws;
  }

  /**
   * takes a working set and adds it to the calling WorkingSet as a child. If
   * there already is a child that has the same dimensions, then it merges the
   * children together
   * 
   * @param added
   *          the WorkingSet to merge
   * @return the added WorkingSet
   */
  public WorkingSet mergeChild(WorkingSet addedWS) {
    
    boolean needToAdd = true;

    for (WorkingSet childWS : this.getChildren()) {
      
      // compare the new working set to each of the existing children
      if (childWS.equalsIgnoreChildren(addedWS)) {
        addedWS.getChildren().stream().forEach(ws -> childWS.mergeChild(ws));
        needToAdd = false; // No need to merge later.
        break; // we are merging
      }
    }
    if (needToAdd) {
      // the WorkingSet we wanted to add wasn't merged with an existing child, so
      // we need to add it here
      children.add(addedWS);
    }

    return addedWS;
  }

  /**
   * test whether two working sets are equal, based solely on size and page #
   *
   **/
  public boolean equalsIgnoreChildren(Object obj) {
    if (obj instanceof WorkingSet) {
      WorkingSet ws = (WorkingSet) obj;
      if (ws.bbox.equals(this.bbox) && (this.getPage() == ws.getPage())) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
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
    // return "<WS "+getName()+" " + getChildren().size() + " children"+" " +
    // getItems().size() + " items>";
    return getName();
  }

  // TAG METHODS

  public void addTag(String tag) {
    tags.add(tag.toLowerCase());
  }

  public void removeTag(String tag) {
    tags.remove(tag.toLowerCase());
  }

  public boolean hasTag(String tag) {
    return tags.contains(tag.toLowerCase());
  }

  // PREDICATES

  public boolean isPageLevel() {
    if (this.getParent() == null) {
      return false;
    } else if (this.getParent().getParent() == null) {
      return true;
    } else
      return false;
  }

  public boolean isDeletable() {
    // check if top level OR if page level
    if (this.getParent() == null || this.getParent().getParent() == null) {
      return false;
    } else {
      return true;
    }

  }

  // GETTERS
  
  public List<El> getEls () {
    return (List<El>) this.getItems();
  }
  
  public List<WorkingSet> getChildrenWithTag(String tag) {
    return getChildren()
             .stream()
             .filter(child -> child.hasTag(tag))
             .collect(Collectors.toList());
    }

  public List<WorkingSet> getChildrenWithTag(TypeTag tag) {
    return getChildrenWithTag(tag.name());
  }

  public static Set<TypeTag> getSuggestedTags(List<String> tagList) {

    Set<TypeTag> tags = new HashSet<TypeTag>();
    if (tagList == null || tagList.isEmpty()) {
      tags.add(TypeTag.TABLE);
      tags.add(TypeTag.FIGURE);
    }
    for (String tagStr : tagList) {
      TypeTag tag = TypeTag.valueOf(tagStr.trim().toUpperCase());
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

  private List<BoundingBox> getItemBBoxes() {
    return getEls().stream()
                   .map(el -> (BoundingBox)el.getBbox())
                   .collect(Collectors.toList());
  }

  /**
   * Used for column detection.
   * 
   * @param bbs
   * @return a list of rectangles such that
   */
  public List<Rectangle> groupHorizUniqueSections(List<Rectangle> bbs) {
    // List<Rectangle> completed = new ArrayList<Rectangle>();
    List<Rectangle> ret = new ArrayList<Rectangle>(bbs);
    int consecutiveRejections = 0;
    while (consecutiveRejections < ret.size()) {
      Rectangle test = ret.get(0);
      System.out.println("testing: " + this.getElsIn(test));

      ret.remove(0);
      Rectangle vertTest = new Rectangle(test);
      vertTest.grow(0, 1000);
      List<Rectangle> columnCandidates = new ArrayList<Rectangle>();
      for (Rectangle r : ret) {
        if (r.intersects(vertTest)) {
          columnCandidates.add(r);
        }
      }

      boolean mergedSomething = false;
      for (Rectangle closest : columnCandidates) {
        System.out.println("  -closest: " + this.getElsIn(closest));
        ret.remove(closest);
        Rectangle combined = new Rectangle(test);
        combined.add(closest);
        Rectangle combinedShrunk = new Rectangle(combined);
        combinedShrunk.grow(-1, -1);
        boolean rejected = false;
        ArrayList<Rectangle> others = new ArrayList<Rectangle>(ret);
        for (Rectangle other : others) {
          if (combinedShrunk.intersects(other)) {
            System.out.println("  -rejected - " + this.getElsIn(other));
            // no good, throw it out.
            // ret.add(test);
            // ret.add(closest);
            rejected = true;

            break;// for
          }
        }
        if (!rejected) {
          // add the combined rectangle to bbs
          // consecutiveRejections=0;
          ret.add(combined);
          mergedSomething = true;
          System.out.println("  -adding combined: " + this.getElsIn(combined));
          break;
        } else {
          // ret.add(test);
          ret.add(closest);
        }
      }
      if (!mergedSomething) {
        consecutiveRejections++;
        ret.add(test);
      } else {
        consecutiveRejections = 0;
      }
    }
    return ret;
  }

  public List<Rectangle> determineColumns(List<Rectangle> bbs) {
    List<Rectangle> ret = new ArrayList<Rectangle>();
    // the highest number of columns that share space in the y dimension
    int maxCollisions = 0;
    for (Rectangle section : bbs) {
      int collisions = 0;
      Rectangle horizTest = new Rectangle(section);
      horizTest.grow(1000, 0);
      for (Rectangle otherSection : bbs) {
        if (horizTest.intersects(otherSection)) {
          collisions++;
        }
      }
      maxCollisions = Math.max(collisions, maxCollisions);
    }

    for (Rectangle section : bbs) {
      int collisions = 0;
      Rectangle horizTest = new Rectangle(section);
      horizTest.grow(1000, 0);
      for (Rectangle otherSection : bbs) {
        if (horizTest.intersects(otherSection)) {
          collisions++;
        }
      }
      if (collisions > 2) {
        ret.add(section);
      }
    }
    return ret;
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
      System.out.println("column els: " + els);
      // identify headers based on font change
      for (El el : els) {
        System.out.println("  -" + el);
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
    List<Rectangle> bbs = getItemBBoxes().stream().map(b -> b.getBounds())
                                         .collect(Collectors.toList());
    bbs = groupHorizUniqueSections(bbs);
//    for (Rectangle r : bbs) {
//      List<El> x1 = this.getElsIn(r);
//      System.out.println("HUnique region: " + x1);
//    }
    List<Rectangle> columns = determineColumns(bbs);
//    for (Rectangle r : columns) {
//      List<El> x1 = this.getElsIn(r);
//      System.out.println("column: " + x1);
//    }
    Rectangle headerRow = determineHeaderRow(columns);

//    System.out.println("Final: ");
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
              // break out from sibling for loop and try again
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
      child.addItems(colItems.get(colX));
    }

    // create and add header_row now
    WorkingSet headerRowWS = this.createChild("AUTO_HEADER_ROW");
    headerRowWS.setName("HEADER_ROW" + (this.getChildrenWithTag("header_row").size() + 1));
    headerRowWS.addTag("header_row".toLowerCase());
    ret.add(headerRowWS);
    headerRowWS.addItems(headerRow);

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
      child.addItems(newRows.get(rowTop));
    }

    return ret;
  }

  public static double round(double value, int places) {
    if (places < 0)
      throw new IllegalArgumentException();

    BigDecimal bd = new BigDecimal(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);

    return bd.doubleValue();
  }

  public boolean isAutoTableReady() {
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

  /*
   * Returns true if the two lists hold an item in common.
   */
  public static boolean elementInCommon(List<?> list1, List<?> list2) {
    return list1.stream().anyMatch(item1 -> list2.contains(item1));
  }

  private static List<El> copyList(List<El> list1) {
    return list1.stream().collect(Collectors.toList());
  }

  // Readjust the elements in the WS when a rectangle is resized.
  public void resize(Rectangle rect) {

    List<El> elsInRect = getParent().getElsIn(rect);
    List<El> elsOutsideRect = copyList(getItems());
    elsOutsideRect.removeAll(elsInRect);
    List<El> newEls = copyList(elsInRect);
    newEls.removeAll(getItems());

    // Remove child working sets containing removed elements.
    List<WorkingSet> wsToRemove = getChildren().stream().filter(ws -> elementInCommon(ws.getItems(), elsOutsideRect))
        .collect(Collectors.toList());
    getChildren().removeAll(wsToRemove);

    // Add new elements from rectangle.
    this.addItems(newEls);

    // Remove elements outside rectangle.
    this.getItems().removeAll(elsOutsideRect);

    this.bbox = new Rectangle(rect);

  }

  /**
   * get a list of drae.j.VisualElement.El objects that are FULLY inside of the
   * rectangle
   * 
   * @param dragRectDescaled
   *          the rectangle
   * @return
   */
  // public List<El> getElsInOLD(Rectangle2D r) {
  // r.setRect((Math.min(r.getMinX(), r.getMaxX())),
  // (Math.min(r.getMinY(), r.getMaxY())),
  // Math.abs(r.getWidth()),
  // Math.abs(r.getHeight()));
  // List<El> ret = new ArrayList<El>();
  // for(El el : getItems()){
  // BoundingBox bb = (BoundingBox)el.getBbox();
  // if (r.contains(new Rectangle(bb.getBounds()))){
  // ret.add(el);
  // }
  // }
  // return ret;
  // }

  public List<El> getElsIn(Rectangle2D r) {
    List<El> elements = (List<El>) getItems();
    return elements.stream().filter(el -> r.contains((Rectangle) el.getBbox())).collect(Collectors.toList());
  }

  public Color getColor() {
    if (hasTag(TypeTag.COLUMN.name())) {
      return new Color(255, 69, 0);
    } else if (hasTag(TypeTag.ROW.name())) {
      return new Color(199, 21, 133);
    } else {
      return new Color(25, 25, 112);
    }
  }

  public double getArea() {
    return bbox.getWidth() * bbox.getHeight();
  }

  public static Comparator<WorkingSet> areaComparator = new Comparator<WorkingSet>() {

    public int compare(WorkingSet ws1, WorkingSet ws2) {
      return Double.compare(ws1.getArea(), ws2.getArea());
    }
  };

  public boolean equals(WorkingSet objWS) {
    return (this.getItems().equals(objWS.getItems()) && this.getTags().equals(objWS.getTags()));
  }

  public boolean containsChild(WorkingSet other) {
    return this.getChildren().contains(other);
  }

  public void addSeparator(Line2D line) {
    this.separators.add(line);
  }

  public List<Line2D> getSeparators() {
    return new ArrayList<Line2D>(separators);
  }
}
