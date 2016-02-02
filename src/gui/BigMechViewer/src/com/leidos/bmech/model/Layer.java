package com.leidos.bmech.model;

import java.awt.Color;
import java.util.ArrayList;
//import java.util.HashMap;
import java.util.List;
import java.util.Map;

import drae.j.VisualElement.El;

public class Layer {
	private String name;
	private List<El> elements;
	private boolean highlight;
	private Color color;
	private List<Map<String, Object>> representation;

	public Layer(String name) {
		this.name = name;
		elements = new ArrayList<El>();
		representation = new ArrayList<Map<String, Object>>();
		setColor(Color.BLUE);
	}

	public void addElement(El el) {
		elements.add(el);
	}

	public String getName() {
		return name;
	}

	public List<El> getItems() {
		return elements;
	}

	public boolean isEmpty() {
		return elements.isEmpty();
	}

	public boolean add(El el) {
		return elements.add(el);
	}

	public boolean isHighlight() {
		return highlight;
	}

	public void setHighlight(boolean highlight) {
		this.highlight = highlight;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public boolean containsEl(El el) {
		return elements.contains(el);
	}

	public List<Map<String, Object>> getRep() {
		return representation;
	}

	public void setRep(List<Map<String, Object>> representation) {
		this.representation = representation;
	}


}
