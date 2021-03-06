package com.leidos.bmech.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
//import java.awt.geom.Line2D.Double;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
//import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
//import javax.swing.event.TreeModelEvent;
//import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import java.awt.Toolkit;

import org.apache.commons.io.FilenameUtils;

import com.leidos.bmech.gui.Task.TaskType;
import com.leidos.bmech.model.CommandLineValues;
import com.leidos.bmech.model.DataManager;
import com.leidos.bmech.model.Layer;
import com.leidos.bmech.model.TypeTag;
import com.leidos.bmech.model.WorkingSet;
import com.leidos.bmech.view.DataManagerView;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import dray.j.BoundingBox;
import dray.j.Doc;
import dray.j.Producer.Table;
//import dray.j.Producer.Entry;
import dray.j.VisualElement.El;
// import dray.j.VisualElement.SplittableEl;
import dray.j.VisualElement.VPage;

/**
 * the GUI for interacting with PDF Documents. All interactions with the data
 * are done through the DocumentManager class. Only GUI related things are
 * exposed in this class.
 * 
 * @author powelldan
 *
 */
public class ViewerApp implements Observer, ActionListener {

	public JFrame					frame;
	DocumentCanvas					canvas;
	public CheckBoxList			layerCheckList;

	JTextArea						textArea;
	JList<Object>					pagePickerList;
	private CanvasButtonPanel	canvasButtonPanel;
	private DataManager			dataManager;
	JTree								visualTree;
	WorkingSetJTree				workingSetTree;
	RepresentationJTree			repTree;
	JTabbedPane						tabbedPane;
	JSlider							offsetSliderH;
	JSlider							offsetSliderV;

	private List<Task>			taskHistory;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		final CommandLineValues cmd = new CommandLineValues(args);

		if (cmd.isHelp() || !cmd.isErrorFree()) {

			return;

		} else if (cmd.isBatch()) {

			DataManager dataManager = new DataManager();
			dataManager.commandLine(cmd);

		} else {

			EventQueue.invokeLater(new Runnable() {

				public void run() {

					createViewerAppWindow(cmd);

				}
			});
		}
	}

	@Override // Observer interface
	public void update(Observable o, Object data) {
		viewWSUpdated();
		refreshPageIconList();
		if (getDataManager().getPreprocessState() == 1) {
			appendToLog("Preprocessing document is complete.");
		}
	}

	/**
	 * another function to start the GUI
	 */
	public static ViewerApp startDray(String[] args) {

		final CommandLineValues cmd = new CommandLineValues(args);

		ViewerApp window = null;

		if (!cmd.isErrorFree() || cmd.isHelp()) {

			return null;

		} else if (cmd.isBatch()) {

			// Command Line Mode
			DataManager dataManager = new DataManager();
			dataManager.commandLine(cmd);

		} else {

			window = createViewerAppWindow(cmd);

		}
		return window;
	}

	public static ViewerApp createViewerAppWindow(CommandLineValues cmd) {

		ViewerApp window = new ViewerApp();

		try {
			window.frame.setVisible(true);

			DataManager dm = window.getDataManager();

			if (cmd.hasInput()) {
				File pdfFile = cmd.getFile();

				dm.setPdfFile(pdfFile);
				dm.getView().setCurrentPage(1);
				if (!cmd.skipOverlays()) {
					dm.restoreDefaultOverlay(pdfFile);
				}

				if (cmd.isTableCode()) {

					for (WorkingSet pg : dm.getHeadWorkingSet().getChildren()) {

						for (WorkingSet tableWS : pg.getChildrenWithTag("TABLE")) {

							if (tableWS.isLeaf()) { // If table is empty, run
								// AutoTable.
								tableWS.doAutoTableWS();
							}

							Table.applyLayerProducer("simple-table", tableWS);

						}

					}

				}

				window.refreshPageIconList();
				window.refreshLayerList();
				window.workingSetTree.reload();
				window.reloadRepTree();
				window.frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				window.viewWSUpdated();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return window;
	}

	/**
	 * Create the application.
	 */
	public ViewerApp() {
		dataManager = new DataManager();
		dataManager.getView().setGui(this);
		dataManager.addObserver(this);
		// dataManager.initializeData();
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initialize() {
		taskHistory = new ArrayList<Task>();
		IFn require = Clojure.var("clojure.core", "require");
		require.invoke(Clojure.read("dray.core"));

		IFn populateFn = Clojure.var("dray.core", "populate-gui-tables");
		// IFn populateFn = Clojure.var("clojure.core", "+");
		populateFn.invoke();
		frame = new JFrame();
		frame.setTitle("Document Viewer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		/* MENU BAR */
		initMenuBar();

		canvas = new DocumentCanvas(this);
		JPanel canvasPanel = new JPanel(new BorderLayout());

		JScrollPane canvasScrollPane = new JScrollPane(canvas);
		canvasButtonPanel = new CanvasButtonPanel(this);
		JScrollPane canvasButtonPanelScroll = new JScrollPane();
		canvasButtonPanelScroll.setViewportView(canvasButtonPanel);
		canvasButtonPanelScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		canvasPanel.add(canvasButtonPanelScroll, BorderLayout.NORTH);
		canvasPanel.add(canvasScrollPane, BorderLayout.CENTER);
		JSplitPane mainSplitPane = new JSplitPane();
		mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

		tabbedPane = new JTabbedPane();
		tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent changeEvent) {
				canvas.repaint();
			}
		});

		DefaultListModel<JCheckBox> model = new DefaultListModel<JCheckBox>();
		layerCheckList = new CheckBoxList(model);
		layerCheckList.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int index = layerCheckList.locationToIndex(e.getPoint());
				if (index != -1) {
					JCheckBox checkbox = (JCheckBox) layerCheckList.getModel().getElementAt(index);
					if (e.getClickCount() >= 1) {
						dataManager.getLayerList().getLayerByName(checkbox.getText()).setHighlight(checkbox.isSelected());
						canvas.repaint();
					}

				}
			}
			/*
			 * public void mouseExited(MouseEvent e) { for (Layer layer :
			 * dataManager.getLayerList().values()){
			 * //layer.setShouldHighlight(false); canvas.repaint(); } currentIndex
			 * = -1; }
			 */
		});
		/*
		 * 
		 * layerCheckList.addMouseMotionListener(new MouseAdapter() { public void
		 * mouseMoved(MouseEvent e) { int index =
		 * layerCheckList.locationToIndex(e.getPoint()); if (index !=
		 * currentIndex){ //dataManager.getLayerList().getLayerByIndex(index).
		 * setShouldHighlight( true); canvas.repaint(); currentIndex = index; } }
		 * });
		 */
		/*
		 * for (Layer layer : dataManager.getLayerList().asList()){ JCheckBox tmp
		 * = new JCheckBox(layer.getName()); tmp.setSelected(true);
		 * model.addElement(tmp); System.out.println(layer.getName() + "/" +
		 * dataManager.getLayerList().asList().size()); }
		 */
		// JCheckBox test = model.elementAt(0);
		// tabbedPane.addTab("Elements", list);
		JSplitPane exploreSplitPane = new JSplitPane();

		exploreSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		// refreshLayerChecklist();

		/* page picker */
		pagePickerList = new JList();
		pagePickerList.setCellRenderer(new ImageListRenderer());
		pagePickerList.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int index = pagePickerList.getSelectedIndex();
				if (index != -1) {
					dataManager.getView().setCurrentPage(index + 1);
					viewWSUpdated();

				}

			}
		});

		JPanel pagePickerPanel = new JPanel(new BorderLayout());
		JScrollPane pagePickerScrollPane = new JScrollPane(pagePickerList);

		/* VISUAL TREE */
		visualTree = new JTree(new DefaultMutableTreeNode("No Document Loaded"));
		// refreshLayerList();
		JScrollPane treeView = new JScrollPane(visualTree);
		visualTree.setRootVisible(false);
		visualTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				getView().getSelected().clear();
				TreePath[] selectedNodes = visualTree.getSelectionPaths();
				if (selectedNodes == null)
					return;
				for (TreePath path : selectedNodes) {
					if (((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject() instanceof El)
						getView().getSelected()
								.add((El) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
				}
				canvas.repaint();

			}
		});

		/* REP TREE */
		repTree = new RepresentationJTree(this);
		// refreshLayerList();
		JScrollPane repView = new JScrollPane(repTree);
		repTree.setRootVisible(false);

		/* TEXT AREA */
		textArea = new JTextArea();
		textArea.setRows(6);
		JScrollPane textScrollPane = new JScrollPane(textArea);

		/* WORKING SET TREE */
		workingSetTree = new WorkingSetJTree(this);
		// refreshWorkingSetTree();
		JScrollPane wsView = new JScrollPane(workingSetTree);

		/* OPTIONS */
		JPanel optionsPanel = new JPanel();
		offsetSliderH = new JSlider(JSlider.HORIZONTAL, -30, 30, 0);
		offsetSliderH.setPaintTicks(true);
		offsetSliderH.setPaintLabels(true);
		offsetSliderH.setMinorTickSpacing(1);
		offsetSliderH.setMajorTickSpacing(10);
		offsetSliderH.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				int value = offsetSliderH.getValue();
				dataManager.setOffsetX(value);
				canvas.repaint();
			}
		});
		offsetSliderV = new JSlider(JSlider.VERTICAL, -30, 30, 0);
		offsetSliderV.setPaintTicks(true);
		offsetSliderV.setPaintLabels(true);
		offsetSliderV.setMinorTickSpacing(1);
		offsetSliderV.setMajorTickSpacing(10);
		offsetSliderV.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				int value = offsetSliderV.getValue();
				dataManager.setOffsetY(value);
				canvas.repaint();
			}
		});

		// Add things
		// mainSplitPane.add(canvasScrollPane, JSplitPane.RIGHT);
		mainSplitPane.add(canvasPanel, JSplitPane.RIGHT);
		optionsPanel.add(offsetSliderH);
		optionsPanel.add(offsetSliderV);
		mainSplitPane.add(tabbedPane, JSplitPane.LEFT);
		exploreSplitPane.add(layerCheckList, JSplitPane.BOTTOM);
		pagePickerPanel.add(pagePickerScrollPane, BorderLayout.CENTER);
		frame.getContentPane().add(mainSplitPane, BorderLayout.CENTER);
		frame.getContentPane().add(textScrollPane, BorderLayout.SOUTH);
		exploreSplitPane.add(wsView, JSplitPane.TOP);
		tabbedPane.addTab("Explore", exploreSplitPane);
		tabbedPane.addTab("Pages", pagePickerPanel);
		tabbedPane.addTab("Details", treeView);
		// tabbedPane.addTab("Options", optionsPanel);
		tabbedPane.addTab("Rep", repView);

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
				if ((e.getKeyCode() == KeyEvent.VK_Z) && (e.getID() == KeyEvent.KEY_PRESSED)
						&& ((e.getModifiers() & ctrl) == ctrl)) {

					undoTask();
				}
				return false;
			}
		});

		/*
		 * dataManager.setPdfFile(new
		 * File("resources/corpora/wnt-pv/008_11_pv.pdf"));
		 * workingSetTree.reload(); refreshPageIconList();
		 * this.refreshLayerList(); this.refreshLayerChecklist();
		 * dataManager.getView().setCurrentWS(dataManager.getPageWS(1));
		 * repTree.reload(); viewWSUpdated();
		 */
		if (dataManager.getVDocument() != null) {
			List pages = (List) dataManager.getVDocument().getItems();
			VPage page = (VPage) pages.get(0);// dataManager.getCurrentPage());
			BoundingBox pagebb = (BoundingBox) page.getBbox();
			canvas.setPreferredSize(new Dimension((int) pagebb.getWidth(), (int) pagebb.getHeight()));
			canvasScrollPane.setPreferredSize(new Dimension((int) pagebb.getWidth(), (int) pagebb.getHeight()));
			canvasScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			canvasScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
			// canvasButtonPanel.setPreferredSize(new
			// Dimension((int)pagebb.getWidth(), 42));
			canvasButtonPanelScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
		} else {
			canvasScrollPane.setPreferredSize(new Dimension(640, 640));
			canvasButtonPanelScroll.setPreferredSize(new Dimension(640, 60));

		}
		frame.pack();
	}

	private void initMenuBar() {
		JMenuBar menubar = new JMenuBar();
		JMenu file = new JMenu("File");

		// LOAD PDF OR OVERLAY
		JMenuItem fMenuItem = new JMenuItem("Load PDF or Overlay...");
		fMenuItem.setToolTipText("Load a PDF file from disk");
		fMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Handle open button action.
				String fileBrowseStart;
				if (dataManager.getPdfFile() == null)
					fileBrowseStart = "resources/corpora/";
				else
					fileBrowseStart = dataManager.getPdfFile().getParent();
				JFileChooser fc = new JFileChooser(fileBrowseStart) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 3381557651546148634L;

					@Override
					public void approveSelection() {
						// confirmation when overwriting unsaved changes
						if (getDataManager().getPdfFile() != null && getDialogType() == OPEN_DIALOG) {
							int result = JOptionPane.showConfirmDialog(this, "Any unsaved work will be lost. Continue?",
									"Opening new file", JOptionPane.YES_NO_CANCEL_OPTION);
							switch (result) {
								case JOptionPane.YES_OPTION:
									super.approveSelection();
									return;
								case JOptionPane.NO_OPTION:
									return;
								case JOptionPane.CLOSED_OPTION:
									return;
								case JOptionPane.CANCEL_OPTION:
									cancelSelection();
									return;
							}
						}
						super.approveSelection();
					}
				};
				FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF Files or Working Set JSON files", "pdf",
						"json");
				fc.setFileFilter(filter);
				int returnVal = fc.showOpenDialog(null);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					offsetSliderH.setValue(0);
					offsetSliderV.setValue(0);
					dataManager.setOffsetX(0);
					dataManager.setOffsetY(0);
					frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
					if (FilenameUtils.getExtension(fc.getSelectedFile().getName()).equalsIgnoreCase("pdf")) {
						getDataManager().setPdfFile(fc.getSelectedFile());
					} else if (FilenameUtils.getExtension(fc.getSelectedFile().getName()).equalsIgnoreCase("json")) {
						System.out.println("Loading pdf and working sets from json file" + fc.getSelectedFile());
						Doc.restoreWSfromOverlay(getDataManager(), fc.getSelectedFile());
					}
					dataManager.getView().setCurrentPage(1);
					refreshPageIconList();
					refreshLayerList();
					workingSetTree.reload();
					reloadRepTree();
					frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					viewWSUpdated();
					System.out.println("Opening: " + dataManager.getPdfFile().getName() + ".");
				} else {
					System.out.println("Open command cancelled by user.");
				}
			}
		});

		// SAVE WORKING SET OVERLAY
		JMenuItem saveMenuItem = new JMenuItem("Save current working sets");
		fMenuItem.setToolTipText("Save this document's working sets to disk as a JSON file");
		saveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Handle open button action.
				@SuppressWarnings("serial")
				JFileChooser fc = new JFileChooser(dataManager.getPdfFile().getParent()) {
					@Override
					public void approveSelection() {
						File f = getSelectedFile();
						if (f.exists() && getDialogType() == SAVE_DIALOG) {
							int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file",
									JOptionPane.YES_NO_CANCEL_OPTION);
							switch (result) {
								case JOptionPane.YES_OPTION:
									super.approveSelection();
									return;
								case JOptionPane.NO_OPTION:
									return;
								case JOptionPane.CLOSED_OPTION:
									return;
								case JOptionPane.CANCEL_OPTION:
									cancelSelection();
									return;
							}
						}
						super.approveSelection();
					}
				};
				// try to figure out the pdf file name and change
				// it to json for convenient naming
				String path = "";
				try {
					path = dataManager.getPdfFile().getCanonicalPath();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				path = path.substring(0, path.lastIndexOf("."));
				path += ".json";
				fc.setSelectedFile(new File(path));
				FileNameExtensionFilter filter = new FileNameExtensionFilter("WS json files", "json");
				fc.setFileFilter(filter);

				int returnVal = fc.showSaveDialog(null);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					Doc.saveWStoOverlay(getDataManager(), fc.getSelectedFile());
					appendToLog("Saving current working sets to " + fc.getSelectedFile());
				} else {
					System.out.println("Open command cancelled by user.");
				}
			}
		});
		JMenuItem eMenuItem = new JMenuItem("Exit");
		eMenuItem.setToolTipText("Exit application");
		eMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});
		file.add(fMenuItem);
		file.add(saveMenuItem);
		file.add(eMenuItem);
		menubar.add(file);
		frame.setJMenuBar(menubar);

	}

	/**
	 * Appends a string to the log at the bottom of the GUI
	 * 
	 * @param string
	 */
	public void appendToLog(String string) {
		textArea.setText(textArea.getText() + "\n" + string);
	}

	/**
	 * Triggers the reloading of the page icons into the page icon list the pages
	 * should already be in memory
	 */
	private void refreshPageIconList() {
		List<JPanel> pageArray = new ArrayList<JPanel>();

		for (int i = 0; i < dataManager.getPageIconList().length; i++) {
			JPanel pagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			pagePanel.add(new JLabel("" + (i + 1), new ImageIcon(dataManager.getPageIcon(i + 1)), JLabel.CENTER));
			pageArray.add(pagePanel);
		}

		pagePickerList.setListData(pageArray.toArray());
	}

	public void viewWSUpdated() {
		refreshLayerList();
		refreshLayerChecklist();
		this.canvasButtonPanel.setQuickTags(WorkingSet.getSuggestedTags(this.getView().getCurrentWS().getTags()));
		this.repTree.reload();
		// wsChangedAlready = true;
		workingSetTree.refresh();
		// wsChangedAlready = false;
		frame.setTitle(dataManager.getPdfFile().getName() + " - " + getView().getCurrentWS().getName());
		// refreshLayerList();
		// refreshLayerChecklist();
		// frame.pack();
		canvas.repaint();
	}

	/**
	 * internal class for the page icon list
	 * 
	 * @author powelldan
	 *
	 */
	@SuppressWarnings("serial")
	public class ImageListRenderer extends DefaultListCellRenderer {

		Font font = new Font("helvitica", Font.ITALIC, 14);

		@Override
		public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			Component component = (Component) value;
			// component.setForeground(Color.white);
			// component.setBackground (isSelected ? Color.gray :
			// Color.lightGray);

			return component;
		}
	}

	public void refreshLayerChecklist() {
		DefaultListModel<JCheckBox> model = (DefaultListModel<JCheckBox>) layerCheckList.getModel();
		model.clear();
		for (Layer layer : dataManager.getLayerList().values()) {
			JCheckBox tmp = new JCheckBox(layer.getName());
			tmp.setSelected(layer.isHighlight());
			model.addElement(tmp);
			tmp.setBackground(UtiliBuddy.makeTransparent(layer.getColor(), 0.1f));
		}
	}

	/**
	 * Populates the tree in the tabbed pane that represents the layers in the
	 * document
	 */
	public void refreshLayerList() {

		DefaultMutableTreeNode top = new DefaultMutableTreeNode("WorkingSet");
		DefaultTreeModel model = new DefaultTreeModel(top);

		for (Layer layer : getView().getCurrentWS().getLayerList().values()) {

			DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(layer.getName());
			top.add(subNode);

			for (El el : layer.getItems()) {
				DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(el);
				subNode.add(itemNode);
			}
		}

		visualTree.setModel(model);

	}

	/**
	 * Notify the datamanager that a rectangle has been drawn on the canvas.
	 * Called by the DocumentCanvas object
	 * 
	 * @param dragRectDescaled
	 *           the rectangle that was drawn on the canvas
	 */
	public void rectangleDrawn(Rectangle2D dragRectDescaled) {

		List<El> els = dataManager.getElsIn(this.getView().getCurrentPage(), dragRectDescaled);// getView().getCurrentWS().getElsIn(dragRectDescaled);
		if (!els.isEmpty()) {

			getView().getSelected().clear();
			getView().getSelected().addAll(els);
			selectedChanged();
			if (isQuickTagEnabled()) {
				insertSelectedAsWS(getQuickTag());
			}
			canvas.repaint();
		}
	}

	public void rectangleDrawn(WorkingSet ws, Rectangle rect) {
		// TODO Auto-generated method stub
		ws.resize(rect);
		selectedChanged();
		this.workingSetTree.reload();
		viewWSUpdated();
		canvas.repaint();
	}

	public void deleteCurrentWS() {

		DefaultTreeModel model = (DefaultTreeModel) workingSetTree.getModel();
		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) workingSetTree.getLastSelectedPathComponent();
		if (selectedNode.getParent() != null) {
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
			// cant delete page Working Sets (aka 1st level below root)
			if (!parent.isRoot()) {
				getDataManager().deleteCurrentWS();
				workingSetTree.setSelectionPath(new TreePath(parent.getPath()));
				model.removeNodeFromParent(selectedNode);
			}
		}
	}

	public DataManager getDataManager() {
		return dataManager;
	}

	public JFrame getFrame() {
		return frame;
	}

	public void deleteWS(WorkingSet victim, boolean addToHistory) {
		getDataManager().deleteWS(victim);
		workingSetTree.deleteWS(victim);
		viewWSUpdated();
		if (addToHistory) {
			taskHistory.add(new Task(TaskType.DEL_WS, victim));
		}
	}

	public void selectedChanged() {
		List<TreePath> paths = new ArrayList<TreePath>();

		for (@SuppressWarnings("rawtypes")
		Enumeration e = ((DefaultMutableTreeNode) visualTree.getModel().getRoot()).depthFirstEnumeration(); e
				.hasMoreElements();) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			if (getView().getSelected().contains(node.getUserObject())) {
				paths.add(new TreePath(node.getPath()));
			}
		}
		if (paths.size() > 0) {
			visualTree.setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
			visualTree.scrollPathToVisible(paths.get(0));
		}

	}

	public void insertSelectedAsWS(TypeTag tag) {
		insertSelectedAsWS(tag.name());
	}

	public void insertSelectedAsWS(String tag) {
		WorkingSet added = getDataManager().createWSFromSel();
		appendToLog("Creating new working set with tag: " + tag);

		// added.setName(tag +
		// (dataManager.getView().getCurrentWS().getChildrenWithTag(tag).size()+1));
		added.setName(tag + " " + added.getItems().get(0).getText().toString().concat("      ").substring(0, 6).trim());
		added.addTag(tag.toLowerCase());
		insertWS(added);
		getView().getCurrentWS().normalize();
		taskHistory.add(new Task(TaskType.ADD_WS, added));
		// getView().setCurrentWS(added);

		// viewWSUpdated();
		workingSetTree.reload();
		this.viewWSUpdated();
		reloadRepTree();
	}

	public void insertWS(WorkingSet addedWS) {
		WorkingSet currentWS = getView().getCurrentWS();
		workingSetTree.insertWorkingSetNode(currentWS, addedWS);
	}

	public DataManagerView getView() {
		return dataManager.getView();
	}

	public WorkingSetJTree getWSTree() {
		return workingSetTree;
	}

	public void reloadRepTree() {
		repTree.reload();
	}

	public void createAndAddLayers(final WorkingSet ws, final String command) {

		SwingWorker<List<Layer>, Void> worker = new SwingWorker<List<Layer>, Void>() {
			@SuppressWarnings("unchecked")
			@Override
			protected List<Layer> doInBackground() throws Exception {
				return (List<Layer>) Table.applyLayerProducer(command, ws);
			}

			// Can safely update the GUI from this method.
			protected void done() {

				try {
					// Retrieve the return value of doInBackground.
					List<Layer> lsets = get();
					for (Layer layer : lsets) {
						// mainApp.appendToLog("Created Layer: "+layer);
						ws.getLayerList().addLayer(layer);
					}
					refreshLayerChecklist();
					reloadRepTree();
				} catch (InterruptedException e) {
					// This is thrown if the thread's interrupted.
				} catch (ExecutionException e) {
					// This is thrown if we throw an exception
					// from doInBackground.
				}
			}

		};

		worker.execute();
	}

	public void processGenes(Set<String> genes) {
		appendToLog("Found genes:");
		for (String gene : genes) {
			appendToLog(gene);
		}

	}

	public void AnalyzeEvidence() {
		appendToLog("Analyzing evidence.");
		dataManager.AnalyzeEvidence();
		reloadRepTree();

	}

	public boolean isQuickTagEnabled() {
		return canvasButtonPanel.isQuickTagEnabled();
	}

	public String getQuickTag() {
		return canvasButtonPanel.getTag();
	}

	private void undoTask() {
		if (taskHistory.size() == 0)
			return;
		Task undo = taskHistory.remove(taskHistory.size() - 1);
		appendToLog("Undo task " + undo);
		switch (undo.getTaskType()) {
			case ADD_WS:
				this.deleteWS((WorkingSet) undo.getTarget(), false);
				break;
			case DEL_WS:
				WorkingSet readd = ((WorkingSet) undo.getTarget());
				readd.getParent().mergeChild(readd);
				break;
			default:
				appendToLog("Could not undo task " + undo);
		}

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		if ("go_up".equals(arg0.getActionCommand())) {
			getView().setCurrentWS(getView().getCurrentWS().getParent());
			viewWSUpdated();
		} else if ("merge".equals(arg0.getActionCommand())) {
			WorkingSet merged = getDataManager().mergeSelection();
			workingSetTree.reload();
			getView().getSelected().clear();
			getView().getSelected().add(merged);
			viewWSUpdated();
		}
	}

	public void doAutoTable() {

		WorkingSet currentWS = dataManager.getView().getCurrentWS();

		List<WorkingSet> newWorkingSets = currentWS.doAutoTableWS();

		for (WorkingSet newWS : newWorkingSets) {
			insertWS(newWS);
			// taskHistory.add(new Task(TaskType.ADD_WS, colWS));
		}

		workingSetTree.reload();
		this.viewWSUpdated();
		reloadRepTree();

	}

	public boolean isSplitModeEnabled() {
		// TODO Auto-generated method stub
		return canvasButtonPanel.isSplitModeEnabled();
	}

	public void splitEl(double x, double y, double h) {

	}

	public void createSeparator(Line2D line) {
		// TODO Auto-generated method stub
		getDataManager().addSeparator(getView().getCurrentPage(), line);
		workingSetTree.reload();
		viewWSUpdated();
		// visualTree
	}

}
