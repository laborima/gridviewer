package arrayData;
import ucar.ma2.Array;
import ucar.ma2.Index;


public class FourDimArrayDataZY extends arrayData {


	public FourDimArrayDataZY(Array array) {
		super(array);
		// TODO Auto-generated constructor stub
	}

	public double getValue(int x,int y){
		Index in = array.getIndex();
		return array.getDouble(in.set(0,y,x,0));
	}

	public int getSyzeX() {
			return array.getShape()[2];
	}

	public int getSyzeY() {
	  return array.getShape()[1];
	}

}
