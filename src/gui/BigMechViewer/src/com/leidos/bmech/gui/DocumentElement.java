package com.leidos.bmech.gui;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

public class DocumentElement {
	private Image sourceImage;
	public int x1InSource, y1InSource, x2InSource, y2InSource;
	private String value;
	public DocumentElement(Image src,int x1,int y1,int x2,int y2){
		sourceImage = src;
		x1InSource = x1;
		y1InSource = y1;
		x2InSource = x2;
		y2InSource = y2; 
	}
	
	public Image getImage(){
		return sourceImage;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	


}
