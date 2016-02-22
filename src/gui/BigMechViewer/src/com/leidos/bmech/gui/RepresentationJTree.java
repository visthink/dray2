package com.leidos.bmech.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import com.leidos.bmech.model.Layer;
import drae.j.VisualElement.VText;
import drae.j.VisualElement.El;

public class RepresentationJTree extends JTree {
	private static final long serialVersionUID = -6900161300006440929L;
	
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
            	Object obj = selectedNode.getUserObject();
            	
            	List<El> elList = new ArrayList<El>();
            	if(obj!=null){
            		pthis.app.getDataManager().getCurrentWS().getLayerList().getElsUnder(elList, obj);
	            }
            	pthis.app.getDataManager().setSelectedEls(elList);
            	pthis.app.canvas.repaint();
            	
            }
        });
	}
	
	public void reload() {
		RepTreeNode top = new RepTreeNode("Layers");
		DefaultTreeModel model = new DefaultTreeModel(top);		

	//	for(Layer layer : app.getView().getCurrentWS().getLayerList().values()){
	    for(Layer layer : app.getDataManager().getCurrentWS().getLayerList().values()){
	        	RepTreeNode layerNode = new RepTreeNode(layer.getName());
			top.add(layerNode);
			addObject(layer.getRep(), layerNode);
		}		
		setModel(model);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
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


