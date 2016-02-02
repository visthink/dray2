package com.leidos.bmech.view;

import java.util.ArrayList;
import java.util.List;

import com.leidos.bmech.gui.ViewerApp;
import com.leidos.bmech.model.DataManager;
import com.leidos.bmech.model.WorkingSet;

// import drae.j.VisualElement.El;  // Not used?

/**
 * DataManagerView -- this class is responsible for maintaining
 * the current state of user interaction with the DataManager,
 * such as which working set is being worked on, and which 
 * elements are selected 
 * @author powelldan
 *
 */
public class DataManagerView {
	private WorkingSet currentWS;
	private List<Object> selectedEls;
	ViewerApp gui;
	DataManager dm;
	public DataManagerView(DataManager d){	
		selectedEls = new ArrayList<Object>();
		dm = d;
	}
	public WorkingSet getCurrentWS() {
		return currentWS;
	}
	public void setCurrentWS(WorkingSet currentWS) {
		this.currentWS = currentWS;
		this.selectedEls.clear();
		//if(gui != null)gui.viewWSUpdated();
	}
	public int getCurrentPage(){
		return getCurrentWS().getPage();
	}
	
	public void setCurrentPage(int page){
		setCurrentWS(dm.getPageWS(page));
	}
	public List<Object> getSelected() {
		return selectedEls;
	}
	public void setSelected(List<Object> selectedEls) {
		this.selectedEls = selectedEls;
	}
	public ViewerApp getGui() {
		return gui;
	}
	public void setGui(ViewerApp gui) {
		this.gui = gui;
	}
	
	
}
