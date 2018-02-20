package com.leidos.bmech.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedList;

import com.leidos.bmech.gui.UtiliBuddy;
import com.leidos.bmech.model.WorkingSet;

import dray.j.VisualElement.El;
import dray.j.VisualElement.VText;

/**
 * A Gazetteer for gene names. Not functional at the moment.
 * 
 * @author Dan Powell
 *
 */
public class GeneGazetteer implements Gazetteer {
	@SuppressWarnings("rawtypes")
	Set		set;
	String	thisClassName	= "gene_protein";

	public GeneGazetteer(WorkingSet ws) {
		set = new HashSet<String>();
		load(ws);
	}

	public GeneGazetteer(WorkingSet ws, Set<El> exclusions) {
		set = new HashSet<String>();
		loadWithExceptions(ws, exclusions);
	}

	@SuppressWarnings({ "unchecked", "null" })
	public void load(WorkingSet head) {
		// for now load using Arizona Processors
		// System.out.println("Working Set " + head + " " +
		// ws.getItems().size());
		// TextAnalyzer ta = new TextAnalyzer();
		// String combined = "";
		for (WorkingSet page : head.getChildren()) {
			for (El el : page.getItems()) {
				if (el instanceof VText) {
					// for(Object tokObj : (List)el.getItems()){
					// VTextToken tok = (VTextToken) tokObj;
					// combined += tok.text + " ";
					// }
					// combined += ((VText) el).text + " ";
				}
			}
		}
		// removed_arizona Set<String> tmp =
		// ta.getGenes(combined.toLowerCase().replace("*", ""));
		Set<String> tmp = null;
		set.addAll(tmp);
		for (String gene : tmp) {
			for (String partialGene : gene.split(" ")) {
				if (!UtiliBuddy.isNumeric(partialGene)) {
					set.add(partialGene);
				}
			}
		}
	}

	public List<El> allContainedEls(WorkingSet headWS, Set<El> exclusions) {
		List<El> res = new ArrayList<El>();
		for (WorkingSet page : headWS.getChildren()) {
			res.addAll(page.getItems());
		}
		res.removeAll(exclusions);
		return res;
	}

	@SuppressWarnings({ "unchecked", "null" })
	public void loadWithExceptions(WorkingSet headWS, Set<El> exclusions) {
		// for now load using Arizona Processors
		// System.out.println("Working Set " + head + " " +
		// ws.getItems().size());
		// TextAnalyzer ta = new TextAnalyzer();
		List<El> elist = allContainedEls(headWS, exclusions);
		String combined = "";
		for (El el : elist) {
			if (el instanceof VText) {
				combined = combined + el.getText() + " ";
			}
		}
		Set<String> tmp = null;// removed_arizona
		// ta.getGenes(combined.toLowerCase().replace("*",
		// ""));
		set.addAll(tmp);
		for (String gene : tmp) {
			for (String partialGene : gene.split(" ")) {
				if (!UtiliBuddy.isNumeric(partialGene)) {
					set.add(partialGene);
				}
			}
		}
	}

	@Override
	public String getName() {
		return "AZ-BANNER";
	}

	@Override
	public String getItemClass(Object item) {
		if (set.contains(item.toString().toLowerCase())) {
			return thisClassName;
		}
		return null;
	}

	@Override
	public List<String> getItemClasses(Object item) {
		// Return a list of potential classes for this item.
		if (set.contains(item.toString().toLowerCase())) { 
			// Note: Always singleton, so return as a list.
			LinkedList<String> res = new LinkedList<String>();
			res.add(thisClassName.toString());
			return res;
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addItemsToClass(List<Object> items, String destClass) {
		if (destClass.equalsIgnoreCase(thisClassName))
			set.addAll(items);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addItemToClass(Object item, String destClass) {
		if (destClass.equalsIgnoreCase(thisClassName))
			set.add(item);
	}

	public String toString() {
		String ret = "{gene: ";
		for (Object word : set) {
			ret += word + ", ";
		}
		ret += "}";
		return ret;
	}

	@SuppressWarnings("rawtypes")
	public Set getSet() {
		return set;
	};

}
