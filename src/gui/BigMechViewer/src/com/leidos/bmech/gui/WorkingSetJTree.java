package com.leidos.bmech.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.leidos.bmech.model.WorkingSet;


public class WorkingSetJTree extends JTree {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3901405746865572882L;
	WorkingSetJTree pthis;
	ViewerApp app;
	public boolean wsChangedAlready = false;
	public WorkingSetJTree(ViewerApp app){
		super(new DefaultMutableTreeNode("No Document Loaded"));
		this.app = app;
		pthis = this;
		addMouseListener (new MouseAdapter () {
			public void mouseClicked(MouseEvent e){
		    	int selRow = getRowForLocation(e.getX(), e.getY());
		    	TreePath selPath = getPathForLocation(e.getX(), e.getY());
		    	if(selRow != -1) {
					if(SwingUtilities.isRightMouseButton(e)){
						DocumentMenu menu;
						menu = new DocumentMenu(pthis.app, getWSFromPath(selPath), null);
						menu.show(e.getComponent(), e.getX(), e.getY());
					}
		    	}
			}
		});
	}
	
	public void reload() {
		//DefaultTreeModel model = new DefaultTreeModel();
			addWSRecursive(app.getDataManager().getHeadWorkingSet(), null);
			this.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
	            @Override
	            public void valueChanged(TreeSelectionEvent e) {
	            	DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) pthis.getLastSelectedPathComponent();
	            	//wsChangedAlready should be false if this valueChanged signal came
	            	//from user interaction with the workingSetTree itself
	            	if(selectedNode != null && !wsChangedAlready){
	                	//appendToLog(selectedNode.toString());
	            		//app.getDataManager().getView().setCurrentWS((WorkingSet)selectedNode.getUserObject());
	            		app.getDataManager().setCurrentWS((WorkingSet)selectedNode.getUserObject());
                        app.viewWSUpdated();
	                	
	                }
	            	
	            }
	        });
			this.setEditable(true);
			this.getModel().addTreeModelListener(new MyTreeModelListener());
			//DefaultMutableTreeNode wsHead = (DefaultMutableTreeNode)workingSetTree.getModel().getRoot();
			this.setRootVisible(false);
	}
	
	public void refresh() {		
		DefaultMutableTreeNode theNode = getNodeOfWs(app.getDataManager().getCurrentWS());
		this.setSelectionPath(new TreePath(theNode.getPath()));
		//this.expandPath(new TreePath(((DefaultMutableTreeNode)theNode.getParent()).getPath()));
		this.expandPath(new TreePath(theNode.getPath()));
	}
	
	@SuppressWarnings("serial")
	private void addWSRecursive(WorkingSet ws, DefaultMutableTreeNode node){
		DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(ws);
		if(node!=null){
			node.add(subNode);
		} else {//otherwise this is the head node
			this.setModel(new DefaultTreeModel(subNode){
				public void valueForPathChanged(TreePath path, Object newValue){
					Object obj = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
					((WorkingSet)obj).setName(newValue.toString());
					super.valueForPathChanged(path, obj);
					
				}
			});
		}
		if (!ws.getChildren().isEmpty()){
			for(WorkingSet subWS : ws.getChildren()){
				addWSRecursive(subWS, subNode);
			}
		}
	}
	
	public void insertWorkingSetNode(WorkingSet parent, WorkingSet ws){
		DefaultMutableTreeNode theNode = getNodeOfWs(parent);
		//DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) workingSetTree.getLastSelectedPathComponent();
		DefaultMutableTreeNode newWs = new DefaultMutableTreeNode(ws);
		theNode.add(newWs);
		
		DefaultTreeModel model = (DefaultTreeModel) (this.getModel());
		model.reload();
		//refresh();

	}
	
	public void deleteWS(WorkingSet victim){
		DefaultTreeModel model = (DefaultTreeModel) getModel();
		if(victim == null) return;
		DefaultMutableTreeNode victimNode = getNodeOfWs(victim);
		if (victim.getParent() != null) {
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) victimNode.getParent();
			//cant delete page Working Sets (aka 1st level below root)
			if(!parentNode.isRoot()){
				model.removeNodeFromParent(victimNode);
			}
		}
	}
	
	private WorkingSet getWSFromPath(TreePath path){
		for (@SuppressWarnings("rawtypes")
		Enumeration e = ((DefaultMutableTreeNode)this.getModel().getRoot()).depthFirstEnumeration(); e.hasMoreElements();) {
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
		    if(new TreePath(node.getPath()).equals(path)){
		    	return (WorkingSet)node.getUserObject();
		    }
		}
		return null;
	}
	
	private DefaultMutableTreeNode getNodeOfWs(WorkingSet ws){
		//DefaultMutableTreeNode theNode = null;
		for (@SuppressWarnings("rawtypes")
		Enumeration e = ((DefaultMutableTreeNode)this.getModel().getRoot()).depthFirstEnumeration(); e.hasMoreElements();) {
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
		    if (ws == node.getUserObject()) {
		        return node;
		    }
		}
		return null;
	}
	public void expandCurrentWsNode(){
		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) this.getLastSelectedPathComponent();
		this.expandPath(new TreePath(selectedNode.getPath()));
	}
	
	/**
	 * catches the rename events from editing the workingSet tree nodes
	 * @author powelldan
	 *
	 */
	class MyTreeModelListener implements TreeModelListener {
	    public void treeNodesChanged(TreeModelEvent e) {
	        DefaultMutableTreeNode node;
	        node = (DefaultMutableTreeNode)
	                 (e.getTreePath().getLastPathComponent());

	        /*
	         * If the event lists children, then the changed
	         * node is the child of the node we have already
	         * gotten.  Otherwise, the changed node and the
	         * specified node are the same.
	         */
	        try {
	            int index = e.getChildIndices()[0];
	            node = (DefaultMutableTreeNode)
	                   (node.getChildAt(index));
	        } catch (NullPointerException exc) {}

	    }
	    public void treeNodesInserted(TreeModelEvent e) {
	    }
	    public void treeNodesRemoved(TreeModelEvent e) {
	    }
	    public void treeStructureChanged(TreeModelEvent e) {
	    }
	}
}
