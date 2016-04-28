package com.leidos.bmech.gui;

import java.util.Map;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import com.leidos.bmech.model.Layer;
import com.leidos.bmech.model.LayerList;

import dray.j.Producer.Table;

public class RepTreeNode extends DefaultMutableTreeNode {
  
  private static final long serialVersionUID = 894926561L;

  String                    stringRepresentation;

  final static Object keywordID = Table.stringToKeyword("ID");
  final static Object keywordName = Table.stringToKeyword("name");
  final static Object keywordMap = Table.stringToKeyword("map");
  
  public RepTreeNode(Object uo) {
    
    super(uo);
    
    if (uo instanceof LayerList) {
      stringRepresentation = "LayerList";
    } else if (uo instanceof Layer) {
      stringRepresentation = ((Layer) uo).getName();
    } else if (uo instanceof Map) {
      @SuppressWarnings("rawtypes")
      Map map = (Map) uo;
      if (map.containsKey(keywordName)) {
        stringRepresentation = map.get(keywordName).toString();
      } else {
        // What's going on here? @@
        stringRepresentation = "map";
      }  
    } else if (uo instanceof List) {
 
      stringRepresentation = "list";
      
    } else {
      
      stringRepresentation = uo.toString();
    }
    
  }

  public RepTreeNode(Object uo, String rep) {
    super(uo);
    stringRepresentation = rep;
  }

  public String toString() {
    return stringRepresentation;
  }

}
