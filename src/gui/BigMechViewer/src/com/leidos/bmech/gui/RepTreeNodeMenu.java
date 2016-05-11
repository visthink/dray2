/**
 * 
 * 
 * 
 */
package com.leidos.bmech.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import dray.j.Producer.Table;
//import dray.j.Producer.Entry;
//import dray.j.Toys;
import dray.j.VisualElement.El;

//import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

//import com.leidos.bmech.model.Layer;
//import com.leidos.bmech.model.TypeTag;
import com.leidos.bmech.model.WorkingSet;
import com.leidos.bmech.view.DataManagerView;

@SuppressWarnings("serial")
public class RepTreeNodeMenu extends JPopupMenu implements ActionListener {
	JMenuItem			splitEl;
	ViewerApp			mainApp;
	WorkingSet			targetWS;
	El						targetEl;

	DataManagerView	view;

	JMenuItem			saveAsJSON;

	public RepTreeNodeMenu(ViewerApp p, TreePath selPath, El el) {

		mainApp = p;
		view = p.getDataManager().getView();

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();

		// Single menu item - Save node's rep as JSON.
		saveAsJSON = new JMenuItem("Save as JSON");
		saveAsJSON.putClientProperty("node", node);
		saveAsJSON.addActionListener(this);
		add(saveAsJSON);

		System.out.println("*** RepTreeNode is " + node.toString());
		System.out.println("  ** User object is " + node.getUserObject().toString());
		// System.out.println("JSON:");
		// System.out.println(Table.layerRepToJSON(node.getUserObject()));

	}

	public void actionPerformed(ActionEvent e) {

		JMenuItem source = (JMenuItem) (e.getSource());
		if (source == saveAsJSON) {
			System.out.println(("HERE IS WHERE WE WOULD PRINT OUT"));
			System.out.println("Source is " + source.toString());
			System.out.println("Source class is " + source.getClass().toString());
			System.out.println("Node string is " + saveAsJSON.getClientProperty("node"));
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) saveAsJSON.getClientProperty("node");
			Table.layerRepToJSON(node.getUserObject());
		}
		// if (source == createWS) {
		// mainApp.insertSelectedAsWS(TypeTag.UNKNOWN);
		// }
	}

	// public void itemStateChanged(ItemEvent e) {
	// JMenuItem source = (JMenuItem) (e.getSource());
	// String s = "Item event detected." + "\n" + " Event source: " +
	// source.getText() + " (an instance of "
	// + getClassName(source) + ")" + "\n" + " New state: "
	// + ((e.getStateChange() == ItemEvent.SELECTED) ? "selected" :
	// "unselected");
	// mainApp.appendToLog(s + "\n");
	// }

	@SuppressWarnings("unused")
	private String getClassName(JMenuItem source) {
		return "RepTreeNodeMenu";
	}
}
