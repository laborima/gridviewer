package configManagerView;

import observer.Observer;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import palette.paletteDialog;
import Context.Context;

public class SelectedAreaPage extends WizardPage implements Observer{

	
	  

	private Text imin = null;
	private Text jmin = null;
	private Text imax = null;
	private Text jmax = null;
	private Context context = null;


	private configManagerData data;
	
	
	public SelectedAreaPage() {
		   super("wizardPage");
		   setTitle("Select Area Page");
		   setDescription("Select here the area that you want to extract");
		   data = configManagerData.getInstance();
		   setPageComplete(false);
		   
	}

	public void createControl(Composite parent) {
		
		final Composite container = new Composite(parent, SWT.NULL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 10;
		gridLayout.verticalSpacing = 9;
		createContext(container);
		container.setLayout(gridLayout);
		GridData gridData  = new GridData(GridData.FILL_HORIZONTAL);
	    gridData.heightHint = 34;
		
		createLine(container, 10);
		
		Label label = new Label(container, SWT.NONE);
		label.setText("imin: ");	
		imin = new Text(container, SWT.BORDER);
	    imin.setLayoutData(gridData);
	   
	    label = new Label(container, SWT.NONE);
		label.setText("jmin: ");
		jmin = new Text(container, SWT.BORDER);
		jmin.setLayoutData(gridData);
		
		label = new Label(container, SWT.NONE);
		label.setText("imax: ");
		imax = new Text(container, SWT.BORDER);
		imax.setLayoutData(gridData);
	
		label = new Label(container, SWT.NONE);
		label.setText("jmax: ");
		jmax = new Text(container, SWT.BORDER);
		jmax.setLayoutData(gridData);
		
		Button zoomButton = new Button(container, SWT.PUSH);
		zoomButton.setText("Zoom Out");
		zoomButton.setLayoutData(gridData);
		zoomButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {   
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {       
				context.med.zoom.setSelectedArea(new Rectangle(data.getJpizoom()-1,
						data.getJpjzoom()-1,data.getJpiglo()+data.getJpizoom()-2,data.getJpjglo()+data.getJpjzoom()-2));
				context.med.zoom.computeZoom();
                      context.readData();
					  context.redraw();
			}
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
				
			}
		});
		Button toolItemPalette = new Button(container, SWT.PUSH);
		toolItemPalette.setText("pallette");
		toolItemPalette.setLayoutData(gridData);
		toolItemPalette
				.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						 paletteDialog dial = new paletteDialog(container.getShell(),context.med.palette);
                         dial.show();
                        while (!dial.isDisposed())
                                if (!Display.getCurrent().readAndDispatch())
                                        Display.getCurrent().sleep();
                     if(context!=null && context.getImageData()!=null)                 
                    	 context.updatePalette();
					}
					public void widgetDefaultSelected(
							org.eclipse.swt.events.SelectionEvent e) {
					}
				});
		
		
		 
		

		createLine(container, 10);
		
		 setControl(container);
		
	}

	
	private void createContext(Composite container) {
		GridData gridData = new GridData();
		gridData.heightHint = 512;
		gridData.widthHint = 722;
		gridData.horizontalSpan = 10;
		context = new Context(container, SWT.NONE);
		context.setLayoutData(gridData);
		context.registerInterest(this);
		context.med.zoom.setCanvasRect(new Rectangle(0,0,722,512));
		context.med.zoom.setDisplayRect(new Rectangle(0,0,720,510));
		context.med.zoom.setMargin_left(1);
		context.med.zoom.setMargin_top(1);
		context.med.zoom.setMargin_bottom(2);
		//context.setIntroView();
	}

	public void sendNotify(String s) {
		if(s!=null)
		{
			String tmp[] = s.split("/");
			int timax = Integer.parseInt(tmp[2]) + Integer.parseInt(tmp[0])+1;
			int tjmin = (Integer.parseInt(tmp[1]) - Integer.parseInt(tmp[3]))+1;
			int timin = Integer.parseInt(tmp[0])+1;
			int tjmax = Integer.parseInt(tmp[1])+1;
			imin.setText(Integer.toString(timin));
			jmin.setText(Integer.toString(tjmin));
			imax.setText(Integer.toString(timax));
			jmax.setText(Integer.toString(tjmax));
			filldata();
			// set Data here
			
		}
		
	}
	
	public void filldata(){
		data.setImin(Integer.parseInt(imin.getText()));
		data.setImax(Integer.parseInt(imax.getText()));
		data.setJmin(Integer.parseInt(jmin.getText()));
		data.setJmax(Integer.parseInt(jmax.getText()));
	}
	
	private void createLine(Composite parent, int ncol)
    {
    
        Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL |
                SWT.BOLD);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = ncol;
        line.setLayoutData(gridData);
    }

	public void onEnterPage() {
			context.med.var.loadFile(data.getFilein_bathy_parent());
			context.med.var.readVar(data.getVarName());
			context.med.zoom.setSelectedArea(new Rectangle(data.getJpizoom()-1,
					data.getJpjzoom()-1,data.getJpiglo()+data.getJpizoom()-2,data.getJpjglo()+data.getJpjzoom()-2));
			context.med.zoom.computeZoom();
			context.readData();
			context.setIJBoxView();
			context.redraw();
			setPageComplete(true);
	}
	
	
}
