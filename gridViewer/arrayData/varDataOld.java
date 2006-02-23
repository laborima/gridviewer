package arrayData;

import java.io.IOException;
import java.util.List;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import Context.Mediator;

/**
 * @author   mlaborie
 */
public class varDataOld {

	
    private NetcdfFile ncFile = null;
	private Variable ncVar = null;
	/**
	 * @uml.property  name="dim"
	 */
	private int dim = 0;
	/**
	 * @uml.property  name="depth"
	 */
	private int depth = 0;
	/**
	 * @uml.property  name="time"
	 */
	private int time = 0;
	Mediator med;
	
	public varDataOld(Mediator med) {
		super();
		this.med = med;
		//CoordSysBuilder.registerConvention("GDT 1.3",opaConvention.class);
	}
	
	public void finalize() {
		try {
				if(ncFile != null)
				{
					ncFile.close();
				    System.out.println("Close netcdf File");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
	}
	
	public void loadFile(String filename) {
		try {
			if(ncFile != null)
				ncFile.close();
			ncFile = NetcdfFile.open(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void readVar(String varName){
		ncVar = ncFile.findVariable(varName);
		dim =  ncVar.getDimensions().size();
		med.palette.setMaskValue(0);
		if(ncVar.findAttribute("_FillValue")!=null){
			med.palette.setRange(ncVar.findAttribute("valid_min").getNumericValue().doubleValue()
					,ncVar.findAttribute("valid_max").getNumericValue().doubleValue());
			med.palette.setMaskValue(ncVar.findAttribute("_FillValue").getNumericValue().doubleValue());
		} else if(!med.palette.setDefaultRange(varName)){
			med.palette.setRange(-1,1);
		}
		med.zoom.setMaxRange(new Point(ncVar.getShape()[dim-1]-2,ncVar.getShape()[dim-2]-1));
		med.zoom.setSelectedArea(new Rectangle(0,0,ncVar.getShape()[dim-1]-1,ncVar.getShape()[dim-2]-1));
		med.zoom.computeZoom();
		this.depth = 0;
	    this.time = 0;
	}

	/**
	 * @param depth  The depth to set.
	 * @uml.property  name="depth"
	 */
	public boolean setDepth(int z) {
		if(dim>2 && z>=0 && z<ncVar.getShape()[1]){
		 this.depth = z;
		 return true;
		}
		return false;
	}

	public List getVariables() {
		return ncFile.getVariables();
	}

	public String getVarInfo(){
	  String s="";
	  for(int i=0; i<ncVar.getAttributes().size();i++){
		  s+=ncVar.getAttributes().get(i).toString()+"\n";
		}
		return s;
	}
	

	/**
	 * @return  Returns the dim.
	 * @uml.property  name="dim"
	 */
	public int getDim() {
		return dim;
	}

	public int getVarSize(int i) {
		return ncVar.getShape()[i];
	}

	public boolean setTimeCounter(int t) {
		if(dim>2 && t>=0 && t<ncVar.getShape()[0]){
			 this.time = t;
			 return true;
			}
			return false;
	}




public int getDimensions() {
	return dim;
}


public Array read(String request) throws IOException, InvalidRangeException {
		return ncVar.read(request);
}

/**
 * @return  Returns the time.
 * @uml.property  name="time"
 */
public int getTime() {
	return time;
}

/**
 * @return  Returns the depth.
 * @uml.property  name="depth"
 */
public int getDepth() {
	return depth;
}

public NetcdfFile getFile() {
	return ncFile;
}

public Variable  getVar() {
	return ncVar;
}
   

}
