package com.leidos.bmech.model;

import java.util.List;
//import java.util.Map;

import com.leidos.bmech.analysis.Evidence;

import drae.j.VisualElement.VText;

public class TableEntry {
	@SuppressWarnings("unused")
	private VText vtext;
	
	List<Evidence> evidenceList;
	
	public TableEntry(VText v){
		vtext = v;
	}
}
