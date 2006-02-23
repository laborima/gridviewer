package PlotStrategy;


import org.eclipse.swt.graphics.ImageData;

import Context.Mediator;
import arrayData.arrayData;
import arrayData.arrayDataFactory;


public class arrayImageDataIJ extends ImageDataStrategy {
	

	public arrayImageDataIJ(Mediator med) {
		super(med);
	}

	public ImageData createImageDataStrategy()
	{	
		//Add Proxy usability
		ImageData imageData = new ImageData(med.zoom.getDisplayRect().width,
				med.zoom.getDisplayRect().height,8,
				med.palette.getPaletteData());
		arrayDataFactory arrayFact = new arrayDataFactory();
		arrayData array = arrayFact.getArrayData(med);
        int pixel = 0;  
        int step = med.zoom.sizePixel();
		for(int x=0;x<imageData.width;x++){
			for(int y=0;y<imageData.height;y++){
				if((x % step == 0 || y % step == 0) 
						&& x/step <array.getSyzeX() && y/step <array.getSyzeY()){
				 pixel = med.palette.getPixelValue(array.getValue(x/step,y/step));
				}
			    imageData.setPixel(x,imageData.height-(y+1),pixel);
			}
		}  
		return imageData;
	}

}
