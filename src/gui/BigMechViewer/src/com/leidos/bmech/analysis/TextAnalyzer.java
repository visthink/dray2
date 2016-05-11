package com.leidos.bmech.analysis;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.leidos.bmech.model.Layer;
import com.leidos.bmech.model.WorkingSet;

import dray.j.VisualElement.El;
import dray.j.VisualElement.VText;
import dray.j.VisualElement.VTextToken;

public class TextAnalyzer {
	/*
	 * public static Layer produceGeneLayer(WorkingSet ws){ String combined = "";
	 * for(El el : ws.getItems()){ if (el instanceof VText){ for(Object tokObj :
	 * (List)el.getItems()){ VTextToken tok = (VTextToken) tokObj; combined +=
	 * tok.text + " "; } //combined += ((VText) el).text + " "; } } //Set<String>
	 * ret = getGenes(combined);
	 * 
	 * return getGenesTokens(ws);//buildEntityRep(ws, ret); }
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Layer buildEntityRep(WorkingSet ws, Set<String> geneNames) {
		String combined = "";
		// map from the character index in the combined string to VText
		List<VTextToken> charToVText = new ArrayList<VTextToken>();
		// map from the vtext to the index of where the vtext starts in the
		// combined string. used later to calculate the start and end points
		Map<VTextToken, Integer> starts = new HashMap<VTextToken, Integer>();
		for (El el : ws.getItems()) {
			if (el instanceof VText) {
				for (Object tokObj : (List) el.getItems()) {
					VTextToken tok = (VTextToken) tokObj;
					String subStr = (String) tok.text;
					int startIdx = combined.length();
					starts.put(tok, startIdx);
					combined += subStr.toLowerCase() + " ";
					int endIdx = combined.length();
					for (int i = startIdx; i < endIdx; i++) {
						charToVText.add(tok);
					}
				}
			}
		}
		// now we can quickly look up the vtext that a certain point in the
		// combined string belongs to

		// find mentions of each entity

		Layer entityLayer = new Layer("NamedEntities");
		// ws.getLayerList().addLayer(entityLayer);
		List<Map<String, Object>> rep = entityLayer.getRep();
		Map<String, Object> entityMap = new HashMap<String, Object>();
		rep.add(entityMap);
		entityMap.put("name", "Genes");

		for (String entity : geneNames) {
			int searchIdx = 0;
			// the map from entity names to a list of occurrences
			List occurrences = new ArrayList();
			entityMap.put(entity, occurrences);
			while (searchIdx >= 0) {
				searchIdx = combined.indexOf(entity.toLowerCase(), searchIdx);
				System.out.println("searchIdx " + searchIdx);
				if (searchIdx >= 0) {

					List<VTextToken> containingVTexts = new ArrayList<VTextToken>();
					for (int i = searchIdx; i < searchIdx + entity.length(); i++) {
						VTextToken txt = charToVText.get(i);
						if (!containingVTexts.contains(txt)) {
							containingVTexts.add(txt);
						}
					}

					searchIdx += entity.length();
					Map<String, Object> thisOccurrence = new HashMap<String, Object>();
					thisOccurrence.put("Tokens", containingVTexts);
					thisOccurrence.put("name", entity + "[" + occurrences.size() + "]");
					occurrences.add(thisOccurrence);

				} else {
					System.out.println("Entity not found " + entity);
				}
			}
		}
		System.out.println("Returning " + entityLayer);
		return entityLayer;
	}

}
