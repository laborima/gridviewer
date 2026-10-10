package arrayData;
import ucar.ma2.Array;


public abstract class arrayData {

	protected Array array;
	
	public arrayData(Array array) {
		this.array = array;
	}
	
	public abstract double getValue(int x,int y);

	public abstract int getSyzeX();

	public abstract int getSyzeY();

}
