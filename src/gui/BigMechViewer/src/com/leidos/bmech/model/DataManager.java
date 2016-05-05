package com.leidos.bmech.model;

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import com.leidos.bmech.analysis.EvidenceGatherer;
import com.leidos.bmech.view.DataManagerView;

import dray.j.BoundingBox;
import dray.j.Doc;
import dray.j.Producer.Table;
import dray.j.VisualElement.El;
import dray.j.VisualElement.VDocument;
import dray.j.VisualElement.VImage;
import dray.j.VisualElement.VPage;
import dray.j.VisualElement.VTable;
import dray.j.VisualElement.VText;

/**
 * The DataManager manages all of the data for a single PDF file.
 * 
 * @author powelldan
 *
 */
public class DataManager extends Observable {

	private File pdfFile;
	private VDocument vDocument;
	private BufferedImage[] pageIconList;
	private BufferedImage[] pageImageList;
	private int[] pageImageStatus;
	private int offsetX;
	private int offsetY;
	private WorkingSet headWS;
	private Map<String, Rectangle2D> imageBBMap;
	private DataManagerView view;
	BufferedImage defaultImage;
	EvidenceGatherer eg;
	int preprocessState; // -1: not started; 0:
							// underway; 1: done
	ArrayList<?> defaultProducers;

	public DataManager() {
		super();
		pageIconList = new BufferedImage[0];
		pageImageList = new BufferedImage[0];
		pageImageStatus = new int[0];
		view = new DataManagerView(this);
		imageBBMap = new HashMap<String, Rectangle2D>();
		defaultImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		eg = new EvidenceGatherer();
		preprocessState = -1;// not started
	}

	/**
	 * Initialize DataManager with data from a VDocument object.
	 * 
	 * @param doc
	 */
	public DataManager(VDocument doc) {
		this();
		loadFromVDocument(doc);
	}

	/**
	 * Initialize Datamanager from PDF file, but don't load images.
	 * 
	 * @param pdf
	 *            The PDF file to load
	 */
	public DataManager(File pdf) {
		this();
		this.pdfFile = pdf;
		loadFromPdf(true);
	}

	/**
	 * Initialize DataManager from PDF file. Internal vdocument will be loaded
	 * by the constructor. Load images for GUI display.
	 * 
	 * @param pdf
	 * @param lazyLoadImages
	 *            false if should load images, true if should not load images
	 */
	public DataManager(File pdf, boolean lazyLoadImages) {
		this();
		this.pdfFile = pdf;
		loadFromPdf(lazyLoadImages);
	}

	public void setView(DataManagerView v) {
		view = v;
	}

	public void loadFromVDocument(VDocument doc) {

		vDocument = doc;
		headWS = new WorkingSet(null, "document");
		headWS.setFilename((String) doc.getFilename());
		view.setCurrentWS(headWS);
		@SuppressWarnings("rawtypes")
		List vPages = (List) vDocument.getItems();
		pageIconList = new BufferedImage[vPages.size()];

		for (int i = 0; i < vPages.size(); i++) {
			createPageWS(i);
		}
		setPreprocessState(-1);

	}

	/**
	 * load the page images from pdf and load the vdocument data from a closure
	 * call. param lazyLoadImages -- set to true if images should only be loaded
	 * when they are used
	 */
	public void loadFromPdf(boolean lazyLoadImages) {

		setPreprocessState(-1);

		if (pdfFile == null) {
			System.err.println("ERROR: PDF file pointer is null");
			return;
		}

		if (!pdfFile.exists()) {
			System.err.println("ERROR: PDF file " + pdfFile.toString() + " doesnt exist.");
			return;
		}

		// System.out.println(pdfFile);
		// Call Clojure to populate the VDocument

		vDocument = (VDocument) Doc.getVDocument(pdfFile);

		headWS = new WorkingSet(null, "document");
		headWS.setFilename(replaceFilenameBackslashes(pdfFile)); // For Windows.
		view.setCurrentWS(headWS);

		@SuppressWarnings("rawtypes")
		List vPages = (List) vDocument.getItems();
		pageImageList = new BufferedImage[vPages.size()];
		pageIconList = new BufferedImage[vPages.size()];
		pageImageStatus = new int[vPages.size()];
		for (int i = 0; i < vPages.size(); i++) {
			pageImageStatus[i] = -1;
			pageImageList[i] = defaultImage;
			pageIconList[i] = defaultImage;
		}

		for (int i = 0; i < vPages.size(); i++) {
			// System.out.println("Creating page ws " + i);
			createPageWS(i);
		}
		// System.out.println("Done creating pages");

	}

	// Helper method - Replace backslashes for Windows filenames.
	static private String replaceFilenameBackslashes(File myfile) {
		String newFilename = "";
		try {
			newFilename = myfile.getCanonicalPath().replace('\\', '/');
		} catch (IOException e1) {
			System.out.println("Attempted but failed to replace backslashes with slashes for " + myfile.toString());
			e1.printStackTrace();
		}
		return newFilename;
	}

	private void createPageWS(int pageIndex) {
		@SuppressWarnings("rawtypes")
		List vPages = (List) vDocument.getItems();
		VPage vPage = (VPage) vPages.get(pageIndex);
		// Go through and add elements to the Working Set
		WorkingSet pageWS = headWS.createChild("page" + (pageIndex + 1));
		@SuppressWarnings("rawtypes")
		List items = (List) vPage.getItems();
		for (int j = 0; j < items.size(); j++) {
			if (items.get(j) instanceof dray.j.VisualElement.El) {
				pageWS.addItem((El) items.get(j));
				El el = (El) items.get(j);
				if (el instanceof VText) {
					pageWS.getLayerList().addElementToLayer("text", el);
				} else if (el instanceof VImage) {
					pageWS.getLayerList().addElementToLayer("images", el);
					VImage image = (VImage) el;
					File path = new File((String) image.bitmap_path);
					imageBBMap.put(path.getName(), (BoundingBox) image.getBbox());
				}

			}
		}
		// pageWS.setImage(pageImage);
		pageWS.setPage(pageIndex + 1);
	}

	public LayerList getLayerList() {
		if (view.getCurrentWS() == null)
			return null;
		return view.getCurrentWS().getLayerList();
	}

	public File getPdfFile() {
		return pdfFile;
	}

	/**
	 * called from the GUI, sets the File to the pdf and calls the loadFromPdf()
	 * to get the data.
	 * 
	 * @param pdfFile
	 */
	public void setPdfFile(File pdfFile, boolean lazyLoadImages) {
		this.pdfFile = pdfFile;
		loadFromPdf(lazyLoadImages);
	}

	public void setPdfFile(File pdfFile) {
		setPdfFile(pdfFile, false);
	}

	public BufferedImage[] getPageIconList() {
		return pageIconList;
	}

	public void setPageIconList(BufferedImage[] pageIconList) {
		this.pageIconList = pageIconList;
	}

	@SuppressWarnings("unused")
	private void runBEE() {
		/*
		 * String dataSubDir =
		 * FilenameUtils.removeExtension(pdfFile.getAbsolutePath()) +
		 * ".xml_data"; System.out.println(dataSubDir);
		 */
	}

	/**
	 * Get a reference to the dray.j.VDocument instance currently being used by
	 * the GUI
	 * 
	 * @return the current dray.j.VDocument instance
	 */
	public VDocument getVDocument() {
		return vDocument;
	}

	public WorkingSet getHeadWorkingSet() {
		return headWS;
	}

	/**
	 * Get the first VText item in the VDocument at (x, y)
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public El getElAt(int x, int y) {

		for (El el : view.getCurrentWS().getItems()) {
			BoundingBox bb = (BoundingBox) el.getBbox();
			if (x >= bb.getMinX() && x <= bb.getMaxX() && y >= bb.getMinY() && y <= bb.getMaxY()) {
				return el;
			}
		}
		return null;
	}

	public El getElAt(Point p) {
		return this.getElAt(p.x, p.y);
	}

	/**
	 * Return a list of working sets that contain the given x,y Useful for
	 * detecting mouse events
	 * 
	 * @param x
	 * @param y
	 * @return the list of working sets
	 */
	public List<WorkingSet> getWSAt(int x, int y) {
		List<WorkingSet> ret = new ArrayList<WorkingSet>();
		for (WorkingSet child : view.getCurrentWS().getChildren()) {
			Rectangle bb = (Rectangle) child.getBboxWide();
			if (x >= bb.getMinX() && x <= bb.getMaxX() && y >= bb.getMinY() && y <= bb.getMaxY()) {
				ret.add(child);
			}
		}
		return ret;
	}

	public List<WorkingSet> getWSAt(Point p) {
		return this.getWSAt(p.x, p.y);
	}

	/**
	 * Return a list of working sets that have an edge at the given x,y Useful
	 * for detecting resize events
	 * 
	 * @param x
	 * @param y
	 * @return the list of working sets
	 */

	public List<WorkingSet> getWSEdgeAt(int x, int y) {
		return this.getWSAt(new Point(x, y));
	}

	public List<WorkingSet> getWSEdgeAt(Point pt) {
		// Point pt = new Point(x, y);
		List<WorkingSet> ret = new ArrayList<WorkingSet>();
		for (WorkingSet child : view.getCurrentWS().getChildren()) {
			Rectangle bbw = (Rectangle) child.getBboxWide();
			Rectangle bbs = (Rectangle) child.getBboxSmall();
			if (bbw.contains(pt) && !bbs.contains(pt)) {
				ret.add(child);
			}
		}
		return ret;
	}

	/**
	 * get a list of dray.j.VisualElement.El objects that are FULLY inside of
	 * the rectangle
	 * 
	 * @param dragRectDescaled
	 *            the rectangle
	 * @return
	 * 
	 */

	@SuppressWarnings("unchecked")
	public List<El> getElsIn(int page, Rectangle2D dragRectDescaled) {

		List<El> ret = new ArrayList<El>();
		VPage pg = ((List<VPage>) this.vDocument.getItems()).get(page - 1);
		List<El> els = (List<El>) pg.getItems();
		for (El el : els) {
			BoundingBox bb = (BoundingBox) el.getBbox();
			if (dragRectDescaled.contains(new Rectangle(bb.getBounds()))) {
				ret.add(el);
			}
		}
		return ret;
	}

	public int getOffsetX() {
		return offsetX;
	}

	public void setOffsetX(int offsetX) {
		this.offsetX = offsetX;
	}

	public int getOffsetY() {
		return offsetY;
	}

	public void setOffsetY(int offsetY) {
		this.offsetY = offsetY;
	}

	/*
	 * public List<El> getSelected(){ return selectedEls; }
	 */
	/**
	 * creates a new working set from the selected Visual Elements the new
	 * working set is a child of the current working set
	 * 
	 * @return the newly created working set
	 */
	public WorkingSet createWSFromSel() {
		WorkingSet newWS = view.getCurrentWS().createChild();
		for (Object el : view.getSelected()) {
			if (el instanceof El)
				newWS.addItem((El) el);
		}
		return newWS;

	}

	/**
	 * Delete the current working set. also sets the current working set to be
	 * the parent of the old working set
	 */
	public void deleteCurrentWS() {
		WorkingSet oldWS = view.getCurrentWS();
		view.setCurrentWS(oldWS.getParent());
		view.getCurrentWS().getChildren().remove(oldWS);
	}

	/**
	 * Deletes the given working set and all of its children from the WorkingSet
	 * tree. If the current working set would be deleted, then the current
	 * working set switches to the parent of the deleted node.
	 * 
	 * @param victim
	 *            -- the working set to be deleted
	 */
	public void deleteWS(WorkingSet victim) {
		// check if null
		if (victim == null)
			return;
		// check if we're trying to delete a page
		if (victim.getParent() == headWS)
			return;
		// check if we're deleting something above the current workingset
		if (view.getCurrentWS() == victim || view.getCurrentWS().hasAncestor(victim)) {
			view.setCurrentWS(victim.getParent());
			view.getCurrentWS().getChildren().remove(victim);
		} else { // just delete it
			victim.getParent().getChildren().remove(victim);
		}
	}

	public Rectangle2D getBBFromImg(String filename) {
		return this.imageBBMap.get(filename);
	}

	/**
	 * Get access to the DataManagerView class associated with this instance of
	 * DataManager
	 * 
	 * @return DataManagerView
	 */
	public DataManagerView getView() {
		return view;
	}

	public WorkingSet getPageWS(int page) {
		if (page < 1 || page > getHeadWorkingSet().getChildren().size()) {
			return null;
		}
		return getHeadWorkingSet().getChildren().get(page - 1);
	}

	public BufferedImage getPageImage(int pageNumber) {
		if (pageNumber > pageImageList.length || pageNumber < 1) {
			System.out
					.println("Can't get page " + pageNumber + ". Must be between 1 and " + pageImageList.length + ".");
			return null;
		}
		if (pageImageStatus[pageNumber - 1] == -1) {
			try {
				loadImage(pageNumber - 1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return defaultImage;
		} else if (pageImageStatus[pageNumber - 1] == 0) {
			return defaultImage;
		} else
			return pageImageList[pageNumber - 1];
	}

	public BufferedImage getPageIcon(int pageNumber) {
		if (pageNumber > pageIconList.length || pageNumber < 1) {
			System.out.println("Can't get icon " + pageNumber + ". Must be between 1 and " + pageIconList.length + ".");
			return null;
		}
		if (pageImageStatus[pageNumber - 1] == -1) {
			try {
				loadImage(pageNumber - 1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return defaultImage;
		} else if (pageImageStatus[pageNumber - 1] == 0) {
			return defaultImage;
		} else
			return pageIconList[pageNumber - 1];
	}

	private void loadImage(final int pageNumber) throws IOException {
		pageImageStatus[pageNumber] = 0; // loading

		System.out.println("Entering loadImage for page number " + pageNumber);

		SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {

			@Override
			protected BufferedImage doInBackground() throws Exception {
				if (Doc.useGhostscript()) {
					System.out.println("Using Ghostscript");
					return ImageIO.read(Doc.getPageImageFor(getPdfFile(), pageNumber + 1));
				} else {
					PDDocument document = PDDocument.load(getPdfFile());
					PDFRenderer renderer = new PDFRenderer(document);
					return renderer.renderImageWithDPI(pageNumber, 600);
				}
			}

			// Can safely update the GUI from this method.
			protected void done() {
				// System.out.println("Calling DataManager.done() for page " +
				// pageNumber);
				// System.out.println("Value of view is "+view.toString());
				BufferedImage image;
				try {
					// System.out.println("Made it to 0.");
					// Let's try to check if the computation was consider done
					// or was cancelled.
					// System.out.println("IsDone: " + isDone() + " isCancelled:
					// " + isCancelled());
					if (isCancelled())
						return; // Abort if the computation was cancelled.
					// System.out.println("Made it to 0.3");
					image = get();
					// System.out.println("Made it to 0.5.");
					double aspect = (double) image.getWidth() / (double) image.getHeight();
					// System.out.println("Made it to 1.");
					BufferedImage iconImage = new BufferedImage(128, (int) (128 / aspect), BufferedImage.TYPE_INT_RGB);
					// System.out.println("Made it to 2.");
					Graphics2D g = iconImage.createGraphics();
					// System.out.println("Made it to 3.");
					g.drawImage(image, 0, 0, 128, (int) (128 / aspect), null);
					setImage(image, pageNumber);
					setIcon(iconImage, pageNumber);
					// System.out.println("Made it to 4.");
					pageImageStatus[pageNumber] = 1; // loading
					setChanged();// Observable Pattern: inform gui that images
									// have
									// changed
					// System.out.println("Made it to 5.");
					notifyObservers();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					System.out.println("Interrupted exception in DataManager.done()");
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					System.out.println("Execution exception in DataManager.done()");
					e.printStackTrace();
				}

			}

		};

		worker.execute();
	}

	public void setImage(BufferedImage image, int pageNum) {
		if (pageNum >= pageImageList.length || pageNum < 0) {
			System.out.println("Can't set page " + pageNum + ". Must be between 1 and " + pageImageList.length + ".");
			return;
		}
		pageImageList[pageNum] = image;
	}

	public void setIcon(BufferedImage image, int pageNum) {
		if (pageNum >= pageIconList.length || pageNum < 0) {
			System.out.println("Can't set icon " + pageNum + ". Must be between 1 and " + pageIconList.length + ".");
			return;
		}
		pageIconList[pageNum] = image;
	}

	public void AnalyzeEvidence() {
		// eg.loadGeneGazetteer(this.getPageWS(view.getCurrentPage()));
		LayerList ll = view.getCurrentWS().getLayerList();
		for (String layerName : ll.keySet()) {
			if (layerName.contains("Labeled table")) {
				VTable table = (VTable) ll.getLayerByName(layerName).getRep().get(0);
				eg.gatherEvidence(table);

			}
		}

	}

	public void PreprocessDocument() {

		setPreprocessState(0);// underway

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			protected Void doInBackground() throws Exception {
				eg.loadGeneGazetteer(getHeadWorkingSet());
				return null;
			}

			protected void done() {
				setPreprocessState(1);
				setChanged();
				notifyObservers();
			}
		};

		worker.execute();
	}

	public static void main(String[] args) {

	}

	public int getPreprocessState() {
		return preprocessState;
	}

	public void setPreprocessState(int preprocessState) {
		this.preprocessState = preprocessState;
	}

	public WorkingSet mergeSelection() {
		WorkingSet merged = null;
		List<WorkingSet> toMerge = new ArrayList<WorkingSet>();
		for (Object obj : view.getSelected()) {
			if (obj instanceof WorkingSet) {
				toMerge.add((WorkingSet) obj);
			}
		}
		if (toMerge.size() > 1) {
			Rectangle combined = toMerge.get(0).getBbox();
			String name = toMerge.get(0).getName() + "_MERGE";
			Set<String> tags = new HashSet<String>();
			WorkingSet parent = toMerge.get(0).getParent();
			for (WorkingSet ws : toMerge) {
				combined.add(ws.getBbox());
				tags.addAll(ws.getTags());
				parent.getChildren().remove(ws);
			}

			merged = parent.createChild(name, combined);
			for (String tag : tags) {
				merged.addTag(tag);
			}
		}
		return merged;
	}

	public void reloadWorkingSets() {
		WorkingSet top = new WorkingSet(null, "document");
		top.setFilename(this.getHeadWorkingSet().getFilename());
		reloadWorkingSets(this.getHeadWorkingSet(), top);
		int page = view.getCurrentPage();
		this.setHeadWorkingSet(top);
		this.view.setCurrentWS(top.getChildren().get(page - 1));

	}

	private void setHeadWorkingSet(WorkingSet top) {
		headWS = top;
	}

	public void reloadWorkingSets(WorkingSet oldWS, WorkingSet newWS) {

		for (WorkingSet oldChild : oldWS.getChildren()) {
			WorkingSet newChild = newWS.createChild(oldChild.getName());
			for (El el : this.getElsIn(oldChild.getPage(), oldChild.getBbox())) {
				newChild.addItem(el);
			}
			for (Line2D line : oldChild.getSeparators()) {
				newChild.addSeparator(line);
			}
			newChild.setPage(oldChild.getPage());
			newChild.setTags(oldChild.getTags());
			reloadWorkingSets(oldChild, newChild);

		}

	}

	@SuppressWarnings("unchecked")
	public void addSeparator(int page, Line2D line) {
		// TODO Auto-generated method stub
		this.getPageWS(page).addSeparator(line);
		((List<VPage>) getVDocument().getItems()).get(page - 1).splitAtSeparator(line);
		this.reloadWorkingSets();

	}

	public List<Line2D> getSeparators(int page) {
		return this.getPageWS(page).getSeparators();
	}

	public void commandLine(CommandLineValues cmd) {

		if (cmd.isHelp() || !cmd.hasInput()) { // Skip if help or no input.
			return;
		}

		loadClojure(); // Make sure that Clojure is loaded.

		File source = cmd.getFile();
		File outFile = cmd.getOutFile();

		// If directory, process each PDF in the directory.
		if (source.isDirectory()) {
			System.out.println("Converting all files in directory " + source);

			for (String subStr : source.list()) {
				subStr = source.getAbsolutePath() + "/" + subStr;
				File sub = new File(subStr);
				if (isPdfFile(sub)) {
					processRepTablesInFile(sub, outFile);
				}
			}

		} else if (isPdfFile(source)) {
			processRepTablesInFile(source, outFile);
		} else {
			System.out.println("No file found: " + source);
			Path currentRelativePath = Paths.get("");
			String s = currentRelativePath.toAbsolutePath().toString();
			System.out.println("Use absolute path or relative path from:");
			System.out.println(s);
		}

	}

	static private boolean isPdfFile(File f) {
		return (f.exists() && f.getName().endsWith(".pdf"));
	}
	
	static private File swapExtensionFor (File inputFile, String newExtension) {
		
		String pathstring = inputFile.getAbsolutePath();
		
		int ext = pathstring.lastIndexOf('.');
		String fileStr = pathstring.substring(0, ext);
		return new File(fileStr + newExtension);
	}
	
	/*
	 * restoreDefaultOverlay -- Restore the default overlay for the given PDF file.
	 * 
	 * @param pdfFile - PDF file pointer.
	 */
	public void restoreDefaultOverlay (File pdfFile) {
		
		File jsonFile = swapExtensionFor(pdfFile,".json");

		if (!jsonFile.exists()) {
			System.out.println("No JSON file for " + pdfFile);
			return;
		}
		
		Doc.restoreWSfromOverlay(this, jsonFile);
	}
	
	private void processRepTablesInFile(File pdf, File out) {
		
		this.setPdfFile(pdf, true);
		
		restoreDefaultOverlay(pdf);

		int tableCounter = 0;

		for (WorkingSet pg : this.getHeadWorkingSet().getChildren()) {

			for (WorkingSet tableWS : pg.getChildrenWithTag("TABLE")) {

				if (tableWS.isLeaf()) { // If table is empty, run AutoTable.
					tableWS.doAutoTableWS();
				}

				tableCounter = tableCounter + 1;

				@SuppressWarnings("unchecked")
				List<Layer> layers = (List<Layer>) Table.applyLayerProducer("table", tableWS);

				File repFile = swapExtensionFor(pdf, ("." + tableCounter + ".json"));
				
				writeLayers(layers,repFile);

			}

		}
	}
   /*
    * writeLayers - Given a list of Layer objects, write the representations
    *               to the given output JSON output file.
    *               
    * @param layerList - A list of layer objects.
    * 
    * @param output - The JSON output file.	
    */
	private void writeLayers (List<Layer> layerList, File output) {
		PrintWriter writer;
		
		try {
			writer = new PrintWriter(output, "UTF-8");
			for (Layer layer : layerList) {
				writer.println(Table.layerToJSONString(layer.getRep()));
			}
			writer.close();
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: File could not be created: " + output);
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	private void loadClojure() {
		System.out.println("Loading clojure core");
		IFn require = Clojure.var("clojure.core", "require");
		require.invoke(Clojure.read("dray.core"));
		IFn populateFn = Clojure.var("dray.core", "populate-gui-tables");
		populateFn.invoke();
	}
}
