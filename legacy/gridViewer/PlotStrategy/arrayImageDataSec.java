package PlotStrategy;


import org.eclipse.swt.graphics.ImageData;

import Context.Mediator;
import arrayData.arrayData;
import arrayData.arrayDataFactory;


public class arrayImageDataSec extends ImageDataStrategy {
	

	public arrayImageDataSec(Mediator med) {
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
        int stepx = med.zoom.sizePixel();
        int stepy = imageData.height / array.getSyzeY();
		for(int x=0;x<imageData.width;x++){
			for(int y=0;y<imageData.height;y++){
				if((x % stepx == 0 || y % stepy == 0) 
						&& x/stepx <array.getSyzeX() && y/stepy <array.getSyzeY()){
				 pixel = med.palette.getPixelValue(array.getValue(x/stepx,y/stepy));
				}
			    imageData.setPixel(x,y,pixel);
			}
		}  
		return imageData;
	}

}
