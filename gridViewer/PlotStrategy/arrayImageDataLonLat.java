package PlotStrategy;


import java.io.IOException;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import Context.Mediator;
import arrayData.TwoDimArrayData;
import arrayData.arrayData;
import arrayData.arrayDataFactory;


public class arrayImageDataLonLat extends ImageDataStrategy {
	
	

	public arrayImageDataLonLat(Mediator med) {
		super(med);
		// TODO Auto-generated constructor stub
	}

	public ImageData createImageDataStrategy()
	{	
		
		
		   String request = new String(""
				 +med.zoom.getSelectedArea().y+":"+med.zoom.getSelectedArea().height+":"+med.zoom.getStride()+","
			   	 +med.zoom.getSelectedArea().x+":"+med.zoom.getSelectedArea().width+":"+med.zoom.getStride()+"");
			arrayDataFactory arrayFact = new arrayDataFactory();
			arrayData array = arrayFact.getArrayData(med);

		
		ImageData imageData = new ImageData(720,
				360,8,
				med.palette.getPaletteData());
		
        int pixel = 0;
        double lon,lat;
		NetcdfFile ncFile =  med.var.getFile();
		
		
		try {
			TwoDimArrayData nav_lon = new TwoDimArrayData( ((NetcdfFile) ncFile).findVariable("nav_lon").read(request));
			TwoDimArrayData nav_lat = new TwoDimArrayData( ((NetcdfFile) ncFile).findVariable("nav_lat").read(request));
			Point pos;
			double incx = (double)360/(double)imageData.width;
	        double incy = (double)180/(double)imageData.height;
	        
	        
	        
			for(int x=0;x<array.getSyzeX();x++){
				for(int y=0;y<array.getSyzeY();y++){
			       lon = nav_lon.getValue(x,y)+180;
			       lat = nav_lat.getValue(x,y)+90;
			       pixel = med.palette.getPixelValue(array.getValue(x,y));
			       if(pixel==0) pixel=255;
				   pos = new Point((int)(lon/incx),(int)(lat/incy));
				   if(pos.x>=0 && pos.x<imageData.width && pos.y>=0 && pos.y<imageData.height)
			        imageData.setPixel(pos.x,imageData.height-pos.y+1,pixel);
			    	   
				}
			
				//imageData.setPixel(x,imageData.height-(y+1),pixel);
			}
			
		
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
       int xnew=0;
       int a,b;
		for(int y=0;y<imageData.height;y++){
			for(int x=1; x<imageData.width-1;x++){
	  	     if(imageData.getPixel(x,y)==0){
	  	    	 xnew=x+1;
	  	    	a = imageData.getPixel(x-1,y);
	  	    	 while((b = imageData.getPixel(xnew,y))==0 && xnew<imageData.width-1)
	  	    		xnew++;
	  	    	imageData.setPixel(x,y,(a+b)/2);
	  	     }
	  	    if(imageData.getPixel(x-1,y)==255)
	  	    	imageData.setPixel(x-1,y,0); //Apply mask color
			}
		}
		
		
		//smooth
		/*
		for(int x=0;x<imageData.width;x++){
			for(int y=imageData.height-1;y>2;y--){
				if(imageData.getPixel(x,y-2)!=0 && imageData.getPixel(x,y-1)==0  && imageData.getPixel(x,y)!=0)
			      imageData.setPixel(x,y-1,imageData.getPixel(x,y));
			}
		} 
		
		for(int y=0;y<imageData.height;y++){
			for(int x=imageData.width-1;x>4;x--){
				if(imageData.getPixel(x-4,y)!=0 && imageData.getPixel(x-1,y)==0  && imageData.getPixel(x,y)!=0)
			      imageData.setPixel(x-1,y,imageData.getPixel(x,y));
			}
		}  
		*/
		
	
		
		//apply mask
		/*for(int x=0;x<imageData.width;x++){
			for(int y=0;y<imageData.height;y++){
				if(  x <array.getSyzeX() && y <array.getSyzeY()){
				 if(med.palette.getPixelValue(array.getValue(x,y))==0){
					 imageData.setPixel(x,imageData.height-(y+1),0);
				 }
				}
			}
		}  */
		
		
		
       return imageData;
        
	}
}

