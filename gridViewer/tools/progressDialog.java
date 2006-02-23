package tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 */

/**
 * @author  mlaborie
 */
public class progressDialog {

	/**
	 * @uml.property  name="sShell"
	 */
	private Shell sShell = null;  //  @jve:decl-index=0:visual-constraint="153,106"
	/**
	 * @uml.property  name="progressBar"
	 */
	private ProgressBar progressBar = null;


	public progressDialog() {
		super();
		createSShell();
		// TODO Auto-generated constructor stub
	}


	/**
	 * This method initializes sShell
	 */
	private void createSShell() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		sShell = new Shell();
		sShell.setText("Loading... Please be patient!!!");
		sShell.setLayout(new GridLayout());
		sShell.setSize(new org.eclipse.swt.graphics.Point(389,56));
		progressBar = new ProgressBar(sShell, SWT.NONE);
		progressBar.setLayoutData(gridData);
		progressBar.setSize(389,56);
	}


	/**
	 * @return  Returns the progressBar.
	 * @uml.property  name="progressBar"
	 */
	public ProgressBar getProgressBar() {
		return progressBar;
	}


	/**
	 * @return  Returns the sShell.
	 * @uml.property  name="sShell"
	 */
	public Shell getSShell() {
		return sShell;
	}

  
}
