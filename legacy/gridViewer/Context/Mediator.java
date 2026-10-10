package Context;
import palette.Palette;
import PlotStrategy.ImageDataStrategy;
import arrayData.varData;

/**
 * @author   mlaborie
 */
public class Mediator {

	public zoomManager zoom;
	public Palette palette;
	public ImageDataStrategy strategy;
	public varData var;
	
	public void registerZoom(zoomManager zoom){
		this.zoom = zoom;
	}
	
	public void registerPalette(Palette palette){
		this.palette = palette;
	}
	
	public void registerVar(varData var){
		this.var = var;
	}
	
	public void registerStrategy(ImageDataStrategy strategy){
		this.strategy = strategy;
	}


	public void dispose() {
		var.finalize();
	}
	
	
}
