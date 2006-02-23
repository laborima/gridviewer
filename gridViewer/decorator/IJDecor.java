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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Tracker;

import Context.Context;
import Context.listPoint;
import Context.procLayout;
import arrayData.arrayData;
import arrayData.arrayDataFactory;

public class IJDecor extends Decorator {
 
	private Canvas canvas;
	private procLayout procs;
    private listPoint points; 
	//private Rectangle selectedRect = null;
	
	public IJDecor(Context context) {
		super(context);
		createCanvas();
		procs = new procLayout(context.med.zoom);
		points =new listPoint(context.med.zoom);
		// TODO Auto-generated constructor stub
	}

	private void paintAxes(GC gc){ 
		gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(context.med.zoom.getDisplayRect().x-1,
				context.med.zoom.getDisplayRect().y-1,
				context.med.zoom.getDisplayRect().width+1,
				context.med.zoom.getDisplayRect().height+1);
		
		/*for(int i=0;i<10;i++){
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
		points.paint(gc);
		procs.paint(gc);
		
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
		
		 canvas.addMouseListener(new MouseAdapter() {
	            public void mouseDown(MouseEvent e) {
	            	if(e.button == 1)
	            	{  
	            	Rectangle zoomRect = null;
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
					if((((p2.x-p1.x) >1 && (p2.y-p1.y) >1) || (context.med.var.getDim()>2 && ((p2.x-p1.x) >1 || (p2.y-p1.y) >1))) 
							&& context.med.zoom.getDisplayRect().contains(zoomRect.x,zoomRect.y) &&
							context.med.zoom.getDisplayRect().contains(zoomRect.width+zoomRect.x,zoomRect.height+zoomRect.y)) {
		                context.med.zoom.setSelectedArea(new Rectangle(p1.x,p1.y,p2.x,p2.y));
		                context.med.zoom.computeZoom();
	                    context.med.palette.resetNewRange();
	                    context.readData();
	                    context.getObserver().sendNotify(null);
	                }
	                redraw();
	            	} else {
	            		createMenu(e.x,e.y);
	            	}
	            	
	            }

		 });
	}

	public void redraw() {
		canvas.setSize(context.med.zoom.getCanvasRect().width,context.med.zoom.getCanvasRect().height);
		canvas.redraw();
	}

	public void dispose() {
		canvas.dispose();
	}
		
	private void createMenu(final int x,final int y) {
	    Menu menu = new Menu(context.getShell(), SWT.POP_UP);
	    
	    MenuItem pointItem = new MenuItem(menu, SWT.PUSH);
        pointItem.setText("Print point coord");
        pointItem.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
        	Point p1 = context.med.zoom.toData(new Point(x,y));
            //System.out.println("Point : "+p1.x+","+p1.y);
            MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(), 
        	          SWT.ICON_INFORMATION);
        	        mb.setMessage("Point : "+p1.x+","+p1.y);
        	        mb.open();
          }
        });
        
        
        MenuItem valueItem = new MenuItem(menu, SWT.PUSH);
        valueItem.setText("Print point value");
        valueItem.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            Point p1 = context.med.zoom.toData(new Point(x,y));
            Point p2 = new Point(0,0);
            p2.x = (p1.x - context.med.zoom.getSelectedArea().x)
            /context.med.zoom.getStride();
            p2.y = (p1.y -context.med.zoom.getSelectedArea().y)
            /context.med.zoom.getStride();
            arrayDataFactory arrayFact = new arrayDataFactory();
    		arrayData array = arrayFact.getArrayData(context.med);
           //System.out.println("Point : "+p1.x+","+p1.y+" Value: "+array.getValue(p1.x,p1.y));	
           MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(), 
     	          SWT.ICON_INFORMATION);
     	        mb.setMessage("Point : "+p1.x+","+p1.y+"\n Value: "+array.getValue(p2.x,p2.y));
     	        mb.open();          
          }
        });
        
      /*  MenuItem LonLatItem = new MenuItem(menu, SWT.PUSH);
        LonLatItem.setText("Print LonLat coord");
        LonLatItem.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
        Point p1 = context.med.zoom.toData(new Point(x,y));
	    VariableDS var = (VariableDS) context.med.var.getVar();
	    var.enhance();
	    CoordinateSystem  c = (CoordinateSystem) var.getCoordinateSystems().get(0);
	     System.out.println(p1.x+" "+p1.y);
	     System.out.println("lat:"+((LatLonProjection)c.getProjection()).projToLatLon(p1.x,p1.y).getLatitude()+" lon:"+
	    		 ((LatLonProjection)c.getProjection()).projToLatLon(p1.x,p1.y).getLongitude() );
          }
        });*/
	    
        canvas.setMenu(menu);
        menu.setVisible(true);
}

	public procLayout getProcLayout() {
		return procs;
	}

	public listPoint getPoints() {
		return points;
	}

	   
	   
	
}
