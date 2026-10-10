package decorator;

import Context.Context;

/**
 * @author   mlaborie
 */
public abstract class Decorator{

	protected Context context;
	
	public Decorator(Context context){
		this.context = context;
	}
	
	public abstract void redraw();

	public abstract void dispose();
	
}
