package com.leidos.bmech.gui;

import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import com.leidos.bmech.model.Layer;
import com.leidos.bmech.model.LayerList;


public class RepTreeNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 894926561L;

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

