package com.leidos.bmech.gui;

import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.leidos.bmech.model.TypeTag;

@SuppressWarnings("serial")
public class CanvasButtonPanel extends JPanel {
	JButton								goBack;
	JButton								merge;
	JToggleButton						split;
	JToggleButton						quickTagToggle;
	@SuppressWarnings("rawtypes")
	JComboBox							quickTags;
	@SuppressWarnings("rawtypes")
	DefaultComboBoxModel				dcm;
	List<TypeTag>						tags;
	Map<JToggleButton, TypeTag>	mapBtnToTag;
	ViewerApp							mainApp;
	Cursor								scissorCursor;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CanvasButtonPanel(ViewerApp app) {
		super(new FlowLayout(FlowLayout.LEFT));
		this.mainApp = app;
		mapBtnToTag = new HashMap<JToggleButton, TypeTag>();
		tags = new ArrayList<TypeTag>();
		tags.add(TypeTag.TABLE);
		tags.add(TypeTag.COLUMN);
		tags.add(TypeTag.ROW);
		tags.add(TypeTag.HEADER_ROW);
		tags.add(TypeTag.HEADER);
		tags.add(TypeTag.CAPTION);
		tags.add(TypeTag.FIGURE);
		tags.add(TypeTag.IGNORE);

		goBack = new JButton("^");
		goBack.addActionListener(mainApp);
		goBack.setActionCommand("go_up");
		goBack.setMargin(new Insets(0, 0, 0, 0));
		add(goBack);

		merge = new JButton("Merge WS");
		merge.addActionListener(mainApp);
		merge.setMargin(new Insets(0, 0, 0, 0));
		merge.setActionCommand("merge");
		add(merge);

		split = new JToggleButton("Split text");
		split.setMargin(new Insets(0, 0, 0, 0));
		add(split);

		for (TypeTag tag : tags) {
			JToggleButton tog = new JToggleButton(tag.name());
			tog.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent ev) {
					// TODO Auto-generated method stub
					if (ev.getStateChange() == ItemEvent.SELECTED) {
						for (JToggleButton btn : mapBtnToTag.keySet()) {

							if (!btn.equals(ev.getSource())) {
								// unselect all other buttons
								btn.setSelected(false);
								// btn.setEnabled(false);

							}
						}
					}
				}
			});
			mapBtnToTag.put(tog, tag);
			tog.setMargin(new Insets(0, 0, 0, 0));
			tog.setEnabled(false);
			add(tog);
		}
		dcm = new DefaultComboBoxModel();
		quickTags = new JComboBox(dcm);
	}

	public boolean isQuickTagEnabled() {
		for (JToggleButton btn : mapBtnToTag.keySet()) {
			if (btn.isEnabled() && btn.isSelected()) {
				return true;
			}
		}
		return false;// quickTagToggle.getModel().isSelected();
	}

	public boolean isSplitModeEnabled() {
		if (split.isEnabled() && split.isSelected()) {
			return true;
		}
		return false;// quickTagToggle.getModel().isSelected();
	}

	public String getTag() {
		for (JToggleButton btn : mapBtnToTag.keySet()) {
			if (btn.isSelected()) {
				return btn.getText();
			}
		}
		return TypeTag.UNKNOWN.name();
	}

	public void setQuickTags(Set<TypeTag> suggestedTags) {
		for (JToggleButton btn : mapBtnToTag.keySet()) {
			btn.setEnabled(suggestedTags.contains(mapBtnToTag.get(btn)));
			if (!btn.isEnabled() && btn.isSelected()) {
				btn.setSelected(false);
			}
		}

	}

}
