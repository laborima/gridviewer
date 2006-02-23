package palette;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author   mlaborie
 */
public class paletteDialog {

	
	private Shell sShell = null;  //  @jve:decl-index=0:visual-constraint="10,10"
	private Table table = null;
	private Palette pal = null;
	


	public paletteDialog(Shell parent,Palette pal) {
		super();
		this.pal = pal;
		createSShell(parent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * This method initializes sShell
	 */
	private void createSShell(Shell parent) {
		sShell = new Shell(parent);
		sShell.setText("Shell");
		sShell.setLayout(new FillLayout());
		sShell.setFocus();
		createTable();
		sShell.setSize(new org.eclipse.swt.graphics.Point(540,600));
	}

	
	
	
	
		
	
	
	public void show(){
	  sShell.open();
	}

	/**
	 * This method initializes table	
	 *
	 */
	private void createTable() {
		table = new Table(sShell, SWT.NONE);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.addMouseListener(new MouseListener() {

			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void mouseDown(MouseEvent e) {
				// TODO Auto-generated method stub
				pal.readColorMap((String) table.getSelection()[0].getData());
				
			}

			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub
            	sShell.dispose();
			}});
		
			
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(Palette.class.getResource("listPalette.txt").openStream()));
	        String str;
	        while ((str = in.readLine()) != null) {
	            	TableItem tab = new TableItem(table,SWT.NONE);
					tab.setImage(pal.readPaletteImage(str));
					tab.setData(str);
	        }
	        in.close();
	    } catch (IOException e) {
	    }

			    		

	}

	public boolean isDisposed() {
		// TODO Auto-generated method stub
		return sShell.isDisposed();
	}

	
	
}
