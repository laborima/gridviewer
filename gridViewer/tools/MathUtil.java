package tools;

public class MathUtil {


	public static double round(double d){
		int count = 0;
		while(Math.abs(d)<=100 && d!=0){
			d*=10;
			count++;
		}
		return (double)Math.round(d)/(double)(Math.pow(10,count));
	}
}
