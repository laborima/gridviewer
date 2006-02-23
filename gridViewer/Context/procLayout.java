package Context;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class procLayout {

	public class proc{
	  public Rectangle rect = null;
	  public int num = 0;
	  
	  public proc(Rectangle r, int n){
		  this.rect = r;
		  this.num = n;
	  }
	  
	}
	
	
	private List rectList = null;
	private zoomManager zoom = null;
	
	
	public procLayout(zoomManager zoom) {
		super();
		this.zoom = zoom;
		// TODO Auto-generated constructor stub
		rectList=new ArrayList();
	}
	
	public void addProc(proc p){
		if(rectList.indexOf(p)==-1)
			rectList.add(p);
	}
	
	public void paint(GC gc){
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
		gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		for(int i=0;i<rectList.size();i++){
			 proc p = (proc) rectList.get(i);
			     gc.setClipping(zoom.getDisplayRect());
				 gc.drawRectangle(zoom.toDisplay(p.rect));
				 gc.drawText(Integer.toString(p.num),zoom.toDisplay(p.rect).x+2,zoom.toDisplay(p.rect).y+2+zoom.toDisplay(p.rect).height);
		}
	}
	
	public void clearList(){
		rectList.clear();
	}
	
	public void loadFile(String filename) {
		try {
	        BufferedReader in = new BufferedReader(new FileReader(filename));
	        String str;
	        String[] s;
	        while ((str = in.readLine()) != null) {
	       
	        	str.trim();
	        	s = str.split(" +");
	        	if(s.length>9){
	            addProc(new proc(new Rectangle(Integer.parseInt(s[8]),Integer.parseInt(s[9]),
	        		   Integer.parseInt(s[2]),-Integer.parseInt(s[3])),Integer.parseInt(s[1])));
	        	}
	        }

	        in.close();
	    } catch (IOException e) {
	    }
		
	}


}
