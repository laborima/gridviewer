package Context;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * @author   mlaborie
 */
public class listPoint {

	private List pointList = null;
	private zoomManager zoom = null;
	
	public void paint(GC gc){
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		for(int i=0;i<pointList.size();i++){
			 Point p = (Point) pointList.get(i);
			 if(zoom.getGridRect().contains(p))
			 {
				 gc.fillRectangle(zoom.toDisplay(p).x,zoom.toDisplay(p).y,Math.max(zoom.sizePixel(),5)
						 ,Math.max(zoom.sizePixel(),5));
			 }
		}
	}
	
	public void addPoint(Point p){
		if(pointList.indexOf(p)==-1)
			pointList.add(p);
	}
	
	public void clearList(){
		pointList.clear();
	}
	
	public listPoint(zoomManager zoom){
		super();
		this.zoom = zoom;
		pointList=new ArrayList();
	}

	public void loadFile(String filename) {
		try {
	        BufferedReader in = new BufferedReader(new FileReader(filename));
	        String str;
	        String[] s;
	        while ((str = in.readLine()) != null) {
	            str.trim();
	        	s = str.split(" +");
	           addPoint(new Point(Integer.parseInt(s[0]),Integer.parseInt(s[1])));
	    }

	        in.close();
	    } catch (IOException e) {
	    }
		
	}

}
