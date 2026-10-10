package arrayData;
import ucar.ma2.Array;
import ucar.ma2.Index;


public class TwoDimArrayData extends arrayData {


	public TwoDimArrayData(Array array) {
		super(array);
		// TODO Auto-generated constructor stub
	}

	public double getValue(int x,int y){
		Index in = array.getIndex();
		return array.getDouble(in.set(y,x));
	}

	public int getSyzeX() {
			return array.getShape()[1];
	}

	public int getSyzeY() {
	  return array.getShape()[0];
	}

}
