/**
 * com.leidos.bmech.model.Layer Represents a single representation layer in the model.
 */
package com.leidos.bmech.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import dray.j.VisualElement.El;

public class Layer {
	
	// FIELDS
	
	private String   name;
	private List<El> items;
	private boolean  highlight;
	private Color    color;
	
	private List<Map<String, Object>> representation;

	// CONSTRUCTOR
	
	public Layer(String name) {
		this.name = name;
		items = new ArrayList<El>();
		representation = new ArrayList<Map<String, Object>>();
		setColor(Color.BLUE);
	}
	
	// SELECTORS
	
	public String    getName()  { return name; }

	public List<El>  getItems() { return items; }

	public Color     getColor() { return color; }

	public List<Map<String, Object>> 
	
	                 getRep()   { return representation; }

    // SETTERS

	public void    addElement(El el)  { items.add(el); }
     
	public boolean add(El el)         { return items.add(el); }

	public void    setColor(Color c)  { this.color = c;}

	public void    setHighlight(boolean highlight) 
	                                  
	                                  { this.highlight = highlight; }
	
	public void    setRep(List<Map<String, Object>> representation) 
	
	                                  { this.representation = representation; }
	
    // PREDICATES
    
    public boolean isEmpty()          { return items.isEmpty(); }

	public boolean isHighlight()      { return highlight; }

	public boolean containsEl(El el)  { return items.contains(el); }
	
}
