package decorator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import Context.Context;

public class LonLatDecor extends Decorator {
 
	private Canvas canvas;

	//private Rectangle selectedRect = null;
	
	public LonLatDecor(Context context) {
		super(context);
		createCanvas();
		
		// TODO Auto-generated constructor stub
	}

	private void paintAxes(GC gc){ 
		gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(context.med.zoom.getDisplayRect().x-1,
				context.med.zoom.getDisplayRect().y-1,
				context.getImageData().width+1,
				context.getImageData().height+1);
	/*	
		for(int i=0;i<10;i++){
			gc.drawLine(context.med.zoom.getDisplayRect().x-5,context.med.zoom.getDisplayRect().height/10*i+context.med.zoom.getDisplayRect().y,
					context.med.zoom.getDisplayRect().x,context.med.zoom.getDisplayRect().height/10*i+context.med.zoom.getDisplayRect().y);
			gc.drawLine(context.med.zoom.getDisplayRect().width/10*i+context.med.zoom.getDisplayRect().x,context.med.zoom.getDisplayRect().height+context.med.zoom.getDisplayRect().y+5,
					context.med.zoom.getDisplayRect().width/10*i+context.med.zoom.getDisplayRect().x,context.med.zoom.getDisplayRect().height+context.med.zoom.getDisplayRect().y-5);
		}*/
	}
	
	public void paintImageData(GC gc) {
		gc.drawImage(new Image(null, context.getImageData()),
				context.med.zoom.getDisplayRect().x,
				context.med.zoom.getDisplayRect().y);
	
	}
	
	
	private void paint(GC gc) {
		paintImageData(gc);
		paintAxes(gc);
		paintPalette(gc);
		
	}
	
	private void paintPalette(GC gc) {
		context.med.palette.paintColorBar(gc);		
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
		
	}

	public void redraw() {
		canvas.setSize(context.med.zoom.getCanvasRect().width,context.med.zoom.getCanvasRect().height);
		canvas.redraw();
	}

	public void dispose() {
		canvas.dispose();
	}
			   
	
}
