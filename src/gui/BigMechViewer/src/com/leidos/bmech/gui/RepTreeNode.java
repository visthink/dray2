package com.leidos.bmech.gui;

//import java.util.Enumeration;
//import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
//import javax.swing.tree.MutableTreeNode;
//import javax.swing.tree.TreeNode;

import com.leidos.bmech.model.Layer;
import com.leidos.bmech.model.LayerList;

//import drae.j.VisualElement.El;
//import drae.j.VisualElement.VText;


public class RepTreeNode extends DefaultMutableTreeNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8949544105051626561L;
	String stringRepresentation;
	public RepTreeNode(Object uo){
		super(uo);
		if(uo instanceof LayerList){
			stringRepresentation = "LayerList";
		} else if(uo instanceof Layer){
			stringRepresentation = ((Layer)uo).getName();	
		} else if (uo instanceof Map){
			@SuppressWarnings("rawtypes")
			Map map = (Map) uo;
			if(map.containsKey("name")){
				stringRepresentation =  map.get("name").toString();
			} else stringRepresentation = "map";
		} else stringRepresentation = uo.toString();
		
	}
	public RepTreeNode(Object uo, String rep){
		super(uo);
		stringRepresentation = rep;
	}
	
	public String toString(){
		return stringRepresentation;
	}
	
}

