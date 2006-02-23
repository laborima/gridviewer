package PlotStrategy;

import java.io.IOException;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;

public class opaConvention extends CoordSysBuilder {

	// return true if is OPA file
	  public static boolean isMine( NetcdfFile ncfile) {
	    // not really sure until we can examine more files
	    //String s =  ncfile.findAttValueIgnoreCase(null, "sensor_name", "none");
	    return true;//s.equalsIgnoreCase("CRAFT/NEXRAD");
	  }

	 public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
		    this.conventionName = "GDT 1.3";
		    NcMLReader.wrapNcMLresource( ncDataset, CoordSysBuilder.resourcesDir+"OPA.ncml", cancelTask);
		  }
	
}
