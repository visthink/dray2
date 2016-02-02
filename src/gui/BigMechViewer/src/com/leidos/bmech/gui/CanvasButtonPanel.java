package com.leidos.bmech.gui;

import java.awt.Cursor;
import java.awt.FlowLayout;
// import java.awt.Image;
import java.awt.Insets;
// import java.awt.Point;
// import java.awt.Toolkit;
// import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// import javax.imageio.ImageIO;
// import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
// import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
// import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import com.leidos.bmech.model.TypeTag;

//import java.awt.event.ActionListener;
//import java.io.File;
//import java.io.IOException;

@SuppressWarnings("serial")
public class CanvasButtonPanel extends JPanel {
	JButton goBack;
	JButton merge;
	JToggleButton split;
	JToggleButton quickTagToggle;
	@SuppressWarnings("rawtypes")
	JComboBox quickTags; 
	@SuppressWarnings("rawtypes")
	DefaultComboBoxModel dcm;
	List<TypeTag> tags;
	Map<JToggleButton, TypeTag> mapBtnToTag;
	ViewerApp mainApp;
	Cursor scissorCursor;
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CanvasButtonPanel(ViewerApp app){
		super(new FlowLayout(FlowLayout.LEFT));
		this.mainApp = app;
	//	Toolkit tk = Toolkit.getDefaultToolkit();
		/*
		Image image;
		try {
			
			image = ImageIO.read(getClass().getResourceAsStream("/src/resources/scissor.png"));
			scissorCursor = tk.createCustomCursor(image, new Point(0,0), "Scissor");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		mapBtnToTag = new HashMap<JToggleButton, TypeTag>();
		tags =  new ArrayList<TypeTag>();
		tags.add(TypeTag.TABLE);
		tags.add(TypeTag.COLUMN);
		tags.add(TypeTag.ROW);
		tags.add(TypeTag.HEADER_ROW);
		tags.add(TypeTag.HEADER);
		tags.add(TypeTag.CAPTION);
		tags.add(TypeTag.FIGURE);
		tags.add(TypeTag.IGNORE);
		//tags.add(TypeTag.MERGE);
		
		goBack = new JButton("^");
		goBack.addActionListener(mainApp);
		goBack.setActionCommand("go_up");
		goBack.setMargin(new Insets(0,0,0,0));
		add(goBack);
		
		//merge = new JButton("[MERGE]");
		//try {
			
		    /*Image img = ImageIO.read(getClass().getResourceAsStream("src/resources/merge.png"));
		    Image img2 = ImageIO.read(getClass().getResourceAsStream("src/resources/mergepressed.png"));
		    merge = new JButton(new ImageIcon(img));
		    merge.setPressedIcon(new ImageIcon(img2));*/
			merge = new JButton("Merge WS");
		   // merge.setOpaque(false);
		    //merge.setContentAreaFilled(false);
		    //merge.setBorderPainted(false);
		    //merge.setFocusPainted(false);
		//} catch (IOException ex) {
		//	System.out.println("merge.png not found");
		//}
		merge.addActionListener(mainApp);
		merge.setMargin(new Insets(0,0,0,0));
		merge.setActionCommand("merge");
		add(merge);
		//split = new JToggleButton("[SPLIT]");
		//try {
		    //Image img = ImageIO.read(getClass().getResourceAsStream("src/resources/split.png"));
		    //Image img2 = ImageIO.read(getClass().getResourceAsStream("src/resources/splitpressed.png"));
		    //split = new JToggleButton(new ImageIcon(img));
		   // split.setSelectedIcon(new ImageIcon(img2));
			split = new JToggleButton("Split text");
		    //split.setOpaque(false);
		    //split.setContentAreaFilled(false);
		    //split.setBorderPainted(false);
		    //split.setFocusPainted(false);
		    /*split.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent ev) {
					// TODO Auto-generated method stub
					if(ev.getStateChange()==ItemEvent.SELECTED){
						//set mouse pointer 
						mainApp.canvas.setCursor(scissorCursor);
					} else {
						//reset mouse pointer
						mainApp.canvas.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					}
				}
			});*/
		//} catch (IOException ex) {
		//	split = new JToggleButton("[SPLIT]");
		//}
		//split.addActionListener(mainApp);
		//split.setActionCommand("split");
		split.setMargin(new Insets(0,0,0,0));
		add(split);
		
	//	String [] names = new String[TypeTag.values().length];
		for(TypeTag tag : tags){
			JToggleButton tog = new JToggleButton(tag.name());
			tog.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent ev) {
					// TODO Auto-generated method stub
					if(ev.getStateChange()==ItemEvent.SELECTED){
						for(JToggleButton btn : mapBtnToTag.keySet()){
							
							if(!btn.equals(ev.getSource())){
								//unselect all other buttons
								btn.setSelected(false);
								//btn.setEnabled(false);
								
							}
						}
					}
				}
			});
			mapBtnToTag.put( tog, tag);
			tog.setMargin(new Insets(0,0,0,0));
			tog.setEnabled(false);
			add(tog);
		}
		dcm = new DefaultComboBoxModel();
		quickTags = new JComboBox(dcm);
	}
	
	public boolean isQuickTagEnabled(){
		for(JToggleButton btn : mapBtnToTag.keySet()){
			if(btn.isEnabled() && btn.isSelected()){
				return true;
			}
		}
		return false;//quickTagToggle.getModel().isSelected();
	}
	
	public boolean isSplitModeEnabled(){
		if(split.isEnabled() && split.isSelected()){
			return true;
		}
		return false;//quickTagToggle.getModel().isSelected();
	}
	
	public String getTag(){
		for(JToggleButton btn : mapBtnToTag.keySet()){
			if(btn.isSelected()){
				return btn.getText();
			}
		}
		return TypeTag.UNKNOWN.name();
	}

	public void setQuickTags(Set<TypeTag> suggestedTags) {
		for(JToggleButton btn : mapBtnToTag.keySet()){
			btn.setEnabled(suggestedTags.contains(mapBtnToTag.get(btn)));
			if(!btn.isEnabled() && btn.isSelected()){
				btn.setSelected(false);
			}
		}
		
	}


	
	
}
