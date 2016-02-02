package com.leidos.bmech.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import com.leidos.bmech.model.Layer;
import com.leidos.bmech.model.WorkingSet;
import com.leidos.bmech.view.DataManagerView;

import drae.j.BoundingBox;
import drae.j.VisualElement.*;

/**
 * the drawing canvas used to draw the document and serve
 * as the most used interface with the user. Gets all of the
 * data to be drawn from the DataManager
 * @author powelldan
 *
 */
@SuppressWarnings("serial")
public class DocumentCanvas extends JPanel {
	List<DocumentElement> currentlySelected;
	DocumentElement lastSelected;
	El lastSelectedEl;
	ViewerApp mainApp;
	private float scale;
	boolean mouseDown;
	Rectangle dragRect;
	DocumentCanvas pThis;
	Point rightDragLast;
	boolean dragged;
	WorkingSet lastWS;
	WorkingSet resizing;
	int hResize;
	int vResize;
	final static float minScale = 0.2f;
	final static float maxScale = 4.0f;
	final static int buffer = 20;
	private DataManagerView view;
	private boolean retainViewZoom;
	/**
	 * initialize variables and add mouse listeners
	 * @param parent
	 */
	public DocumentCanvas (ViewerApp parent) {
		//setBackground (Color.GRAY);
		//setSize(300, 300);
		pThis = this;
		retainViewZoom = false;
		mouseDown = false;
		mainApp = parent;
		view = mainApp.getDataManager().getView();
		scale = 1.0f;
		currentlySelected = new ArrayList<DocumentElement>();
		dragged = false;
		addMouseListener (new MouseAdapter () {
			public void mouseClicked(MouseEvent e){
				//check if anything has been loaded
				if(mainApp.getDataManager().getVDocument() == null) return;
				El targetEl = mainApp.getDataManager().getElAt(
						(int)((e.getX()/scale+ offX())), 
						(int)((e.getY()/scale+ offY())));
				List<WorkingSet> childrenClicked = mainApp.getDataManager().getWSAt(
						(int)((e.getX()/scale+ offX())), 
						(int)((e.getY()/scale+ offY())));
				if(SwingUtilities.isLeftMouseButton(e)){

					//if double click, drill down to child WS
					if(e.getClickCount() == 2){
						//only drill down if unambiguous
						for(WorkingSet clicked : childrenClicked){
							if(view.getSelected().contains(clicked)){
								mainApp.getView().setCurrentWS(childrenClicked.get(0));
								mainApp.viewWSUpdated();
								break;
							}
						}
					} else if (e.getClickCount() == 1){
						//view.getSelected().clear();
						Object clickedObj = null;
						if (childrenClicked.size() > 0){
							//view.getSelected().add(clickedWS.get(0));
							clickedObj = childrenClicked.get(0);
						} else {
							view.getSelected().add(targetEl);
							clickedObj = targetEl;
						}
					//if ctrl is pressed, dont clear the selection, just toggle the clicked el
					if((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK){						
						if(view.getSelected().contains(clickedObj)){
							view.getSelected().remove(clickedObj);
						} else {
							view.getSelected().add(clickedObj);
						}
					} else { 
						//clear selected and add the clicked el 
						view.getSelected().clear();
						view.getSelected().add(clickedObj);
					}
					
					
					mainApp.selectedChanged();
					//}
					if(lastSelectedEl!=null)mainApp.appendToLog(lastSelectedEl.toString());
					}
				} else if(SwingUtilities.isRightMouseButton(e)){
					boolean selectionClicked = false;
	//				Object clickedObj = null;
					if(dragRect != null){
						Rectangle2D dragRectDescaled = new Rectangle(
								(int) (dragRect.getX() + offX() ),
								(int) (dragRect.getY() + offY()),
								(int) (dragRect.getWidth()),
								(int) (dragRect.getHeight()));
						if(dragRectDescaled.contains(new Point((int)((e.getX()/scale+ offX())), 
							(int)((e.getY()/scale+ offY()))))){
								selectionClicked = true;
						}
					}
					DocumentMenu menu;
					
					if(selectionClicked)
						menu = new DocumentMenu(mainApp, null, targetEl);
					else if (childrenClicked.size() > 0 && childrenClicked.get(0) instanceof WorkingSet){
						menu = new DocumentMenu(mainApp,(WorkingSet)childrenClicked.get(0), targetEl);
					} else {
						menu = new DocumentMenu(mainApp, view.getCurrentWS(), targetEl);
					}
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
				repaint();
			}
			public void mousePressed(MouseEvent e){
				//check if anything has been loaded
				if(mainApp.getDataManager().getVDocument() == null) return;
				if (e.getButton() == MouseEvent.BUTTON1){
					mouseDown = true;
					//check to see if we should be resizing a working set boundary
					//these ints are the mouse point in ws space
					int wsX = (int)((e.getX()/scale+ offX()));
					int wsY = (int)((e.getY()/scale+ offY()));
					List<WorkingSet> clickedWS = mainApp.getDataManager().getWSEdgeAt(wsX, wsY);
					
					if (clickedWS.size() > 0 ){
						System.out.println(view.getSelected().contains(clickedWS.get(0)));

						if(view.getSelected().contains(clickedWS.get(0))){
							resizing = clickedWS.get(0);
							Rectangle tmp = new Rectangle(clickedWS.get(0).getBbox());
							//checkToSee vertical vs Horizontal resize
							hResize = clickedWS.get(0).isHorizontalEdge(wsX);
							vResize = clickedWS.get(0).isVerticalEdge(wsY);	
							
							dragRect = new Rectangle(
									(int)(tmp.getX() - offX()), 
									(int)(tmp.getY() - offY()),
									(int)(tmp.getWidth()),
									(int)(tmp.getHeight()));
						}
					
					} else { 
						dragRect = new Rectangle(new Point((int)(e.getX()/scale), (int)(e.getY()/scale)));
					}
				} else if (SwingUtilities.isRightMouseButton(e)){
					rightDragLast = e.getLocationOnScreen();
					pThis.setCursor(new Cursor(Cursor.MOVE_CURSOR));
				}
			}
			public void mouseReleased(MouseEvent e){
				//check if anything has been loaded
				if(mainApp.getDataManager().getVDocument() == null) return;
				if (e.getButton() == MouseEvent.BUTTON1){
					if(dragged){
						//if ctrl not pressed, clear list
						
						//translate the rectangle from screen space to VDocument space
						Rectangle dragRectDescaled = new Rectangle(
								(int) (dragRect.getX() + offX() ),
								(int) (dragRect.getY() + offY()),
								(int) (dragRect.getWidth()),
								(int) (dragRect.getHeight()));
						if(resizing == null){
							//create new WS
							if((e.getModifiers() & ActionEvent.CTRL_MASK) != ActionEvent.CTRL_MASK){
								view.getSelected().clear();
								
							}
							//normalize
							dragRectDescaled.setRect((Math.min(dragRectDescaled.getMinX(), dragRectDescaled.getMaxX())),
									(Math.min(dragRectDescaled.getMinY(), dragRectDescaled.getMaxY())),
									Math.abs(dragRectDescaled.getWidth()), 
									Math.abs(dragRectDescaled.getHeight()));
							if(mainApp.isSplitModeEnabled()){							
								mainApp.createSeparator(new Line2D.Double(dragRectDescaled.getMinX(), dragRectDescaled.getMinY(), dragRectDescaled.getX(), dragRectDescaled.getMaxY()));
								//remember old viewport information, this will get lost when we
								//reload the workingsets based on the split
								pThis.retainViewZoom = true;
				    			repaint();
							
							} else {
								mainApp.rectangleDrawn(dragRectDescaled);
							}
						} else {
							//resize WS using rectangle
							mainApp.rectangleDrawn(resizing, dragRectDescaled);
						}
					}
					dragged = false;
					mouseDown = false;
					resizing = null;
				}else if (SwingUtilities.isRightMouseButton(e)){
					pThis.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}

			}
			
		});
		addMouseWheelListener (new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e){
				//check if anything has been loaded
				if(mainApp.getDataManager().getVDocument() == null) return;
				float newScale;
                if (e.getWheelRotation() < 0) {
                	newScale = Math.min(scale + 0.1f, maxScale);
                } else {
                	newScale = Math.max(scale - 0.1f, minScale);
                }
                pThis.setZoom(newScale);

			}
		});
		addMouseMotionListener (new MouseMotionListener(){

			@Override
			public void mouseDragged(MouseEvent e) {
				//check if anything has been loaded
				if(mainApp.getDataManager().getVDocument() == null) return;
				//adjust the width of the rectangle and scale
				if(SwingUtilities.isLeftMouseButton(e)){
					dragged = true;
					//check if we are resizing a WS
					if(resizing != null){
						double x =  dragRect.getMinX();
						double y = dragRect.getMinY();
						double h = dragRect.getHeight();
						double w = dragRect.getWidth();
						if(pThis.hResize == 1)
							w = (e.getX()/scale-x);
						if(pThis.hResize == -1){
							w = dragRect.getMaxX() - (e.getX()/scale);
							x = (e.getX()/scale);		
						}
						if(pThis.vResize == 1)
							h = (e.getY()/scale-y);
						if(pThis.vResize == -1){
							h = dragRect.getMaxY() - (e.getY()/scale);
							y = (e.getY()/scale);
						}
						//dont allow dragging inverse rectangle
						if(w > 0 && h > 0){
							dragRect.setFrame(x, y, w, h);
						}
						
					} else if (mainApp.isSplitModeEnabled()){
						//split line
						dragRect.setSize(0, (int)((e.getY()/scale-dragRect.y)));
						
					}
					else {//regular drag
						dragRect.setSize((int)((e.getX()/scale-dragRect.x)), (int)((e.getY()/scale-dragRect.y)));
					}
				} else if (SwingUtilities.isRightMouseButton(e)){
					JViewport viewPort = (JViewport) pThis.getParent();
					double deltaX = rightDragLast.getX() - e.getLocationOnScreen().getX();
					double deltaY = rightDragLast.getY() - e.getLocationOnScreen().getY();
	                Point vpp = viewPort.getViewPosition();
	                vpp.translate((int)deltaX, (int)deltaY);
	                pThis.scrollRectToVisible(new Rectangle(vpp, viewPort.getSize()));
	                rightDragLast = e.getLocationOnScreen();
				}
				repaint();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				if(pThis.lastWS==null)return;
				int wsX = (int)((e.getX()/scale+ offX()));
				int wsY = (int)((e.getY()/scale+ offY()));
				List<WorkingSet> clickedWS = mainApp.getDataManager().getWSEdgeAt(wsX, wsY);
				
				if (clickedWS.size() > 0 ){
					if(view.getSelected().contains(clickedWS.get(0))){
						int h = clickedWS.get(0).isHorizontalEdge(wsX);
						int v = clickedWS.get(0).isVerticalEdge(wsY);
						if(h!=0 && v == 0){
							pThis.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
						}else if(h==0 && v != 0){
							pThis.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
						}else if((h==1 && v == 1) || (h==-1 &&v==-1)){
							pThis.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
						}else if((h==1 && v == -1) || (h==-1 &&v==1)){
							pThis.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
						}
							
					}
				} else{
					pThis.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
				
			}
			
		});

	}
	@Override
    public void paintComponent (Graphics g) {
		Graphics2D g2;
        g2 = (Graphics2D) g;
		super.paintComponent(g);
		paintUsingVDocument(g);
		if (dragRect != null){
			g2.setColor(new Color(0f,1f,0f));
			g2.drawRect((int)(Math.min(dragRect.getMinX(), dragRect.getMaxX())*scale),
					(int)(Math.min(dragRect.getMinY(), dragRect.getMaxY())*scale),
					(int)(Math.abs(dragRect.width)*scale), 
					(int)(Math.abs(dragRect.height)*scale));
		}
		
		//draw up button
		g2.setColor(Color.DARK_GRAY);
		//g2.drawRect(2, 2, 50, 15);
		g2.drawString("[^]", 5, 5);
		
	}
	/**
	 * Draw the VDocument objects on the canvas
	 * @param g
	 */
	public void paintUsingVDocument (Graphics g) {
        Graphics2D g2;
        g2 = (Graphics2D) g;	
		if(mainApp.getDataManager().getVDocument() == null){
			g2.drawString("No Document Loaded", 200, 200);
			return;
		}
		int x = mainApp.getDataManager().getOffsetX();
		int y = mainApp.getDataManager().getOffsetY();
		//check if working set has changed so we can refocus if needed
		if(view.getCurrentWS() != lastWS){
			lastWS = view.getCurrentWS();
			if(!retainViewZoom){
				scale = (float) Math.min( getVisibleRect().getWidth() / (lastWS.getBbox().getWidth() + scale*buffer*2),
						getVisibleRect().getHeight() / (lastWS.getBbox().getHeight()+ scale*buffer*2));
				this.scrollRectToVisible(new Rectangle());
			}
			dragRect = null;
			retainViewZoom = false;
		}
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		@SuppressWarnings("rawtypes")
		List pages = (List)mainApp.getDataManager().getVDocument().getItems();
		VPage page = (VPage)pages.get(view.getCurrentPage()-1);
		BoundingBox pagebb = (BoundingBox)page.getBbox();
		BufferedImage pageImage = mainApp.getDataManager().getPageImage(view.getCurrentPage());
		double factor = (double)pageImage.getWidth()/pagebb.getWidth();
    	//draw rectangles around child WSs
    	drawChildWSRecursive(g2, view.getCurrentWS(), 1);
		
    	//draw image
		for(El el : view.getCurrentWS().getItems()){
				BoundingBox bb = (BoundingBox)el.getBbox();
				
				g2.drawImage(pageImage,
					(int)((bb.getMinX()- offX())*scale),
					(int)((bb.getMinY()- offY())*scale),
					(int)((bb.getMaxX()- offX())*scale),
					(int)((bb.getMaxY()- offY())*scale),
					(int)(bb.x*factor) + x,
					(int)(bb.y*factor) + y, 
					(int)((bb.x+bb.width)*factor) + x,
					(int)((bb.y+bb.height)*factor) + y, null);
		}
		//draw working set and El boxes
		if(view.getSelected()!=null){
        	g2.setColor(new Color(0f,1f,0f,.3f ));
        	for (Object obj : view.getSelected()){
        		if (obj != null){
        			Rectangle bb = null;
	        		if(obj instanceof El){
		        		bb = ((BoundingBox)((El)obj).getBbox()).getBounds();
	        		} else if (obj instanceof WorkingSet){
	        			bb = ((WorkingSet) obj).getBboxWide();
	        			//g2.setColor(((WorkingSet) obj).getColor());
	        		}
	        		if(bb!=null){
		        		g2.fill(new Rectangle( (int)((bb.getMinX()- offX())*scale),
								(int)((bb.getMinY()- offY())*scale), 
								(int)(bb.getWidth()*scale),
								(int)(bb.getHeight()*scale)));
	        		}
        		}
        	
        	}
        	

        	/*g2.setColor(Color.BLUE);
        	g2.setStroke(new BasicStroke(3));
        	for ( WorkingSet child : view.getCurrentWS().getChildren()){
        		Rectangle bb = child.getBbox();
        		g2.draw(new Rectangle((int)((bb.getMinX()- offX())*scale),
						(int)((bb.getMinY()- offY())*scale), 
						(int)(bb.getWidth()*scale),
						(int)(bb.getHeight()*scale)));
        		g2.drawString(child.getName(), 
        				(int)((bb.getMinX()- offX())*scale),
						(int)((bb.getMinY()- offY() )*scale)-2 );
        	}
        	*/
		}
		
        /*if (lastSelectedEl != null){
        	BoundingBox bb = (BoundingBox)lastSelectedEl.bbox;
			g2.fill(new Rectangle( (int)(bb.getMinX()*scale),
					(int)(bb.getMinY()*scale), 
					(int)(bb.getWidth()*scale),
					(int)(bb.getHeight()*scale)));
		}*/
		for(Line2D sep : mainApp.getDataManager().getPageWS(view.getCurrentPage()).getSeparators()){
			
	        Graphics2D g2d = (Graphics2D) g.create();
	        g2d.setColor(Color.RED);
	        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0);
	        g2d.setStroke(dashed);
	        g2d.drawLine((int)((sep.getX1()- offX())*scale),
	        		(int)((sep.getY1()- offY())*scale), 
	        		(int)((sep.getX2()- offX())*scale),
	        		(int)((sep.getY2()- offY())*scale));
	        //gets rid of the copy
	        g2d.dispose();

		}
		
		
		//List<Layer> layers = mainApp.getDataManager().getLayerList().
		Rectangle2D imageBB = new Rectangle();
		for(Layer layer : mainApp.getDataManager().getLayerList().values()){
        	if(layer.isHighlight()){
				g2.setColor(UtiliBuddy.makeTransparent(layer.getColor(), 0.1f) );
	        	
	        	for (El el : layer.getItems()){
	        		if (el != null){
		        		BoundingBox bb = (BoundingBox)el.getBbox();
		        		g2.fill(new Rectangle( (int)((bb.getMinX()- offX())*scale),
								(int)((bb.getMinY()- offY())*scale), 
								(int)(bb.getWidth()*scale),
								(int)(bb.getHeight()*scale)));
		        	}
	        	}
	        	
	        	//System.out.println(layer.getRep());
	        	for (Map<String, Object>hm : layer.getRep()){
	        		//System.out.println("Rep " + hm.keySet());
	        		if(hm.containsKey("File")){
		        		imageBB = mainApp.getDataManager().getBBFromImg(new File((String)hm.get("File")).getName());
		        		double bee2pdfX = imageBB.getWidth()/((Long)hm.get("imageWidth")).doubleValue();
		        		double bee2pdfY = imageBB.getHeight()/((Long)hm.get("imageHeight")).doubleValue();
		        		if(hm.containsKey("Blobs")){
		        			@SuppressWarnings("rawtypes")
							List blobs = (List)hm.get("Blobs");
		        			for(Object obj : blobs){
		        				@SuppressWarnings("unchecked")
								Map<String, Object> blob = (Map<String, Object>)obj;
			        			g2.setColor((Color)blob.get("color"));
			        			/*
			        					(float)((Double)blob.get("MeanRed")).floatValue()/255.0f, 
			        					(float)((Double)blob.get("MeanGreen")).floatValue()/255.0f, 
			        					(float)((Double)blob.get("MeanBlue")).floatValue()/255.0f, 1.0f) );
			        					*/
			        			g2.setStroke(new BasicStroke(3));
			        			BoundingBox bb = (BoundingBox)blob.get("bbox");
			        			g2.draw(new Rectangle( (int)((bb.getMinX()*bee2pdfX- offX() + imageBB.getX())*scale),
										(int)((bb.getMinY()*bee2pdfY- offY() + imageBB.getY())*scale), 
										(int)(bb.getWidth()*scale*bee2pdfX),
										(int)(bb.getHeight()*scale*bee2pdfY)));
		        			}
		        		}
		        	}
	        	}
        	}
		}

	
	}
	
	private void drawChildWSRecursive(Graphics2D g2, WorkingSet parent, int depth){
		g2.setStroke(new BasicStroke(2));
    	for ( WorkingSet child : parent.getChildren()){
    		
    		//first delve deeper so that top most WS are drawn over children
    		drawChildWSRecursive(g2, child, depth+1);

        	g2.setColor(UtiliBuddy.makeTransparent(child.getColor(), (float)1.0/(depth+1)));
    		Rectangle bb = child.getBboxWide();
    		g2.fill(new Rectangle((int)((bb.getMinX()- offX())*scale),
					(int)((bb.getMinY()- offY())*scale), 
					(int)(bb.getWidth()*scale),
					(int)(bb.getHeight()*scale)));
    		//only draw WS title for first level
    		if(depth == 1){
	    		g2.drawString(child.getName(), 
	    				(int)((bb.getMinX()- offX())*scale),
						(int)((bb.getMinY()- offY() )*scale)-2 );
    		}
    	}
	}

	private int offX(){
    	return (int)lastWS.getBbox().getX() - buffer;
    }
    private int offY(){
    	return (int)lastWS.getBbox().getY() - buffer;
    }
    private void setZoom(float zoom){
    	this.scale = zoom;
		@SuppressWarnings("rawtypes")
		List pages = (List)mainApp.getDataManager().getVDocument().getItems();
		VPage page = (VPage)pages.get(view.getCurrentPage()-1);
		BoundingBox pagebb = (BoundingBox)page.getBbox();
		pThis.setPreferredSize(new Dimension((int)(pagebb.getWidth()*scale), (int)(pagebb.getHeight()*scale)));
        pThis.revalidate();
		repaint();
    	
    }


}
