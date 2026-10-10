package configManagerView;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class mainView{

	

	public mainView() {
		super();
		// TODO Auto-generated constructor stub
	}


	public static void main(String[] args) {

		Display display = new Display();
	    Shell shell = new Shell(display);
	    WizardDialog dlg = new WizardDialog(shell, new configManagerWizard());
	    dlg.open();
	    display.dispose();

	}

	
	
}
