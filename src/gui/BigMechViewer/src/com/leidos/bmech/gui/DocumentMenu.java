package com.leidos.bmech.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

import drae.j.Producer.Table;
import drae.j.Producer.Entry;
import drae.j.Toys;
import drae.j.VisualElement.El;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.leidos.bmech.model.Layer;
import com.leidos.bmech.model.TypeTag;
import com.leidos.bmech.model.WorkingSet;
import com.leidos.bmech.view.DataManagerView;

public class DocumentMenu extends JPopupMenu implements ActionListener{
	JMenuItem createWS;
	JMenuItem deleteWS;
	JMenuItem editTags;
	JMenuItem createTableWS;
	JMenuItem createColumnWS;
	JMenuItem createHeaderWS;
	JMenuItem createCaptionWS;
	JMenuItem performEvAnalysis;
	JMenuItem createIgnoreWS;
	JMenuItem createMergeWS;
	JMenuItem createRowWS;
	JMenuItem runPreprocess;
	JMenuItem upOneLevel;
	JMenuItem doAutoTable;
	JMenuItem splitEl;
	private JMenuItem createHeaderRowWS;
	private JMenuItem createFigureWS;
	ViewerApp mainApp;
	WorkingSet targetWS;
	El targetEl;
	final static int CURRENT_WS = 1;
	final static int SELECTION = 2;
	final static int OTHER_WS = 3;
	final static String strCreateNewWS = "Create New Working Set";
	final static String strDeleteWS = "Delete this Working Set";
	final static String strEditTags = "Edit tags for current WS";
	final static String strCreateTableWS = "Create table WS from selection";
	final static String strCreateColumnWS = "Create column from selection";
	
	private int mode;
	DataManagerView view;
	
	
    public DocumentMenu(ViewerApp p, WorkingSet ws, El el){
    	mainApp = p;
    	view = p.getDataManager().getView();
    	targetWS = ws;
    	if(ws == null){
    		mode = SELECTION;
    		targetWS = view.getCurrentWS();
    	} else if (targetWS == view.getCurrentWS()){
    		mode = CURRENT_WS;
    	} else mode = OTHER_WS;
    	//this.mode = mode;
    	if(mode == CURRENT_WS || mode == SELECTION){
    		upOneLevel = new JMenuItem("Go up one working set");
    		upOneLevel.addActionListener(this);
	        add(upOneLevel);
	        //only enable not parent
	        upOneLevel.setEnabled(!targetWS.isPageLevel());
	        		
	        		
	        createWS = new JMenuItem(strCreateNewWS);
	        createWS.addActionListener(this);
	        add(createWS);
	        //only enable if elements are selected
	        createWS.setEnabled(!mainApp.getView().getSelected().isEmpty());
        	
	        //only give option to preprocess if it hasnt been started
	       
	        runPreprocess = new JMenuItem("Preprocess Document");
	        runPreprocess.addActionListener(this);
	        runPreprocess.setEnabled(false);
	        runPreprocess.setToolTipText("Disabled for now");
	        add(runPreprocess);
	        runPreprocess.setEnabled(mainApp.getDataManager().getPreprocessState()<0);

	        
	        JMenu tableMenu = new JMenu ("Table Operations");
	        add(tableMenu);	       
    	    //context sensitive menu for WS creation 
            if(targetWS.hasTag(TypeTag.TABLE.name()))  { //WE ARE IN A TABLE
	        	doAutoTable = new JMenuItem("Do Auto-Table");
	        	doAutoTable.addActionListener(this);
            	tableMenu.add(doAutoTable);
    	        doAutoTable.setEnabled(targetWS.isAutoTableReady());
    	        if(targetWS.isAutoTableReady()){
    	        	doAutoTable.setToolTipText("create columns to enable auto table");
    	        }
    	        	
    	        createColumnWS = new JMenuItem(strCreateColumnWS);
    	        createColumnWS.addActionListener(this);
            	tableMenu.add(createColumnWS);
            	
            	createRowWS = new JMenuItem("Create row from selection");
    	        createRowWS.addActionListener(this);
            	tableMenu.add(createRowWS);
            	
            	createHeaderRowWS = new JMenuItem("Create header row from selection");
            	createHeaderRowWS.addActionListener(this);
            	tableMenu.add(createHeaderRowWS);
            	
            	createCaptionWS = new JMenuItem("Create caption from selection");
            	createCaptionWS.addActionListener(this);
            	tableMenu.add(createCaptionWS);
            	
            	createIgnoreWS = new JMenuItem("Tag selection to be ignored");
            	createIgnoreWS.addActionListener(this);
            	tableMenu.add(createIgnoreWS);
            	
            	
            	performEvAnalysis = new JMenuItem("Perform Evidence Analysis");
            	performEvAnalysis.addActionListener(this);
            	tableMenu.add(performEvAnalysis);
            	performEvAnalysis.setEnabled(mainApp.getDataManager().getPreprocessState()>0);
            	
        	} else if (targetWS.hasTag(TypeTag.COLUMN.name()) ||
        			targetWS.hasTag(TypeTag.ROW.name())){
            	createHeaderWS = new JMenuItem("Create header from selection");
    	        createHeaderWS.addActionListener(this);
            	tableMenu.add(createHeaderWS);
            	
            	createIgnoreWS = new JMenuItem("Tag selection to be ignored");
            	createIgnoreWS.addActionListener(this);
            	tableMenu.add(createIgnoreWS);
            	
            	createMergeWS = new JMenuItem("Tag selection to be merged");
    	        createMergeWS.addActionListener(this);
            	tableMenu.add(createMergeWS);
            	
        	} else if (targetWS.hasTag(TypeTag.FIGURE.name())){
        		createCaptionWS = new JMenuItem("Create caption from selection");
            	createCaptionWS.addActionListener(this);
            	tableMenu.add(createCaptionWS);
        	}else {
        		createTableWS = new JMenuItem(strCreateTableWS);
     	        createTableWS.addActionListener(this);
             	tableMenu.add(createTableWS);
             	
             	createFigureWS = new JMenuItem("Create figure WS from selection");
             	createFigureWS.addActionListener(this);
             	tableMenu.add(createFigureWS);
        	}
	        
    	}
        if(mode == CURRENT_WS || mode == OTHER_WS){
	        deleteWS = new JMenuItem("Delete " + targetWS.getName());
	        deleteWS.addActionListener(this);
	        add(deleteWS);
	        //only enable if not page level or doc level
	        deleteWS.setEnabled(ws.isDeletable());
	        editTags = new JMenuItem("Edit tags for " + targetWS.getName());
	        editTags.addActionListener(this);
	        add(editTags);

        }
    	
        addSeparator();
        JMenu toysMenu = new JMenu("Toys");
        add(toysMenu);
        Toys toys = new Toys();
        List cmdList = null;
        if(mode == CURRENT_WS || mode == OTHER_WS){
        	toysMenu.setText("WS Toys");
        	cmdList = toys.getWSToyList();
        	
        } else if (mode == SELECTION){
        	toysMenu.setText("Selection Toys");
        	cmdList = toys.getSelToyList();
        }
         
        for(Object cmd : cmdList){
        	String cmdStr = (String) cmd;
        	JMenuItem toyCmd = new JMenuItem(cmdStr);
        	toyCmd.addActionListener(this);
        	toysMenu.add(toyCmd);
        	
        }
        //dont show if clicked on selection
        if(mode == CURRENT_WS || mode == OTHER_WS){
	        //addSeparator();

        	
	        JMenu wsProducerMenu = new JMenu ("WS Producers");
	        add(wsProducerMenu);
	        cmdList = Table.allWSProducers();
	        if(cmdList != null){
		        for(Entry cmd : (List<Entry>)cmdList){
		        	String cmdStr = (String) cmd.key;
		        	JMenuItem producerCmd = new JMenuItem((String)cmd.name);
		        	producerCmd.setActionCommand((String) cmd.key);
		        	producerCmd.setToolTipText((String)cmd.doc);
		        	producerCmd.addActionListener(this);
		        	wsProducerMenu.add(producerCmd);
		        	
		        }
	        }
	        //addSeparator();

	        
	        JMenu layerProducerMenu = new JMenu ("Layer Producers");
	        add(layerProducerMenu);
	        cmdList = Table.allLayerProducers();
	        if(cmdList != null){
		        for(Entry cmd : (List<Entry>)cmdList){
		        	JMenuItem producerCmd = new JMenuItem((String)cmd.name);
		        	producerCmd.setActionCommand((String) cmd.key);
		        	producerCmd.setToolTipText((String)cmd.doc);
		        	producerCmd.addActionListener(this);
		        	layerProducerMenu.add(producerCmd);
		        	
		        }
	        }
        }
        if(el != null){
        	addSeparator();
        	splitEl = new JMenuItem("Split text block " + el.getText());
        	splitEl.addActionListener(this);
        	targetEl = el;
        	add(splitEl);
        }
        
    }
    public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
        if (source == createWS) {
        	mainApp.insertSelectedAsWS(TypeTag.UNKNOWN);
        } else if (source == createTableWS) {
        	mainApp.insertSelectedAsWS(TypeTag.TABLE);
        }else if (source == createFigureWS) {
        	mainApp.insertSelectedAsWS(TypeTag.FIGURE);
        } else if (source == createColumnWS) {
        	mainApp.insertSelectedAsWS(TypeTag.COLUMN);           	
        } else if (source == createCaptionWS) {
        	mainApp.insertSelectedAsWS(TypeTag.CAPTION);           	
        } else if (source == createHeaderWS) {
        	mainApp.insertSelectedAsWS(TypeTag.HEADER);           	
        }else if (source == createRowWS) {
        	mainApp.insertSelectedAsWS(TypeTag.ROW);           	
        }else if (source == createHeaderRowWS) {
        	mainApp.insertSelectedAsWS(TypeTag.HEADER_ROW);           	
        }else if (source == createIgnoreWS) {
        	mainApp.insertSelectedAsWS(TypeTag.IGNORE);           	
        }else if (source == createMergeWS) {
        	mainApp.insertSelectedAsWS(TypeTag.MERGE);           	
        } else if (source == performEvAnalysis) {
        	mainApp.AnalyzeEvidence(); 
        }else if (source == doAutoTable) {
            	mainApp.doAutoTable();           	           
        }else if (source == splitEl) {
        	//mainApp.splitEl(targetEl);           	    
        } else if (source == deleteWS) {
        	mainApp.deleteWS(targetWS, true);      	
        } else if (source == upOneLevel) {
        	mainApp.getView().setCurrentWS(targetWS.getParent());
        	mainApp.viewWSUpdated();
        } else if (source == runPreprocess) {
        	mainApp.getDataManager().PreprocessDocument();      	
        } else if (source == editTags) {
        	TagEditDialog tagDialog = new TagEditDialog(targetWS.getTags(), targetWS.getName());
        	//showDialog returns the modified list of tags, so set tags to that
        	List<String> val = tagDialog.showDialog();
        	targetWS.setTags(val);
        }else {
        	boolean stop = false;
        	if(Table.allWSProducers()!=null){
	        	for(Object obj : Table.allWSProducers()){
	        		//check if the action is a ws producer
	        		if(source.getActionCommand().equals((String)((Entry)obj).key)){
	        			stop = true;
	        			List<WorkingSet> wsets = (List<WorkingSet>)Table.applyWSProducer(source.getActionCommand(), targetWS);
	                	for(WorkingSet ws : wsets){
	                		mainApp.appendToLog("Created Working Set: "+ws);
		        			mainApp.insertWS(ws);
		                	
	                	}
	        		}
	        	}
        	}
        	//check if the action is a layer producer
        	if(!stop && Table.allLayerProducers()!=null){
	        	for(Object obj : Table.allLayerProducers()){
	        		if(source.getActionCommand().equals((String)((Entry)obj).key)){
	        			stop = true;
	        			mainApp.createAndAddLayers(targetWS, source.getActionCommand());
	        		}
	        	}
	        	mainApp.refreshLayerChecklist();
	        	mainApp.repTree.reload();
        	}
        	if(!stop){
	        	if(mode == SELECTION){
	        		Toys.callSelToy(source.getText(), view.getSelected());
	        	} else if(mode == CURRENT_WS || mode == OTHER_WS){
	        		Toys.callWSToy(source.getText(), targetWS);
        	
	        	}
	        }
        }
    }
    public void itemStateChanged(ItemEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
        String s = "Item event detected."
                   + "\n"
                   + "    Event source: " + source.getText()
                   + " (an instance of " + getClassName(source) + ")"
                   + "\n"
                   + "    New state: "
                   + ((e.getStateChange() == ItemEvent.SELECTED) ?
                     "selected":"unselected");
        mainApp.appendToLog(s + "\n");
    }
	private String getClassName(JMenuItem source) {
		return "DocumentMenu";
	}
}
