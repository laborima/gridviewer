package PlotStrategy;

import Context.Mediator;
import org.eclipse.swt.graphics.ImageData;

/**
 * @author   mlaborie
 */
public abstract class  ImageDataStrategy {

	protected Mediator med = null ;
	
	public ImageDataStrategy(Mediator med) {
		super();
		this.med = med;
	}
	
	public abstract ImageData createImageDataStrategy();
	

}
