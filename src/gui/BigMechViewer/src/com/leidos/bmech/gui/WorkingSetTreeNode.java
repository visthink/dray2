package com.leidos.bmech.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import com.leidos.bmech.model.WorkingSet;

public class WorkingSetTreeNode extends DefaultMutableTreeNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 935999429559366756L;
	WorkingSet ws;
	public WorkingSetTreeNode(WorkingSet ws){
		super(ws.getName());
		this.ws = ws;
	}
	public WorkingSet getWorkingSet(){
		return ws;
	}
}
