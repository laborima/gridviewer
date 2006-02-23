package palette;


import Context.Mediator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import tools.MathUtil;

/**
 * @author   mlaborie
 */
public class Palette {

	PaletteData pal;
	/**
	 * @uml.property  name="min"
	 */
	private double min=0;
	/**
	 * @uml.property  name="max"
	 */
	private double max=0;
	/**
	 * @uml.property  name="newmax"
	 */
	private double newmax= - 1.7E308;
	/**
	 * @uml.property  name="newmin"
	 */
	private double newmin = 1.7E308;
	/**
	 * @uml.property  name="maskValue"
	 */
	private double maskValue;
	private Mediator med;
    private static RGB[] colorMap = null;
	paletteDialog dial=null;
    
	
	public Palette(Mediator med) {
		super();
		this.med = med;
		//computeColorsIndex();
		readColorMap(null);
		// TODO Auto-generated constructor stub
	}
	
	public void computeRange(double val){
		if(val > newmax && val!= maskValue)
			newmax = val;
		if(val < newmin && val!= maskValue)
			newmin = val;
	}
	
	
	public void resetNewRange(){
		newmax= - 1.7E308;
		newmin = 1.7E308;
	}
	
	
	public void paintColorBar(GC gc){	
		 int xoffset = (med.zoom.getCanvasRect().width - 510)/2;
    	 int yoffset = med.zoom.getDisplayRect().y + med.zoom.getDisplayRect().height;

		for(int i = 0; i<255 ;i++){
			gc.setBackground(new Color(Display.getDefault(),colorMap[i+1]));
			gc.fillRectangle(i*2+xoffset,20+yoffset,2,20);
		}
		
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		gc.drawRectangle(xoffset,20+yoffset,510,20);
		gc.drawText(""+MathUtil.round(min)+"",xoffset,yoffset+41);
		gc.drawText(""+MathUtil.round(max)+"",510+xoffset,yoffset+41);
		gc.drawText(""+MathUtil.round((max+min)/2)+"",256+xoffset,yoffset+41);
	}
	
	

	

	public void setRange(double min, double max) {
		this.min = min;
		this.max = max;
	}
	

	public boolean setDefaultRange(String name){
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(Palette.class.getResource("range.txt").openStream()));
	        String str;
	        String[] s;
	        while ((str = in.readLine()) != null) {
	            s = str.split(" ");
	            if(s[0].equals(name)){
	            	this.min =  Double.parseDouble(s[1]);
	            	this.max =  Double.parseDouble(s[2]);
	            	return true;
	            }
	        }
	        in.close();
	    } catch (IOException e) {
	    }
		return false;
	}


	/**
	 * @return  Returns the maskValue.
	 * @uml.property  name="maskValue"
	 */
	public double getMaskValue() {
		return maskValue;
	}


	/**
	 * @param maskValue  The maskValue to set.
	 * @uml.property  name="maskValue"
	 */
	public void setMaskValue(double maskValue) {
		this.maskValue = maskValue;
	}


	/**
	 * @return  Returns the max.
	 * @uml.property  name="max"
	 */
	public double getMax() {
		return max;
	}


	/**
	 * @param max  The max to set.
	 * @uml.property  name="max"
	 */
	public void setMax(double max) {
		this.max = max;
	}


	/**
	 * @return  Returns the min.
	 * @uml.property  name="min"
	 */
	public double getMin() {
		return min;
	}


	/**
	 * @param min  The min to set.
	 * @uml.property  name="min"
	 */
	public void setMin(double min) {
		this.min = min;
	}


	/**
	 * @return  Returns the newmax.
	 * @uml.property  name="newmax"
	 */
	public double getNewmax() {
		return newmax;
	}


	/**
	 * @return  Returns the newmin.
	 * @uml.property  name="newmin"
	 */
	public double getNewmin() {
		return newmin;
	}

	public PaletteData getPaletteData() {
		// TODO Auto-generated method stub
		return pal;
	}

	public int getPixelValue(double value) {
		computeRange(value);
		if(value==maskValue )
			value = 0;
		else if( value <= min )
			value = 1;
		else if( value >= max)
			value = 255;
		else
			value = (value-min)*(255/(max-min))+1;
	     return (int) value;
	}
	
	/* private static void computeColorsIndex()
	  {
		colorMap = new RGB[256];
	    
		 for(int i =0 ;i<256;i++)
		    	colorMap[i] = new RGB(0,0,0);
		 
	    // From 1 to 32 we go from black to blue (000000 to FF0000)
	    // We use a parabola here ( y = sqrt( 2048*y) )
	    for( int i=1;i<=32;i++)
	    {
	    	colorMap[i].blue = (int) Math.sqrt( 2048. * i );
	    }
	    // Now, from 32 to 128 we go from red to yellow
	    // That is, from FF0000 to FFFF00
	    for( int i=32; i<=64; i++ )
	    {
	    	colorMap[i].blue = (int) 0xFF;
	    	colorMap[i].green = (int) (256.0f*(i/128.f));
	    	colorMap[i].red = (int) (256.0f*(i/256.f));
	    }
	    
	    for( int i=32; i<=128; i++ )
	    {
	    	colorMap[i].blue = (int) (256 -256.0f*(i/128.f));
	    	colorMap[i].green = (int) 0xFF;
	    	colorMap[i].red = (int) 0xFF;
	    }
	    
	    // Finally from 128 to 256 we go from yellow to white (FFFF00 to FFFFFF)
	    for( int i=128; i<256; i++ )
	    {
	    	colorMap[i].blue=(int) 0;
	        colorMap[i].green=(int)(256 - 256.0f*(i/256.f));
	    	colorMap[i].red = (int) 0xFF;
	    }
	    
	    colorMap[255] = new RGB(0xff,0xff,0xff);
	    
	      
	  } */

	 
	 public void readColorMap(String filename){
		if(filename==null)
			filename = "palette_blue_red.txt";
		 try {
				BufferedReader in = new BufferedReader(new InputStreamReader(Palette.class.getResource(filename).openStream()));
		        String str;
		        String[] s;
		        int i=0;
		        colorMap = new RGB[256];
		        while ((str = in.readLine()) != null) {
		            s = str.split(" +");
		            colorMap[i] = new RGB(Integer.parseInt(s[s.length-3])
		            		,Integer.parseInt(s[s.length-2])
		            		,Integer.parseInt(s[s.length-1]));
		            i++;
		        }
		        in.close();
		    } catch (IOException e) {
		    }
		    pal = new PaletteData(colorMap);
	 }
	
	 public Image readPaletteImage(String filename){
			Image img = new Image(null,512,40); 
			RGB [] colorMap = new RGB[256];
			try {
					BufferedReader in = new BufferedReader(new InputStreamReader(Palette.class.getResource(filename).openStream()));
			        String str;
			        String[] s;
			        int i=0;
			       
			        while ((str = in.readLine()) != null) {
			            s = str.split(" +");
			            colorMap[i] = new RGB(Integer.parseInt(s[s.length-3])
			            		,Integer.parseInt(s[s.length-2])
			            		,Integer.parseInt(s[s.length-1]));
			            i++;
			        }
			        in.close();
			    } catch (IOException e) {
			    }
			    GC gc = new GC(img);
			    for(int i = 0; i<255 ;i++){
					gc.setBackground(new Color(Display.getDefault(),colorMap[i+1]));
					gc.fillRectangle(i*2,10,2,40);
				}
			    return img;
		 }
	 
	
}
