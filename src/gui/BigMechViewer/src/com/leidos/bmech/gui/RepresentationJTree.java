package com.leidos.bmech.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.leidos.bmech.gui.WorkingSetJTree.MyTreeModelListener;
import com.leidos.bmech.model.Layer;

import drae.j.VisualElement.El;
import drae.j.VisualElement.VTable;
import drae.j.VisualElement.VText;
import clojure.lang.Named;
public class RepresentationJTree extends JTree {
	
	RepresentationJTree pthis;
	ViewerApp app;
	public boolean wsChangedAlready = false;
	public RepresentationJTree(ViewerApp app){
		super(new RepTreeNode("No Document Loaded"));
		this.app = app;
		pthis = this;
		this.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
            	RepTreeNode selectedNode = (RepTreeNode) pthis.getLastSelectedPathComponent();
            	if(selectedNode == null) return;
            	TreeNode[] path = selectedNode.getPath();
            	List<TreeNode> pathList = new ArrayList<TreeNode>(Arrays.asList(path));
            	pathList.remove(0);//get rid of the root node
            	//Object obj = pthis.app.getView().getCurrentWS().getLayerList().getObjectOnPath(pthis.app.getView().getCurrentWS().getLayerList(), pathList);   	
            	Object obj = selectedNode.getUserObject();
            	
            	List<Object> elList = new ArrayList<Object>();
            	if(obj!=null){
            		pthis.app.getView().getCurrentWS().getLayerList().getElsUnder(elList, obj);
	            }
            	pthis.app.getView().setSelected(elList);
            	pthis.app.canvas.repaint();
            	//pthis.app.viewWSUpdated();

            }
        });
	}
	
	public void reload() {
		RepTreeNode top = new RepTreeNode("Layers");
		DefaultTreeModel model = new DefaultTreeModel(top);		

		for(Layer layer : app.getView().getCurrentWS().getLayerList().values()){
			RepTreeNode layerNode = new RepTreeNode(layer.getName());
			top.add(layerNode);
			int count = 1;
			addObject(layer.getRep(), layerNode);
			/*for(Map<String, Object> repMap : layer.getRep()){
				String name;
				if(repMap.containsKey("name")){
					name = (String) repMap.get("name");
				} else {
					name = "map #" + count;
				}
				DefaultMutableTreeNode mapNode = new DefaultMutableTreeNode(name);
				layerNode.add(mapNode);
				//addMap(repMap, mapNode);
				addObject(repMap, mapNode);

			}*/
		}		
		setModel(model);

	}

	private void addObject(Object obj, RepTreeNode parentNode){
		if(obj instanceof VText){
			RepTreeNode elNode = new RepTreeNode(obj);		
			parentNode.add(elNode);
		}else if(obj instanceof Map){
			RepTreeNode mapNode;
			
			Map<Object, Object> map  = (Map<Object, Object>) obj;
			Map<String,Object> strMap = new HashMap<String, Object>();
			for(Object o : map.keySet()){
				String str = o.toString();
				if (str.startsWith(":"))
					str = str.substring(1);
				strMap.put(str, map.get(o));
				
			}
			
			if (map.containsKey("ID")){
				mapNode = new RepTreeNode(strMap, strMap.get("ID").toString());
				parentNode.add(mapNode);
			} else if (strMap.containsKey("name")){
				mapNode = new RepTreeNode(strMap);
				parentNode.add(mapNode);
			}else {
				mapNode = parentNode;
			}
			for(Object key : strMap.keySet()){
				if(!key.equals("name")){
					Object subobj = strMap.get(key);
					if(subobj instanceof Map ||	subobj instanceof List){
						RepTreeNode keyNode = new RepTreeNode(subobj, key.toString());
						mapNode.add(keyNode);
						addObject(subobj, keyNode);
					} else {
						RepTreeNode keyNode = new RepTreeNode(key + ": " + strMap.get(key));		
						mapNode.add(keyNode);
					}
				}
				
			}
		} else if (obj instanceof List){
			for(Object subobj : (List) obj){
				addObject(subobj, parentNode);
			}
		} 
	}
	
	
	
	
	

}


