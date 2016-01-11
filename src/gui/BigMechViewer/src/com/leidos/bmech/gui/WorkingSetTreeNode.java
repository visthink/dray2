package com.leidos.bmech.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import com.leidos.bmech.model.WorkingSet;

public class WorkingSetTreeNode extends DefaultMutableTreeNode {
	WorkingSet ws;
	public WorkingSetTreeNode(WorkingSet ws){
		super(ws.getName());
		this.ws = ws;
	}
	public WorkingSet getWorkingSet(){
		return ws;
	}
}
