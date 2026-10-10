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

public class introDecor extends Decorator {
 
	private Canvas canvas;
	
	public introDecor(Context context) {
		super(context);
		createCanvas();
		// TODO Auto-generated constructor stub
	}

	
	
	private void paint(GC gc) {
		Image image = new Image(null,this.getClass().getResourceAsStream("logo.png"));
		gc.drawImage(image,72,20);
		gc.drawText("Version 1.0  By M. Laborie",330,260);
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
		canvas.redraw();
	}



	public void dispose() {
		canvas.dispose();
	}	   
	   
	
}
