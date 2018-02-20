package com.leidos.bmech.analysis;

import java.util.List;

/*
 * Gazetteer object for looking up entity names for particular types.
 */
public interface Gazetteer {

	/*
	 * The name of the Gazetteer.
	 */
	String getName();

	/*
	 * The item class as a string.
	 */
	String getItemClass(Object item);

	/*
	 * A list of all items classes for this object.
	 */
	List<String> getItemClasses(Object item);

	/* 
	 * Add a single item to the classification. 
	 */
	void addItemToClass(Object item, String destClass);

	/*
	 * Add multiple items to a classification.
	 */
	void addItemsToClass(List<Object> items, String destClass);

	
}
