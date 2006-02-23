package Context;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * @author   mlaborie
 */
public class zoomManager {

	
	private int margin_top = 5;
	private int margin_left = 5;
	private int margin_bottom = 70;
	
	private Mediator med;
	/**
	 * @uml.property  name="selectedArea"
	 */
	private Rectangle selectedArea = null;
	/**
	 * @uml.property  name="displayRect"
	 */
	private Rectangle displayRect = null;
	private Point sizePic = null;

	/**
	 * @uml.property  name="canvasRect"
	 */
	private Rectangle canvasRect = null;
	/**
	 * @uml.property  name="maxRange"
	 */
	private Point maxRange = null;
	private long zoomIn  = 1;
	private long zoomOut = 1;
	/**
	 * @uml.property  name="zoom"
	 */
	private double zoom  = 1;
	private boolean section = false;
	private boolean debug = false;
	

	public zoomManager(Mediator med) {
		super();
		this.med = med;
		sizePic = new Point(720,530);
		displayRect = new Rectangle(margin_left,margin_top,sizePic.x,sizePic.y);
		canvasRect = new Rectangle(0,0,760,600);
		selectedArea = new Rectangle(0,0,0,0);
		// TODO Auto-generated constructor stub
	}

	public Point toData(Point p){
		return new Point((p.x-displayRect.x)/sizePixel()*getStride() + selectedArea.x,
				invertY(p.y-displayRect.y)/sizePixel()*getStride() + selectedArea.y);
	}
	
	public Point toDisplay(Point p){
		return new Point((p.x - selectedArea.x)*sizePixel()/getStride()+displayRect.x,
				invertY((p.y - selectedArea.y)*sizePixel()/getStride())+displayRect.y);
	}
	
	public Rectangle toData(Rectangle r){
		return new Rectangle((r.x-displayRect.x)/sizePixel()*getStride() + selectedArea.x,
				invertY(r.y-displayRect.y)/sizePixel()*getStride() + selectedArea.y,
				r.width/sizePixel()*getStride(),r.height/sizePixel()*getStride());
	}
	
	public Rectangle toDisplay(Rectangle r){
		return new Rectangle((r.x - selectedArea.x)*sizePixel()/getStride()+displayRect.x,
				invertY((r.y - selectedArea.y)*sizePixel()/getStride())+displayRect.y,
		              r.width*sizePixel()/getStride(),r.height*sizePixel()/getStride());
	}
	
	
	public Point paintCoord(int x,int y){
			return new Point(x*sizePixel()+displayRect.x,invertY(y*sizePixel())+displayRect.y);
	}
	
	public Point pixelCoord(int x,int y){
		return new Point(x*sizePixel(),invertY(y*sizePixel()));
    }


	/**
	 * @return  Returns the canvasRect.
	 * @uml.property  name="canvasRect"
	 */
	public Rectangle getCanvasRect() {
		return canvasRect;
	}

	/**
	 * @param canvasRect  The canvasRect to set.
	 * @uml.property  name="canvasRect"
	 */
	public void setCanvasRect(Rectangle canvasRect) {
		this.canvasRect = canvasRect;
	}

	private int invertY(int y){
		return displayRect.height - y - sizePixel();
	}
	
	/**
	 * @return  Returns the displayRect.
	 * @uml.property  name="displayRect"
	 */
	public Rectangle getDisplayRect() {
		return displayRect;
	}

	/**
	 * @param displayRect  The displayRect to set.
	 * @uml.property  name="displayRect"
	 */
	public void setDisplayRect(Rectangle displayRect) {
		this.displayRect = displayRect;
	}

	/**
	 * @return  Returns the selectedArea.
	 * @uml.property  name="selectedArea"
	 */
	public Rectangle getSelectedArea() {
		return selectedArea;
	}

	/**
	 * @param selectedArea  The selectedArea to set.
	 * @uml.property  name="selectedArea"
	 */
	public void setSelectedArea(Rectangle selectedArea) {
		this.selectedArea = selectedArea;
		if(selectedArea.width - selectedArea.x == 0 || selectedArea.height - selectedArea.y == 0)
			section = true;
		else 
			section = false;
	}
	
	public void computeZoom(){
		
	displayRect.width  = sizePic.x;
	displayRect.height = sizePic.y;
		
		
	if(selectedArea.width - selectedArea.x == 0  ) //YZ case
	{
		zoom = (double)(selectedArea.height-selectedArea.y)/(double)(displayRect.width);
		zoomIn = Math.round(1/zoom);
		zoomOut = Math.round(zoom);
		 
		displayRect.width = Math.min(getDataSyze().y*sizePixel(),canvasRect.width-margin_left*2);
		displayRect.x = (canvasRect.width - displayRect.width)/2;
	} else if ( selectedArea.height - selectedArea.y == 0 ) //XZ case
	{
		zoom = (double)(selectedArea.width-selectedArea.x)/(double)(displayRect.width);
		zoomIn = Math.round(1/zoom);
		zoomOut = Math.round(zoom);
		 
		 displayRect.width = Math.min(getDataSyze().x*sizePixel(),canvasRect.width-margin_left*2);
		 displayRect.x = (canvasRect.width - displayRect.width)/2;
	} //Normal case
		else{
	
		 zoom = Math.max((double)(selectedArea.height-selectedArea.y)/(double)(displayRect.height),
				 (double)(selectedArea.width-selectedArea.x)/(double)(displayRect.width));
		
		 if(zoom-0.4>Math.round(zoom))
			 zoom++; //Cheap trick to round
		 zoomIn = Math.round(1/zoom);
		 zoomOut = Math.round(zoom);
		 
		 displayRect.width = Math.min(getDataSyze().x*sizePixel(),sizePic.x);
		 displayRect.x = (canvasRect.width - displayRect.width)/2;
		 displayRect.height = Math.min(getDataSyze().y*sizePixel(),sizePic.y);
		 displayRect.y = (canvasRect.height - (displayRect.height+margin_bottom))/2+margin_top;
		}
	
		if(debug ){
		  System.out.println("Zoom:"+zoom);
		  System.out.println("ZoomIn:"+zoomIn);
		  System.out.println("ZoomOut:"+zoomOut);
		}
		 //displayRect.y = (canvasRect.height - displayRect.height)/2;
		 
		 //Readjust Data Syze;
		 //setDataSyze(new Point(displayRect.width/sizePixel(),displayRect.height/sizePixel()) );

	
	}
	
	
	public int sizePixel(){
		return (int) Math.max(zoomIn,1);
	}
	
	/**
	 * @return  Returns the zoom.
	 * @uml.property  name="zoom"
	 */
	public double getZoom() {
		return zoom;
	}	
	
	private Point getDataSyze(){
			return new Point((selectedArea.width-selectedArea.x+1)/getStride(),(selectedArea.height-selectedArea.y+1)/getStride());
	}
	
	public void setDataSyze(Point p){
		if(p.x<maxRange.x && p.y<maxRange.y){
		  selectedArea.width = p.x*getStride() + selectedArea.x - 1;
		  selectedArea.height  = p.y*getStride() + selectedArea.y - 1;
		  if(selectedArea.width > maxRange.x)
			  selectedArea.x = Math.max(0, selectedArea.x - (selectedArea.width - maxRange.x));
		  if(selectedArea.height > maxRange.y)
			  selectedArea.y = Math.max(0, selectedArea.y - (selectedArea.height - maxRange.y));
		}
		selectedArea.width = Math.min(maxRange.x-1,selectedArea.width);
		selectedArea.height  = Math.min(maxRange.y-1,selectedArea.height);
	}

	public int getStride() {
		return (int) Math.max(zoomOut,1);
	}

	/**
	 * @return  Returns the maxRange.
	 * @uml.property  name="maxRange"
	 */
	public Point getMaxRange() {
		return maxRange;
	}

	/**
	 * @param maxRange  The maxRange to set.
	 * @uml.property  name="maxRange"
	 */
	public void setMaxRange(Point maxRange) {
		this.maxRange = maxRange;
	}

	public Rectangle getGridRect(){
		return new Rectangle(selectedArea.x, selectedArea.y,
				selectedArea.width-selectedArea.x+1,selectedArea.height-selectedArea.y+1);
	}

	public Mediator getMediator(){
		return med;
	}

	public boolean isSections() {
		return section;
	}

	public int getMargin_bottom() {
		return margin_bottom;
	}

	public void setMargin_bottom(int margin_bottom) {
		this.margin_bottom = margin_bottom;
	}

	public int getMargin_left() {
		return margin_left;
	}

	public void setMargin_left(int margin_left) {
		this.margin_left = margin_left;
	}

	public int getMargin_top() {
		return margin_top;
	}

	public void setSizePic(Point p){
		this.sizePic = p;
	}
	
	public void setMargin_top(int margin_top) {
		this.margin_top = margin_top;
	}

	public void computeDisplay() {
	  sizePic = new Point(canvasRect.width - 2*margin_left , canvasRect.height -margin_top -margin_bottom); 		
	}

	
}
