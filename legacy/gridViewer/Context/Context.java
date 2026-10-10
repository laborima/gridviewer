package Context;

import observer.Observable;
import observer.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import palette.Palette;
import PlotStrategy.ImageDataStrategy;
import PlotStrategy.arrayImageDataIJ;
import PlotStrategy.arrayImageDataLonLat;
import PlotStrategy.arrayImageDataSec;
import arrayData.varData;
import decorator.Decorator;
import decorator.IJBoxDecor;
import decorator.IJDecor;
import decorator.LonLatDecor;
import decorator.introDecor;
import decorator.threeDDecor;

/**
 * @author   mlaborie
 */
public class Context extends Composite implements Observable{
	
	public Mediator med;
	private ImageDataStrategy strategy;
	private varData var;
	private Decorator decor;
	private zoomManager zoom;
	private Palette palette;
	public Menu menu;
	private ImageData data = null;
	private Observer obs = null;
	
	private boolean lonlat = false;
	
	
	public Context( Composite parent, int style)
	{
	//set SWT.EMBEDDED so we can use SWT_AWT.new_Frame
	  super(parent, style);
      med = new Mediator();
      var = new varData(med);
      zoom = new zoomManager(med);
      palette = new Palette(med);
 
      
      med.registerPalette(palette);
      med.registerVar(var);
      med.registerZoom(zoom);
      
	  
	  setIJContext();
	  setIntroView();
	}
	
	
	public void setIJContext()
	{
		
		if(strategy == null)
		  {
			strategy = new arrayImageDataIJ(med);
			med.registerStrategy(strategy);
		  } else if(!strategy.getClass().equals(arrayImageDataIJ.class)){
			  strategy = new arrayImageDataIJ(med);
				med.registerStrategy(strategy);
		  }
	}

	public void setLonLatContext()
	{
		 if( strategy == null)
		  {
			 strategy = new arrayImageDataLonLat(med);
			 med.registerStrategy(strategy);
		  } else if(!strategy.getClass().equals(arrayImageDataLonLat.class)){
			  strategy = new arrayImageDataLonLat(med);
			  med.registerStrategy(strategy);
		  }
	}

	public void setThreeDView()
	{
		  if(!decor.getClass().equals(threeDDecor.class) && data !=null)
		  {
		  decor.dispose();
		  decor = new threeDDecor(this);
		  }
	}


	public void setIJView()
	{
		lonlat = false;
		if(!decor.getClass().equals(IJDecor.class) && data !=null )
		  {
		    decor.dispose();
			decor = new IJDecor(this);
			redraw();
		  } 
	}
	
	public void setIntroView()
	{
		   decor = new introDecor(this);
	}
	
	public void readData(){
		if(zoom.isSections() && med.var.getDim()>2){
			setSectionContext();
		} else {
			if(lonlat)
			setLonLatContext();
			else
			setIJContext();
		}
		data = strategy.createImageDataStrategy();
	}
	
	private void setSectionContext() {
		if(strategy == null)
		  {
			strategy = new arrayImageDataSec(med);
			med.registerStrategy(strategy);
		  } else if(!strategy.getClass().equals(arrayImageDataSec.class) || strategy == null)
		  {
			strategy = new arrayImageDataSec(med);
			med.registerStrategy(strategy);
		  }
	}


	public void updatePalette(){
		data.palette = med.palette.getPaletteData();
		redraw();
	}
	
	public void redraw(){
		decor.redraw();
	}

	public ImageData getTextureData() {
		   //  med.zoom.setSizePic(new Point(1024,1024));
		   // med.zoom.computeZoom();
		   //  readData();
		     return data.scaledTo(1024,1024);
	}

	public ImageData getImageData() {
		// TODO Auto-generated method stub
		return data;
	}
	
	public void dispose(){
		med.dispose();
	}

	

	 public void SaveImageAsPng(String filename){
		 if(data!=null){
		   ImageLoader imageLoader = new ImageLoader();
		   imageLoader.data = new ImageData[] {data};
		   imageLoader.save(filename,SWT.IMAGE_PNG); 
		 }
	   }
	
	public void SaveImageAsJpg(String filename) {
		if(data!=null){
		ImageLoader imageLoader = new ImageLoader();
		  
		   imageLoader.data = new ImageData[] {data};
		   imageLoader.save(filename,SWT.IMAGE_JPEG);
		}
	}
	
	

	
	
	public void SaveImageAsGif(String filename) {
		if(data!=null){
		System.out.println("Save as gif");
		ImageLoader imageLoader = new ImageLoader();
		   imageLoader.backgroundPixel = 0;
		   imageLoader.logicalScreenHeight = data.height;
		   imageLoader.logicalScreenWidth = data.width;
		   imageLoader.repeatCount = 0; // run forever
		   imageLoader.data = new ImageData[] {data};
		   imageLoader.data[0].disposalMethod =  SWT.DM_FILL_NONE;
		   imageLoader.data[0].delayTime = 10 ;
		   imageLoader.save(filename,SWT.IMAGE_GIF); 
		}
	}
	
	/*
	public void createAnimatedGif(String filename) {
		if(data!=null){
		System.out.println("Create animated gif");
		ImageLoader imageLoader = new ImageLoader();
		ImageData temp[] = imageLoader.load(filename);
		System.out.println(imageLoader.toString());
		System.out.println(temp[0].height+" "+temp[0].width+" "+temp[0].disposalMethod);
		imageLoader.data = new ImageData[temp.length+1];
		for(int i=0;i<temp.length;i++){
			imageLoader.data[i] = temp[i];
			imageLoader.data[i].disposalMethod = SWT.DM_FILL_NONE;
			imageLoader.data[i].delayTime = 10;
		}
		   imageLoader.data[temp.length] = data;
		   imageLoader.backgroundPixel = 0;
		   imageLoader.logicalScreenHeight = data.height;
		   imageLoader.logicalScreenWidth = data.width;
		   imageLoader.repeatCount = 0; // run forever
		   System.out.println(imageLoader.data.length);
		   imageLoader.save(filename,SWT.IMAGE_GIF); 
		}
	}
	*/
	
	/**
	 * @return  Returns the decor.
	 * @uml.property  name="decor"
	 */
	public Decorator getDecor() {
		return decor;
	}


	public void registerInterest(Observer obs) {
		this.obs = obs;
		
	}


	public Observer getObserver() {
		return obs;
	}


	public void setIJBoxView() {
		if(!decor.getClass().equals(IJBoxDecor.class) && data !=null )
		  {
		    decor.dispose();
			decor = new IJBoxDecor(this);
			redraw();
		  } 
	}


	public void setLonLatView() {
		lonlat = true;
		if(!decor.getClass().equals(LonLatDecor.class) && data !=null )
		  {
		    decor.dispose();
			decor = new LonLatDecor(this);
			redraw();
		  } 
		
	}


	
}
