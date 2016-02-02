package com.leidos.bmech.analysis;

// import java.util.HashSet;
import java.util.List;

//import com.leidos.bmech.model.TypeTag;

public interface Gazetteer {
	//private LoadState loadState;
	
	// void makeGazetter(List<String> classes); // Not used.
	
	String getName(); 
	
	String getItemClass(Object item);
	
	List<String> getItemClasses(Object item);
	
	void addItemsToClass(List<Object> items, String destClass);
	
	void addItemToClass(Object item, String destClass);
	
/*	
	public enum LoadState{
		NOT_LOADED,
		LOADING,
		LOADED
	}
	
	public void setLoaded(){
		loadState=LoadState.LOADED;
	}
	public void setLoading(){
		loadState=LoadState.LOADING;
	}
	public void setNotLoaded(){
		loadState=LoadState.LOADED;
	}
	public boolean isLoaded(){
		return loadState==LoadState.LOADED;
	}
	public boolean isLoading(){
		return loadState==LoadState.LOADING;
	}
	public boolean isNotLoaded(){
		return loadState==LoadState.NOT_LOADED;
	}
*/	
	
}
