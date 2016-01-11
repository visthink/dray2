package com.leidos.bmech.analysis;

import java.util.HashMap;
import java.util.List;

/**
 * Evidence -- representation of a piece of evidence consists of:
 * belief: how strong this piece of evidence is
 * support: what this evidence is supported by. usually a string, but
 * 		represented as an Object
 * classification: what this evidence says about the object (i.e. PROTEIN)
 * @author powelldan
 *
 */
public class Evidence extends HashMap implements Comparable {

	public Evidence(Double b , Object support , String classification){
		this.put("belief" , b);
		this.put("support", support);
		this.put("classification", classification);
		this.put("name", this.toString()); // Modified to use new string method.
	//	this.put("resolved", false); // Default value is false.
	//	this.put("group", false);    // Default value is false.
	}
	
	public int compareTo(Object o1){
        if (this.get("belief") == ((Evidence) o1).get("belief"))
            return 0;
        else if ((Double)(this.get("belief")) > (Double)((Evidence) o1).get("belief"))
            return 1;
        else return -1;
   	}

    /**
     * Returns a belief level between 0.0 and 1.0 for the given Evidence.
     * 
     * @return the belief level.
     */
	public Double getBelief() {
		return (Double)get("belief");
	}

	/**
	 * Returns an object representing the support for the given
	 * evidence. Often, but not always, a String value.
	 * @return support object.
	 */
	public Object getSupport() {
		return get("support");
	}

	/**
	 * Returns the classification of the object as a string.
	 * 
	 * @return Classification string.
	 */
	public String getClassification() {
		return (String) get("classification");
	}
	
	/**
	 * Returns true if the classification is unambigous (i.e., belief = 1.0),
	 * false otherwise.
	 * 
	 * @return true if resolved.
	 */
	public Boolean isResolved() {
		if (this.containsKey("resolved")) { // Default == false.
   		   return (Boolean) get("resolved");
		} else {
		   return false;
		}
	}
	
	/**
	 * Marks the given evidence as resolved.
	 */
	public void markResolved() {
	   	this.put("resolved", true);
	}
	
	/**
	 * Returns true if the evidence represents a composite
	 * or group object.
	 * 
	 * @return true if evidence represents a group.
	 */
	public Boolean isGroup() {
		return (Boolean) get("group");
	}
	
	/**
	 * Mark the evidence as representing a group.
	 */
	public void markAsGroup() {
		this.put("group",true);
	}
	
	public String toString() {
		return String.format("<Ev: %S [%s,%.2f]>", getClassification(), getSupport(), getBelief());
				
	}
	
	/**
	 * Set the part of the represented object. For example, if
	 * this evidence represents a column, the parts contain 
	 * the cells. Evidence for these cells would then need to be 
	 * retrieved from the containing evidence table.
	 * 
	 * @param objs a list of objects
	 */
	public void setParts (List<Object> objs) {
		this.put("parts", objs);
	}

	/**
	 * Return the parts of the represented object. Returns
	 * nil if there are no parts.
	 * @return List of objects.
	 */
	public List<Object> getParts () {
		return (List<Object>) this.get("parts");
	}
}
