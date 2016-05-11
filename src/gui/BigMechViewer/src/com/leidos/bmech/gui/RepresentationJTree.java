package com.leidos.bmech.gui;

import java.util.ArrayList;
import java.util.Arrays;
// import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.leidos.bmech.model.Layer;
import com.leidos.bmech.gui.RepTreeNodeMenu;
import dray.j.VisualElement.VText;
import dray.j.Producer.Table;

public class RepresentationJTree extends JTree {

	private static final long	serialVersionUID	= -6900161300006440929L;

	RepresentationJTree			pthis;
	ViewerApp						app;
	public boolean					wsChangedAlready	= false;

	final static Object			keywordID			= Table.stringToKeyword("ID");
	final static Object			keywordName			= Table.stringToKeyword("name");

	public RepresentationJTree(ViewerApp app) {

		super(new RepTreeNode("No Document Loaded"));
		this.app = app;
		pthis = this;

		this.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				RepTreeNode selectedNode = (RepTreeNode) pthis.getLastSelectedPathComponent();
				if (selectedNode == null)
					return;
				TreeNode[] path = selectedNode.getPath();
				List<TreeNode> pathList = new ArrayList<TreeNode>(Arrays.asList(path));
				pathList.remove(0);// get rid of the root node
				Object obj = selectedNode.getUserObject();

				List<Object> elList = new ArrayList<Object>();
				if (obj != null) {
					pthis.app.getView().getCurrentWS().getLayerList().getElsUnder(elList, obj);
				}
				pthis.app.getView().setSelected(elList);
				pthis.app.canvas.repaint();

			}
		});

		addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				int selRow = getRowForLocation(e.getX(), e.getY());
				TreePath selPath = getPathForLocation(e.getX(), e.getY());
				if (selRow != -1) {
					if (SwingUtilities.isRightMouseButton(e)) {
						/// @@@ RESt GOES HERE ONCE WE HAVE REP-MENU
						RepTreeNodeMenu menu;
						menu = new RepTreeNodeMenu(pthis.app, selPath, null);
						menu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}

		});

	}

	public void reload() {
		RepTreeNode top = new RepTreeNode("Layers");
		DefaultTreeModel model = new DefaultTreeModel(top);

		for (Layer layer : app.getView().getCurrentWS().getLayerList().values()) {
			RepTreeNode layerNode = new RepTreeNode(layer.getName());
			top.add(layerNode);
			addObject(layer.getRep(), layerNode);
		}
		setModel(model);

	}

	private void addObject(Object obj, RepTreeNode parentNode) {

		if (obj instanceof VText) {
			addObjectVText((VText) obj, parentNode);
		} else if (obj instanceof Map) {
			addObjectMap((Map<?, ?>) obj, parentNode, null);
		} else if (obj instanceof List) {
			addObjectList((List<?>) obj, parentNode, "List");
		} else {
			System.out.println("No method to add " + obj.toString() + " to tree.");
			System.out.println("Obj class is " + obj.getClass().toString());
			System.out.println("Parent object is " + parentNode.toString());
			System.out.println("Parent object class is " + parentNode.getClass().toString());
		}
	}

	private void addObjectVText(VText obj, RepTreeNode parentNode) {
		RepTreeNode elNode = new RepTreeNode(obj, "VText: " + obj.getText());
		parentNode.add(elNode);
	}

	private void addObjectMap(java.util.Map<?, ?> map, RepTreeNode parentNode, String label) {

		RepTreeNode mapNode;

		// Set up node to represent map and add to parent.

		// Determine the label, if not assigned.

		String mapNodeLabel;
		if (label != null) {
			mapNodeLabel = label;
		} else {
			if (map.containsKey(keywordID)) {
				mapNodeLabel = map.get(keywordID).toString();
			} else if (map.containsKey(keywordName)) {
				mapNodeLabel = map.get(keywordName).toString();
			} else {
				mapNodeLabel = "unlabeled map";
			}
		}

		// Create new node and add to parent.
		mapNode = new RepTreeNode(map, mapNodeLabel);
		parentNode.add(mapNode);

		// If map contains entries (likely), handle each one as a node.
		for (Object key : map.keySet()) {

			Object keyValue = map.get(key);

			if (keyValue instanceof Map) {

				addObjectMap((Map<?, ?>) keyValue, mapNode, key.toString());

			} else if (keyValue instanceof List) {

				addObjectList((List<?>) keyValue, mapNode, key.toString());

			} else { // key-value pair.

				RepTreeNode keyNode = new RepTreeNode(key + " = " + map.get(key));
				mapNode.add(keyNode);

			}
		}

	}

	private void addObjectList(List<?> obj, RepTreeNode parentNode, String name) {

		RepTreeNode listNode;
		listNode = new RepTreeNode(obj, name);

		parentNode.add(listNode);

		for (Object subobj : obj) {
			addObject(subobj, listNode);
		}
	}

} // endClass
