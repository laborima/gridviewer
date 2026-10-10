package decorator;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tracker;

import configManagerView.selectedArea;

import Context.Context;

public class IJBoxDecor extends Decorator {
 
	private Canvas canvas;
	private selectedArea  selection = null;
	private Rectangle zoomRect = null;
	
	public IJBoxDecor(Context context) {
		super(context);
		createCanvas();
		selection = new selectedArea(context.med.zoom);
	}


	public void paintImageData(GC gc) {
		gc.drawImage(new Image(null, context.getImageData()),
				context.med.zoom.getDisplayRect().x,
				context.med.zoom.getDisplayRect().y);
	}
	
	
	private void paint(GC gc) {
		paintImageData(gc);
		gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(context.med.zoom.getDisplayRect().x-1,
				context.med.zoom.getDisplayRect().y-1,
				context.med.zoom.getDisplayRect().width+1,
				context.med.zoom.getDisplayRect().height+1);
		gc.setClipping(context.med.zoom.getDisplayRect());
		selection.paint(gc);	
	}
	

	private void createCanvas() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		canvas = new Canvas(context, SWT.NONE);
		canvas.setLayoutData(gridData);
		canvas.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        canvas.setSize(context.med.zoom.getCanvasRect().width,context.med.zoom.getCanvasRect().height);
        
		canvas.addPaintListener( new PaintListener(){
		    public void paintControl( PaintEvent e){
		        GC gc = new GC(canvas);
		        paint(gc);    
		        gc.dispose();
		    }
		}
		);

		 canvas.addMouseListener(new MouseAdapter() {
	            public void mouseDown(MouseEvent e) {
		           int style = selection.getTrackerStyle(e);
	            	if( selection.getTrackerStyle(e) == 50 ){
	            	Point p1=new Point(0,0);
	    			Point p2=new Point(0,0);
	                Tracker tracker = new Tracker(canvas,SWT.RESIZE );
	                tracker.setStippled(false); 
	                tracker.setRectangles( new Rectangle[] {new Rectangle(e.x,e.y,0,0)});
	                tracker.open();
	                zoomRect = tracker.getRectangles()[0];
	                p1 = context.med.zoom.toData(new Point(zoomRect.x,zoomRect.y));
	                p2 = context.med.zoom.toData(new Point(zoomRect.width+zoomRect.x,zoomRect.height+zoomRect.y));
	                int tmp;
					if(p1.x > p2.x)
					{
						tmp = p1.x;
						p1.x = p2.x;
						p2.x = tmp;
					}
					if(p1.y > p2.y)
					{
						tmp = p1.y;
						p1.y = p2.y;
						p2.y = tmp;
					}
					if(((p2.x-p1.x) >1 && (p2.y-p1.y) >1) 
							&& context.med.zoom.getDisplayRect().contains(zoomRect.x,zoomRect.y) &&
							context.med.zoom.getDisplayRect().contains(zoomRect.width+zoomRect.x,zoomRect.height+zoomRect.y)) {
	                context.med.zoom.setSelectedArea(new Rectangle(p1.x,p1.y,p2.x,p2.y));
	                context.med.zoom.computeZoom();
                    context.med.palette.resetNewRange();
                    context.readData();
					}
	                redraw();
		            } 
	            	else{
		               Tracker tracker = new Tracker(canvas,style );
			           tracker.setStippled(true); 
			           tracker.setRectangles( new Rectangle[] { context.med.zoom.toDisplay(selection.getSelectedRect())});
			           tracker.open();
			           selection.setSelectedRect(context.med.zoom.toData(tracker.getRectangles()[0]));
			           context.getObserver().sendNotify(selection.getSelectedRect().x+"/"+selection.getSelectedRect().y+"/"+selection.getSelectedRect().width+"/"+selection.getSelectedRect().height);
			           canvas.redraw();
		            }
	            }

		 });
	
	   
				
	    
	}
		

	public void redraw() {
		canvas.redraw();
	}

	public void dispose() {
		canvas.dispose();
	}
		
        
}

	   
	   
