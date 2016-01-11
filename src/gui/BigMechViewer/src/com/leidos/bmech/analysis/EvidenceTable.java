package com.leidos.bmech.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * EvidenceTable: A mapping of an object such as a column or a cell to
 * a list of evidence. Essentially just a hashmap with additional functions 
 * @author powelldan
 *
 */
public class EvidenceTable extends HashMap<Object, List<Evidence>> {
	
	public EvidenceTable(){

	}
	
	/**
	 * Add a piece of evidence to an entity
	 * @param entity
	 * @param classification
	 * @param support
	 * @param belief
	 */
	public void addEvidence(Object entity, 
			String classification, 
			Object support, 
			Double belief){
		
		addEvidence(entity, new Evidence (belief, support, classification));		
	}

	/**
	 * add a piece of evidence to an entity
	 * @param entity
	 * @param evidence
	 */
	public void addEvidence(Object entity, Evidence evidence) {
		// TODO Auto-generated method stub
		if(this.containsKey(entity)){
			this.get(entity).add(evidence);
		} else { //new entity
			ArrayList<Evidence> tmp = new ArrayList<Evidence>();
			tmp.add(evidence);
			this.put(entity, tmp);
		}
	}
	
	/**
	 * retrieve all evidence for the given entity
	 * @param entity
	 * @return the list of evidence objects. If object is not in the map, an
	 * empty list is returned.
	 */
	public List<Evidence> getEvidenceFor(Object entity){
		if(this.containsKey(entity))
			return this.get(entity);
		else return new ArrayList<Evidence>();
		
	}
	/**
	 * Gets the strength of the highest strength evidence for a given classification
	 * @param entity the object(i.e. column or cell) to search evidence of
	 * @param classification the classification to search for evidence of
	 * @return negative value if not found, strongest belief otherwise
	 */
	public double getBelief(Object entity, String classification){
		double ret = -1;
		for(Evidence ev : getEvidenceFor(entity)){
			if(ev.getClassification().equalsIgnoreCase(classification) && ev.getBelief() > ret){
				ret = ev.getBelief();
			}
		}
		return ret;
	}
	

	
}
