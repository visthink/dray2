package com.leidos.bmech.analysis;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.leidos.bmech.model.Layer;
import com.leidos.bmech.model.WorkingSet;

import drae.j.VisualElement.El;
import drae.j.VisualElement.VText;
import drae.j.VisualElement.VTextToken;


public class TextAnalyzer {
/*
	public static Layer produceGeneLayer(WorkingSet ws){
		String combined = "";
		for(El el :  ws.getItems()){
			if (el instanceof VText){
				for(Object tokObj : (List)el.getItems()){
					VTextToken tok = (VTextToken) tokObj;
					combined += tok.text + " ";
				}
				//combined += ((VText) el).text + " ";
			}
		}
		//Set<String> ret = getGenes(combined);
		
		return getGenesTokens(ws);//buildEntityRep(ws, ret);		
	}
	*/
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Layer buildEntityRep(WorkingSet ws, Set<String> geneNames){
		String combined = "";
		//map from the character index in the combined string to VText
		List<VTextToken> charToVText = new ArrayList<VTextToken>();
		//map from the vtext to the index of where the vtext starts in the
		//combined string. used later to calculate the start and end points
		Map<VTextToken, Integer> starts = new HashMap<VTextToken, Integer>(); 
		for(El el :  ws.getItems()){
			if (el instanceof VText){
				for(Object tokObj : (List)el.getItems()){
					VTextToken tok = (VTextToken) tokObj;
					String subStr = (String)tok.text;
					int startIdx = combined.length();
					starts.put(tok, startIdx);
					combined += subStr.toLowerCase() + " ";
					int endIdx = combined.length();
					for(int i = startIdx; i < endIdx; i++){
						charToVText.add(tok);
					}
				}
			}
		}
		//now we can quickly look up the vtext that a certain point in the
		//combined string belongs to
		
		//find mentions of each entity
		
		Layer entityLayer = new Layer("NamedEntities");
		//ws.getLayerList().addLayer(entityLayer);
		List<Map<String, Object>> rep = entityLayer.getRep();
		Map<String, Object> entityMap = new HashMap<String, Object>();
		rep.add(entityMap);
		entityMap.put("name", "Genes");
		
		for(String entity : geneNames){
			int searchIdx = 0;
			//the map from entity names to a list of occurrences
			List occurrences = new ArrayList();
			entityMap.put(entity, occurrences);
			while(searchIdx >= 0){
				searchIdx = combined.indexOf(entity.toLowerCase(), searchIdx);
				System.out.println("searchIdx " +  searchIdx);
				if(searchIdx >= 0){
					
					List<VTextToken> containingVTexts = new ArrayList<VTextToken>();
					for(int i =searchIdx; i < searchIdx+entity.length(); i++){
						VTextToken txt = charToVText.get(i);
						if(!containingVTexts.contains(txt)){
							containingVTexts.add(txt);
						}	
					}

					//int start = searchIdx - starts.get(containingVTexts.get(0));
					//int end = searchIdx + entity.length() - 1 - starts.get(containingVTexts.get(containingVTexts.size()-1));
					searchIdx+=entity.length();
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
	


	/*
	public static Set<String> getGenes(String text){
		/*
		Processor proc = new BioNLPProcessor(false, true, false, 100);
		Document doc = proc.mkDocument(text);
		proc.tagPartsOfSpeech(doc);
		proc.lemmatize(doc);
		proc.recognizeNamedEntities(doc);
		//proc.labelSemanticRoles(doc);
		proc.parse(doc);
		doc.clear();
		 // let's print the sentence-level annotations
        int sentenceCount = 0;
        Set<String> geneList = new HashSet<String>();
        int docOffset = 0;
        for (Sentence sentence: doc.sentences()) {
            int index = 0;
            boolean midgene = false;
            String geneName = "";
            for(String entity : sentence.entities().get()){
            	if(entity.equals("B-GENE")){
            		midgene = true;
            		geneName += sentence.words()[index];
            	}else if (entity.equals("I-GENE")){
            		geneName += " " + sentence.words()[index];
            	}else if (entity.equals("O")){
            		if(midgene){
            			geneList.add(geneName);
            			geneName = "";
            		}
            		midgene = false;       		
            	} else {
            		System.out.println("Found other entity: " + entity);
            	}
            	if(midgene){
            		geneList.add(geneName);
            	}
            	index++;
            }

            sentenceCount += 1;
            docOffset += sentence.getSentenceText().length();
        }
        return geneList;
	}
	
	*/

/*
public static Layer getGenesTokens(WorkingSet ws){
	String text = "";
	Layer entityLayer = new Layer("NamedEntities");
	//ws.getLayerList().addLayer(entityLayer);
	List<Map<String, Object>> rep = entityLayer.getRep();
	Map<String, Object> entityMap = new HashMap<String, Object>();
	rep.add(entityMap);
	entityMap.put("name", "Genes");
	
	
	
	//map from the character index in the combined string to VText
	List<VTextToken> charToVText = new ArrayList<VTextToken>();
	//map from the vtext to the index of where the vtext starts in the
	//combined string. used later to calculate the start and end points
	Map<VTextToken, Integer> starts = new HashMap<VTextToken, Integer>(); 
	for(El el :  ws.getItems()){
		if (el instanceof VText){
			for(Object tokObj : (List)el.getItems()){
				VTextToken tok = (VTextToken) tokObj;
				String subStr = (String)tok.text;
				int startIdx = text.length();
				starts.put(tok, startIdx);
				text += subStr.toLowerCase() + " ";
				int endIdx = text.length();
				for(int i = startIdx; i < endIdx; i++){
					charToVText.add(tok);
				}
			}
		}
	}
	
	Processor proc = new BioNLPProcessor(false, true, false, 100);
	Document doc = proc.mkDocument(text);
	proc.tagPartsOfSpeech(doc);
	proc.lemmatize(doc);
	proc.recognizeNamedEntities(doc);
	//proc.labelSemanticRoles(doc);
	proc.parse(doc);
	doc.clear();
	 // let's print the sentence-level annotations
    int sentenceCount = 0;
    Set<String> geneList = new HashSet<String>();
    boolean midgene = false;
    String geneName = "";
    List<VTextToken> tokensForThisGene = null;
    for (Sentence sentence: doc.sentences()) {
        int index = 0;

        for(String entity : sentence.entities().get()){
        	if(entity.equals("B-GENE")){
        		if(midgene){
        			geneList.add(geneName);
        			addToEntityMap(entityMap, geneName, tokensForThisGene);
        			geneName = "";
        			
        		}
        		midgene = true;
        		geneName += sentence.words()[index];
        		int start = sentence.startOffsets()[index];
        		tokensForThisGene = new ArrayList<VTextToken>();
        		tokensForThisGene.add(charToVText.get(start));
        	}else if (entity.equals("I-GENE")){
        		geneName += " " + sentence.words()[index];
        		int start = sentence.startOffsets()[index];
        		VTextToken tmp = charToVText.get(start);
        		if(!tokensForThisGene.contains(tmp))
        			tokensForThisGene.add(tmp);
        	}else if (entity.equals("O")){
        		if(midgene){
        			geneList.add(geneName);
        			addToEntityMap(entityMap, geneName, tokensForThisGene);
        			geneName = "";
        			
        		}
        		midgene = false;       		
        	} else {
        		System.out.println("Found other entity: " + entity);
        	}

        	index++;
        }

        sentenceCount += 1;
    }
	if(midgene){
		geneList.add(geneName);
		addToEntityMap(entityMap, geneName, tokensForThisGene);
	}
    return entityLayer;
}
*/
	
/*
private static void addToEntityMap(Map<String, Object> entityMap, String geneName, List<VTextToken> tokens){
	List<Map> occurrences = null;
	if(entityMap.containsKey(geneName)){
		occurrences = (List<Map>)entityMap.get(geneName);
	} else {
		occurrences = new ArrayList<Map>();
		entityMap.put(geneName, occurrences);
	}
	Map<String, Object> thisOccurrence = new HashMap<String, Object>();
	thisOccurrence.put("Tokens", tokens);

	
	thisOccurrence.put("name", geneName + "[" + occurrences.size() + "]");
	occurrences.add(thisOccurrence);
}
*/
	

}
