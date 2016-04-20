package com.leidos.bmech.gui;

import java.awt.Color;

public class UtiliBuddy {
	final static Color[] colors = {Color.BLUE, Color.RED,
		Color.GREEN, Color.ORANGE, Color.PINK, Color.CYAN, Color.YELLOW};
	
    public static Color makeTransparent(Color c1, float trans){
    	return new Color((float)c1.getRed()/255.0f, 
    			(float)c1.getGreen()/255.0f, 
    			(float)c1.getBlue()/255.0f, trans);
    }
    
    public static Color getAColor(int index){
    	return colors[index%colors.length];
    }
    public static boolean isNumeric(String str)
    {
      //return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    	return str.matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
    }
    
    public static double mean(double[] vals) {
    	double sum = 0;
        for (int i = 0; i < vals.length; i++) {
            sum += vals[i];
        }
        return sum / vals.length;
    }
    public static double stddev(double [] vals){

        double avg = mean(vals);
        double sum = 0.0;
        for (int i = 0; i < vals.length; i++) {
            sum += (vals[i] - avg) * (vals[i] - avg);
        }
        return Math.sqrt(sum / (vals.length));
    }
    //must be sorted
    public static double median(Double[] m) {
        int middle = m.length/2;
        if (m.length%2 == 1) {
            return m[middle];
        } else {
            return (m[middle-1] + m[middle]) / 2.0;
        }
    }
    public static double mode(Double a[]) {
        int maxCount = 0;
        double maxValue = 0.0;
        for (int i = 0; i < a.length; ++i) {
            int count = 0;
            for (int j = 0; j < a.length; ++j) {
                if (a[j] == a[i]) ++count;
            }
            if (count > maxCount) {
                maxCount = count;
                maxValue = a[i];
            }
        }

        return maxValue;
    }
    
    public static double distance(double x1, double y1, double x2, double y2){
    	return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    	
    }
    
    
}
