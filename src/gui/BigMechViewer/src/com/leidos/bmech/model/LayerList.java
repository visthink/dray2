/**
 * com.leidos.bmech.model.LayerList is a list of Layer objects indexed by layer name.
 */
package com.leidos.bmech.model;

//import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.tree.TreeNode;
import com.leidos.bmech.gui.UtiliBuddy;
import dray.j.VisualElement.El;
import dray.j.VisualElement.VText;

public class LayerList extends LinkedHashMap<String, Layer> {

	private static final long serialVersionUID = 3668368849271492741L;

	// CONSTRUCTOR

	public LayerList(WorkingSet p) {
		super();
		addLayer(new Layer("all"));
	}

	// GETTERS

	public Layer getLayerByName(String name) {
		return (Layer) this.get(name);
	}

	public Layer getBase() {
		return get("all");
	}

	// SETTERS

	public void addLayer(Layer layer) {
		layer.setColor(UtiliBuddy.getAColor(this.size()));
		put(layer.getName(), layer);
	}

	public void addElementToLayer(String destLayer, El el) {

		if (this.containsKey(destLayer)) {
			Layer layer = this.get(destLayer);
			if (!layer.containsEl(el)) {
				layer.addElement(el);
			}
			;
		} else {
			addLayer(new Layer(destLayer));
			addElementToLayer(destLayer, el);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })

	public Object getObjectOnPath(Object root, List<TreeNode> path) {

		Object ret = null;
		String nodeName = path.get(0).toString();
		path.remove(0);

		if (root instanceof LayerList) {
			ret = ((LayerList) root).getLayerByName(nodeName);
		} else if (root instanceof Layer) {
			Layer layer = (Layer) root;
			// first try to find the right representation
			for (Map map : layer.getRep()) {
				if (nodeName.equals(map.get("name"))) {
					ret = map;
				} else if (nodeName.equals(map.get(":name"))) {
					ret = map;
				}
			}
			if (ret == null) {// if we still haven't found it
				int repIndex = Integer.parseInt(nodeName.split("#")[1]) - 1;
				ret = layer.getRep().get(repIndex);
			}
		} else if (root instanceof Map) {

			Map map = (Map) root;
			ret = map.get(nodeName);
		} else if (root instanceof List<?>) {
			for (Object el : (List<Object>) root) {
				if (el instanceof HashMap) {
					if (nodeName.equals(((HashMap) el).get("name"))) {
						ret = el;
					} else if (nodeName.equals(((HashMap) el).get(":name"))) {
						ret = el;
					}
				}
				if (el instanceof El) {
					if (el.toString().equals(nodeName)) {
						ret = el;
					}
				}
			}
		} else if (root instanceof El) {
			ret = root;
		}

		if (ret == null) {
			return null;
		}

		if (path.size() == 0) {
			return ret;
		} else {
			return getObjectOnPath(ret, path);
		}
	}

	@SuppressWarnings("rawtypes")
	public void getElsUnder(List<Object> list, Object root) {

		if (root instanceof VText) {
			list.add((El) root);
		} else if (root instanceof LayerList) {
			LayerList layerList = (LayerList) root;
			for (Layer layer : layerList.values()) {
				getElsUnder(list, layer);
			}
		} else if (root instanceof Layer) {
			Layer layer = (Layer) root;
			for (Map map : layer.getRep()) {
				getElsUnder(list, map);
			}
		} else if (root instanceof Map) {
			Map map = (Map) root;
			for (Object obj : map.values()) {
				getElsUnder(list, obj);
			}
		} else if (root instanceof List<?>) {
			for (Object obj : (List) root) {
				getElsUnder(list, obj);
			}
		}
	}

}
