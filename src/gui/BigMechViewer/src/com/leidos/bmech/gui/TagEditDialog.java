package com.leidos.bmech.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
//import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
//import javax.swing.text.html.HTML.Tag;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

public class TagEditDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2471204031276037497L;
	private final JPanel contentPanel = new JPanel();
	private JTextField textField;
	private final Action addAction = new AddAction();
	@SuppressWarnings("rawtypes")
	private DefaultListModel listModel;
	@SuppressWarnings("rawtypes")
	private JList list;
	private final Action removeAction = new RemoveAction();
	private final Action okAction = new OkAction();
	private final Action cancelAction = new CancelAction();
	private List<String> returnValue;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			TagEditDialog dialog = new TagEditDialog(new ArrayList<String>(), "test");
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			//
			System.out.println(dialog.showDialog());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TagEditDialog(List<String> data, String name) {
		setModal(true);
		//save the list incase the user cancels
		returnValue = data;
		setTitle("Edit tags for " + name);
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			listModel = new DefaultListModel();
			for(String tag : data){
				listModel.addElement(tag);
			}
			list = new JList(listModel);
			contentPanel.add(list, BorderLayout.CENTER);
		}
		{
			JPanel panel = new JPanel();
			contentPanel.add(panel, BorderLayout.NORTH);
			{
				textField = new JTextField();
				panel.add(textField);
				textField.setColumns(10);
			}
			{
				JButton btnAddTag = new JButton("Add");
				btnAddTag.setAction(addAction);
				panel.add(btnAddTag);
				//this button will be pressed when the user hits enter
				getRootPane().setDefaultButton(btnAddTag);
			}
			{
				JButton btnRemoveTag = new JButton("Remove");
				btnRemoveTag.setAction(removeAction);
				panel.add(btnRemoveTag);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setAction(okAction);
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setAction(cancelAction);
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	List<String> showDialog(){
		setVisible(true);
		return returnValue;
	}
	
	private class AddAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1720678939224547324L;
		public AddAction() {
			putValue(NAME, "Add");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}
		@SuppressWarnings("unchecked")
		public void actionPerformed(ActionEvent e) {
			String str = textField.getText().replaceAll("[^a-zA-Z\\s0-9]", "");
			for(String token : str.split("\\s+")){	
				if( !listModel.contains(token.toLowerCase())){
					listModel.addElement(token.toLowerCase());
				}
			}
			textField.setText("");
		}
	}
	private class RemoveAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = -8169215852422710101L;
		public RemoveAction() {
			putValue(NAME, "Remove");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}
		public void actionPerformed(ActionEvent e) {
			for(Object tagObj : list.getSelectedValuesList()){
				listModel.removeElement(tagObj);
			}
		}
	}
	private class OkAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4382439560279118291L;
		public OkAction() {
			putValue(NAME, "OK");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}
		public void actionPerformed(ActionEvent e) {
			//first, prepare the list to be returned
			returnValue = new ArrayList<String>();		
			for(int i=0; i < listModel.getSize(); i++){
				returnValue.add((String) listModel.getElementAt(i));
			}
			//Now exit, returning control to the showDialog() function
			setVisible(false);
			dispose();
		}
	}
	private class CancelAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6578350625229901655L;
		public CancelAction() {
			putValue(NAME, "Cancel");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}
		public void actionPerformed(ActionEvent e) {
			//Now exit, returning control to the showDialog() function
			setVisible(false);
			dispose();
		}
	}
}
