package arrayData;
import java.io.IOException;

import Context.Mediator;
import ucar.ma2.InvalidRangeException;


public class arrayDataFactory {
	
	public arrayData getArrayData(Mediator med)
	{
		String request = null;
		try 
		{

			switch(med.var.getDim())
			{
				case 4:
					request = new String(""+med.var.getTime()+","
							+0+":"+(med.var.getVarSize(1)-1)+","
							+med.zoom.getSelectedArea().y+":"+med.zoom.getSelectedArea().height+":"+med.zoom.getStride()+","
							+med.zoom.getSelectedArea().x+":"+med.zoom.getSelectedArea().width+":"+med.zoom.getStride()+"");
					if(med.zoom.getSelectedArea().width - med.zoom.getSelectedArea().x==0){
						   System.out.println("YZ section:"+request+"");
						   return new FourDimArrayDataZY(med.var.read(request));
					} else if(med.zoom.getSelectedArea().height - med.zoom.getSelectedArea().y==0){
						   System.out.println("XZ section:"+request+"");
						   return new FourDimArrayDataZX(med.var.read(request));
					}else {
					request = new String(""+med.var.getTime()+","+med.var.getDepth()+","
						+med.zoom.getSelectedArea().y+":"+med.zoom.getSelectedArea().height+":"+med.zoom.getStride()+","
						+med.zoom.getSelectedArea().x+":"+med.zoom.getSelectedArea().width+":"+med.zoom.getStride()+"");
					System.out.println("Selected med.zoom.getSelectedArea():"+request+"");
				   return new FourDimArrayData(med.var.read(request));
					}
				case 3:
					request = new String( 0+":"+(med.var.getVarSize(0)-1)+","
							+med.zoom.getSelectedArea().y+":"+med.zoom.getSelectedArea().height+":"+med.zoom.getStride()+","
							+med.zoom.getSelectedArea().x+":"+med.zoom.getSelectedArea().width+":"+med.zoom.getStride()+"");
					if(med.zoom.getSelectedArea().width - med.zoom.getSelectedArea().x==0){
						   System.out.println("YZ section:"+request+"");
						   return new ThreeDimArrayDataZY(med.var.read(request));
					} else if(med.zoom.getSelectedArea().height - med.zoom.getSelectedArea().y==0){
						   System.out.println("XZ section:"+request+"");
						   return new ThreeDimArrayDataZX(med.var.read(request));
					}else {
					request = new String(""+med.var.getTime()+","
						+med.zoom.getSelectedArea().y+":"+med.zoom.getSelectedArea().height+":"+med.zoom.getStride()+","
						+med.zoom.getSelectedArea().x+":"+med.zoom.getSelectedArea().width+":"+med.zoom.getStride()+"");
					//System.out.println("Selected med.zoom.getSelectedArea():"+request+"");
					return new ThreeDimArrayData(med.var.read(request));
					}
				case 2:
					request = new String(""
						 +med.zoom.getSelectedArea().y+":"+med.zoom.getSelectedArea().height+":"+med.zoom.getStride()+","
						+med.zoom.getSelectedArea().x+":"+med.zoom.getSelectedArea().width+":"+med.zoom.getStride()+"");
					//System.out.println("Selected med.zoom.getSelectedArea():"+request+"");
					return new TwoDimArrayData(med.var.read(request));
				default:
					System.out.println("Bad dimension for ncdf variable");
			}
		}
		catch (InvalidRangeException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		catch (NullPointerException e) 
		{
			e.printStackTrace();
		}
	 return null;
	}

}
