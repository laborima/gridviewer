package configManagerView;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import Context.zoomManager;

public class selectedArea {

	
	Composite parent = null;
	zoomManager zoom = null;
	private int MARGIN = 10;
	
	
	private Rectangle selectedRect = null;
	private Rectangle innerRect = null;
	private Rectangle topLeft = null;
	private Rectangle topRight = null;
	private Rectangle bottomLeft = null;
	private Rectangle bottomRight = null;
	
	public selectedArea(zoomManager zoom) {
		super();
		this.zoom = zoom;
		createSelectedArea();

	}
	
	private void createSelectedArea(){
		
		selectedRect = zoom.toData(new Rectangle(zoom.getDisplayRect().width/2-100,zoom.getDisplayRect().height/2-100,200,200)); 
			//zoom.toData(new Rectangle(100,150,300,300));
	}
	
	private void updateArea(){
		Rectangle temp = zoom.toDisplay(selectedRect);
		innerRect = new Rectangle(temp.x - MARGIN,
				temp.y + MARGIN,
				temp.width - MARGIN,
				temp.height - MARGIN);
		topLeft = new Rectangle(temp.x - MARGIN,
				temp.y - MARGIN,
				MARGIN,
				MARGIN);
		topRight = new Rectangle(temp.width + temp.x,
				temp.y - MARGIN,
				MARGIN,
				MARGIN);
		bottomRight = new Rectangle(temp.width + temp.x,
				temp.height+temp.y,
				MARGIN,
				MARGIN);
		bottomLeft = new Rectangle(temp.x - MARGIN,
				temp.height+temp.y,
				MARGIN,
				MARGIN);
	}
	
	
	public void paint(GC gc){
	   updateArea();
	   gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
	   gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
	   gc.drawRectangle(zoom.toDisplay(selectedRect));
	   gc.fillRectangle(topLeft);
	   gc.fillRectangle(topRight);
	   gc.fillRectangle(bottomLeft);
	   gc.fillRectangle(bottomRight);
	}
	
	
	public int getTrackerStyle(MouseEvent e){
		Point p = new Point(e.x,e.y);
		if(topLeft.contains(p))
			return SWT.RESIZE | SWT.UP | SWT.LEFT;
		if(topRight.contains(p))
			return SWT.RESIZE | SWT.UP | SWT.RIGHT;
		if(bottomLeft.contains(p))
			return SWT.RESIZE | SWT.DOWN |SWT.LEFT ;
		if(bottomRight.contains(p))
			return SWT.RESIZE | SWT.DOWN | SWT.RIGHT;
		if(!innerRect.contains(p) && zoom.toDisplay(selectedRect).contains(p))
			return SWT.NONE;
		return 50;
	}

	public void setSelectedRect(Rectangle rectangle) {
		selectedRect = rectangle;
	}

	public Rectangle getSelectedRect() {
		return selectedRect;
	}

}
