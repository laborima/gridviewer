package configManagerView;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class FilePage extends WizardPage{

	    
	
	
	    private Combo varSelect  = null;
	    private String path;
	    private Text   filein_bathy_parent;
	    private Text   filein_coord_parent;
	    private Text   fileout_bathy_child;
	    private Text   fileout_coord_child;
	    private Text   filein_bathy_child;
	    private Text   filein_coord_child;
	    private Combo   nmatch;
	    private Combo   nraff;
	    private Button interp;
	    private Button match;
	    private Button fill_closed;
	    private Button coord;
		private configManagerData data;
		private Button zoom = null;
		private Text  jpizoom = null;
		private Text  jpjzoom = null;
		private Text  jpiglo = null;
		private Text  jpjglo = null; 
		

	    public FilePage()
	    {
	        super("wizardPage");
	        setTitle("File Options");
	        setDescription("Enter here necessary file Name.");
	        data = configManagerData.getInstance();
	    }

	    public void createControl(Composite parent)
	    {
	        final Composite container = new Composite(parent, SWT.NULL);
	        Image img = new Image(Display.getDefault(),FilePage.class.getResourceAsStream("gnome-fs-directory.png"));
	        
	        
	        GridLayout layout = new GridLayout();
	        container.setLayout(layout);
	        layout.numColumns = 10;
	        layout.verticalSpacing = 9;

	        Button  openButton;
	 
	        
	        Label label = new Label(container, SWT.NULL);
	        label.setText("&Bathy in:");

	        filein_bathy_parent = new Text(container, SWT.BORDER | SWT.SINGLE);

	        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	        gd.heightHint = 34;
	        gd.horizontalSpan = 8;
	        GridData bd = new GridData();
	        bd.widthHint = 40;
	        bd.heightHint =34;
	        filein_bathy_parent.setLayoutData(gd);
	        filein_bathy_parent.addModifyListener(new ModifyListener()
	            {
	                public void modifyText(ModifyEvent e)
	                {
	                	NetcdfFile ncFile;
	                	try {
							ncFile = NetcdfFile.open(filein_bathy_parent.getText());
						
	                      
	                	varSelect.removeAll();
	                	for(int i=0; i<ncFile.getVariables().size();i++){
	                		Variable v =(Variable) ncFile.getVariables().get(i);
	                		if(v.getDimensions().size()>=2)
	                			varSelect.add(v.getName());
	                	}
	                		varSelect.setEnabled(true);
	                    ncFile.close();
	                    dialogChanged();
	                	} catch (IOException e1) {
	                		updateStatus("Not a valid Netcdf File");
	                		//e1.printStackTrace();
						}
	                }
	            });
	        
	        
	        openButton = new Button(container, SWT.NONE);
	        openButton.setImage(img);
			openButton.setLayoutData(bd);
			openButton.addListener(SWT.Selection, new Listener() {
			      public void handleEvent(Event e) {       
			        FileDialog dialog = new FileDialog(container.getShell(), SWT.OPEN);
					dialog.setFilterPath(path);
			        dialog.setFilterExtensions(new String[] { "*.nc","*" });
			        String filename = dialog.open();
			        if(filename != null){
			        	path = dialog.getFilterPath();
			        	filein_bathy_parent.setText(filename);
			        }
			      } });
			
			label = new Label(container, SWT.NULL);
	        label.setText("&Choose bathy var:");
	         
	        varSelect = new Combo(container, SWT.NONE);
			varSelect.setText("Variables");
			varSelect.setLayoutData(gd);
			varSelect.addSelectionListener(
					  new SelectionAdapter()
					  { 
						  public void widgetSelected(SelectionEvent e){
							  NetcdfFile ncFile;
			                	try {
									ncFile = NetcdfFile.open(filein_bathy_parent.getText());
									  Variable ncVar = ncFile.findVariable(varSelect.getText());
									  int dim =ncVar.getDimensions().size();
								      jpizoom.setText(Integer.toString(1));
								      jpjzoom.setText(Integer.toString(1));
								      jpiglo.setText(Integer.toString(ncVar.getShape()[dim-1]));
								      jpjglo.setText(Integer.toString(ncVar.getShape()[dim-2]));
								      ncFile.close();
					                    dialogChanged();
					                	} catch (IOException e1) {
					                		updateStatus("Not a valid Netcdf File");
										}
			                }
						  
					  });
			
			label = new Label(container, SWT.NULL);
			
			label = new Label(container, SWT.NULL);
	        label.setText("&Bathy out:");
			
	        fileout_bathy_child = new Text(container, SWT.BORDER | SWT.SINGLE);
	        fileout_bathy_child.setLayoutData(gd);
	        fileout_bathy_child.addModifyListener(new ModifyListener()
		            {
		                public void modifyText(ModifyEvent e)
		                {
		                	dialogChanged();
		                }
		            });
	        
	        openButton = new Button(container, SWT.NONE);
	        openButton.setImage(img);;
			openButton.setLayoutData(bd);
			openButton.addListener(SWT.Selection, new Listener() {
			      public void handleEvent(Event e) {       
			        FileDialog dialog = new FileDialog(container.getShell(), SWT.SAVE);
					dialog.setFilterPath(path);
			        dialog.setFilterExtensions(new String[] { "*.nc","*" });
			        String filename = dialog.open();
			        if(filename != null){
			        	path = dialog.getFilterPath();
			        	fileout_bathy_child.setText(filename);
			        }
			      } });
           
			
			
			createLine(container, layout.numColumns);
	      
			
			label = new Label(container, SWT.NULL);
	        label.setText("&Coord:");
	        
	        coord = new Button(container,SWT.CHECK);
	        coord.addListener(SWT.Selection, new Listener() {
			      public void handleEvent(Event e) {       
				        if(coord.getSelection()){
				        	dialogChanged();
				        	fileout_coord_child.setEnabled(true);
				        	if(!interp.getSelection()){
				        		filein_coord_child.setEnabled(true);
				        		filein_coord_parent.setEnabled(true);
				        	}
				        } else {
				        	dialogChanged();
				        	fileout_coord_child.setEnabled(false);	
				        	if(!interp.getSelection()){
				        		filein_coord_child.setEnabled(false);
				        		filein_coord_parent.setEnabled(false);
				        	}
				        }
				        }
				      } );
	        
	        label = new Label(container, SWT.NULL);
	        label.setLayoutData(gd);
			
	        label = new Label(container, SWT.NULL);
	        label.setText("&Coord in:");

	        filein_coord_parent = new Text(container, SWT.BORDER | SWT.SINGLE);

	        filein_coord_parent.setLayoutData(gd);
	        filein_coord_parent.setEnabled(false);
	        filein_coord_parent.addModifyListener(new ModifyListener()
	            {
	                public void modifyText(ModifyEvent e)
	                {
	                	dialogChanged();
	                }
	            });
	        
	        openButton = new Button(container, SWT.NONE);
	        openButton.setImage(img);
			openButton.setLayoutData(bd);
			openButton.addListener(SWT.Selection, new Listener() {
			      public void handleEvent(Event e) {       
			        FileDialog dialog = new FileDialog(container.getShell(), SWT.OPEN);
					dialog.setFilterPath(path);
			        dialog.setFilterExtensions(new String[] { "*.nc","*" });
			        String filename = dialog.open();
			        if(filename != null){
			        	path = dialog.getFilterPath();
			        	filein_coord_parent.setText(filename);
			        }
			      } });
	        
			label = new Label(container, SWT.NULL);
	        label.setText("&Coord out:");
			
	        fileout_coord_child = new Text(container, SWT.BORDER | SWT.SINGLE);
	        fileout_coord_child.setLayoutData(gd);
	        fileout_coord_child.setEnabled(false);
	        
	        openButton = new Button(container, SWT.NONE);
	        openButton.setImage(img);
			openButton.setLayoutData(bd);
			openButton.addListener(SWT.Selection, new Listener() {
			      public void handleEvent(Event e) {       
			        FileDialog dialog = new FileDialog(container.getShell(), SWT.SAVE);
					dialog.setFilterPath(path);
			        dialog.setFilterExtensions(new String[] { "*.nc","*" });
			        String filename = dialog.open();
			        if(filename != null){
			        	path = dialog.getFilterPath();
			        	fileout_coord_child.setText(filename);
			        }
			      } });
			
			
	        
			
	        createLine(container, layout.numColumns);
            
	        label = new Label(container, SWT.NULL);
	        label.setText("&Interp:");
	        
	        interp = new Button(container,SWT.CHECK);
	        interp.setSelection(true);
	        interp.addListener(SWT.Selection, new Listener() {
			      public void handleEvent(Event e) {       
				        if(!interp.getSelection()){
				        	dialogChanged();
				        	filein_bathy_child.setEnabled(true);
				        	if(coord.getSelection()){
				        		filein_coord_child.setEnabled(true);
				        		filein_coord_parent.setEnabled(true);
				        	}
				        } else {
				        	dialogChanged();
				        	filein_bathy_child.setEnabled(false);	
				        	filein_coord_child.setEnabled(false);
				        	if(coord.getSelection()){
				        		filein_coord_parent.setEnabled(false);
				        	}
				        }
				        }
				      } );
	        
	        label = new Label(container, SWT.NULL);
	        label.setLayoutData(gd);
	        
	        label = new Label(container, SWT.NULL);
	        label.setText("&Bathy child:");

	        filein_bathy_child = new Text(container, SWT.BORDER | SWT.SINGLE);

	        filein_bathy_child.setLayoutData(gd);
	        filein_bathy_child.setEnabled(false);
	        filein_bathy_child.addModifyListener(new ModifyListener()
	            {
	                public void modifyText(ModifyEvent e)
	                {
	                	dialogChanged();
	                }
	            });
	        
	        openButton = new Button(container, SWT.NONE);
	        openButton.setImage(img);
			openButton.setLayoutData(bd);
			openButton.addListener(SWT.Selection, new Listener() {
			      public void handleEvent(Event e) {       
			        FileDialog dialog = new FileDialog(container.getShell(), SWT.OPEN);
					dialog.setFilterPath(path);
			        dialog.setFilterExtensions(new String[] { "*.nc","*" });
			        String filename = dialog.open();
			        if(filename != null){
			        	path = dialog.getFilterPath();
			        	filein_bathy_child.setText(filename);
			        }
			      } });
	        
	        label = new Label(container, SWT.NULL);
	        label.setText("&Coord child:");

	        filein_coord_child = new Text(container, SWT.BORDER | SWT.SINGLE);

	        filein_coord_child.setLayoutData(gd);
	        filein_coord_child.setEnabled(false);
	        filein_coord_child.addModifyListener(new ModifyListener()
	            {
	                public void modifyText(ModifyEvent e)
	                {
	                	dialogChanged();
	                }
	            });
	        
	        
	        openButton = new Button(container, SWT.NONE);
	        openButton.setImage(img);
			openButton.setLayoutData(bd);
			openButton.addListener(SWT.Selection, new Listener() {
			      public void handleEvent(Event e) {       
			        FileDialog dialog = new FileDialog(container.getShell(), SWT.OPEN);
					dialog.setFilterPath(path);
			        dialog.setFilterExtensions(new String[] { "*.nc","*" });
			        String filename = dialog.open();
			        if(filename != null){
			        	path = dialog.getFilterPath();
			        	filein_coord_child.setText(filename);
			        }
			      } });
	        

			createLine(container, layout.numColumns);
	        
	        
	        match = new Button(container,SWT.CHECK);
	        match.setText("Match");
	       // match.setLayoutData(gd);
	        match.addListener(SWT.Selection, new Listener() {
			      public void handleEvent(Event e) {       
				        if(match.getSelection() == true){
				        	nmatch.setEnabled(true);
				        } else {
				        	nmatch.setEnabled(false);
				        }
				        }
				      } );
	       
	       

	        nmatch = new Combo(container, SWT.NONE);
	        nmatch.setLayoutData(gd);
	        nmatch.add("3");
			nmatch.add("5");
			nmatch.add("7");
			nmatch.select(0);
	        nmatch.setEnabled(false);
	        nmatch.addModifyListener(new ModifyListener()
	            {
	                public void modifyText(ModifyEvent e)
	                {
	                	//Test here file validity
	                	
	                }
	            });
	        label = new Label(container, SWT.NULL);
	        
	        
	        label = new Label(container, SWT.NULL);
	        label.setText("&Reffine Factor:");
	        
	        nraff  = new Combo(container, SWT.NONE);
	        nraff.add("3");
			nraff.add("5");
			nraff.add("7");
			nraff.select(0);
	        nraff.setLayoutData(gd);
	        
	        
	      
	       label = new Label(container, SWT.NULL);
	        
	        fill_closed = new Button(container,SWT.CHECK);
	        fill_closed.setText("fill_closed");
	        label = new Label(container, SWT.NULL);
	        label = new Label(container, SWT.NULL);
	        
	        
	        
	        createLine(container, layout.numColumns);

	        
	        zoom = new Button(container,SWT.CHECK);
			zoom.setText("Zoom");
			zoom.addListener(SWT.Selection, new Listener() {
				      public void handleEvent(Event e) {       
					        if(zoom.getSelection()){
					        	jpiglo.setEnabled(true);
					        	jpjglo.setEnabled(true);
					        	jpizoom.setEnabled(true);
					        	jpjzoom.setEnabled(true);
					        } else {
					        	jpiglo.setEnabled(false);
					        	jpjglo.setEnabled(false);
					        	jpizoom.setEnabled(false);
					        	jpjzoom.setEnabled(false);
					        }
					        }
					      } );
	        
	        GridData gridData  = new GridData(GridData.FILL_HORIZONTAL);
	        gridData.heightHint =30;
			label = new Label(container, SWT.NONE);
			label.setText("jpiglo: ");	
			jpiglo = new Text(container, SWT.BORDER);
			jpiglo.setLayoutData(gridData);
			jpiglo.setEnabled(false);
		   
		    label = new Label(container, SWT.NONE);
			label.setText("jpjglo: ");
			jpjglo = new Text(container, SWT.BORDER);
			jpjglo.setLayoutData(gridData);
			jpjglo.setEnabled(false);
			
			label = new Label(container, SWT.NONE);
			label.setText("jpizoom: ");
			jpizoom = new Text(container, SWT.BORDER);
			jpizoom.setLayoutData(gridData);
			jpizoom.setEnabled(false);
		
			label = new Label(container, SWT.NONE);
			label.setText("jpjzoom: ");
			jpjzoom = new Text(container, SWT.BORDER);
			jpjzoom.setLayoutData(gridData);
			jpjzoom.setEnabled(false);

			 label = new Label(container, SWT.NULL);
			
	        
	        setControl(container);
	       
	    }

	    public boolean canFlipToNextPage(){
	    	   if (getErrorMessage() != null) return false;
	    	   if (filein_bathy_parent.getText().length()!=0)
	    	        return true;
	    	    return false;
	    	}
	    
	   
	    

		private void dialogChanged()
	    {
	        if (filein_bathy_parent.getText().length() == 0)
	        {
	            updateStatus("Bathy In filename must be specify.");
	            return;
	        }
	        if (varSelect.getText().length()==0){
	        	updateStatus("select bathy var");
	        	return;
	        }
	        if (fileout_bathy_child.getText().length() == 0)
	        {
	            updateStatus("Bathy out filename must be specify.");
	            return;
	        }
	        if(!interp.getSelection()){
		        if (filein_bathy_child.getText().length() == 0)
		        {
		            updateStatus("Bathy In Child filename must be specify.");
		            return;
		        }
		        if (!fileTest(filein_bathy_child.getText()))
		        {
		            updateStatus("Parent bathymetry file doesn't exist");
		            return;
		        }
	        }
	        if(coord.getSelection()){
	        	if (filein_coord_parent.getText().length() == 0)
		        {
		            updateStatus("Coord In filename must be specify.");
		            return;
		        }
		        if (fileout_coord_child.getText().length() == 0)
		        {
		            updateStatus("Coord out filename must be specify.");
		            return;
		        }
		        if (!fileTest(filein_coord_parent.getText()))
		        {
		            updateStatus("Parent coordinate file doesn't exist");
		            return;
		        }
		        
		        if(!interp.getSelection()){
			        if (this.filein_coord_child.getText().length() == 0)
			        {
			            updateStatus("Coord In Child filename must be specify.");
			            return;
			        }
			        if (!fileTest(filein_coord_child.getText()))
			        {
			        	 updateStatus("Child coordinate file doesn't exist");
			            return;
			        }
		        }
		        
	        }
	        
	        
	        updateStatus(null);
	    }
	
		
		private boolean fileTest(String filename){
			 File file = new File(filename);
        	 return file.isFile();
		}
		
		  
		private void updateStatus(String message)
	    {
	        setErrorMessage(message);
	        setPageComplete(message == null);
	    }


	    private void createLine(Composite parent, int ncol)
	    {
	    
	        Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL |
	                SWT.BOLD);
	        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	        gridData.horizontalSpan = ncol;
	        line.setLayoutData(gridData);
	    }
	    
	    public IWizardPage getNextPage() {
	    	     SelectedAreaPage page = ((configManagerWizard)getWizard()).page2;
	    	     fillData();
	    	     page.onEnterPage();
	    	     return page;
	    }

		private void fillData() {
			  data.setFilein_bathy_parent(filein_bathy_parent.getText());
			  data.setVarName(varSelect.getText());
			  data.setFileout_bathy_child(fileout_bathy_child.getText());
			  data.setCoord(coord.getSelection());
			  if(data.getCoord()){
				  data.setFilein_coord_parent(filein_coord_parent.getText());
				  data.setFileout_coord_child(fileout_coord_child.getText());
			  }
			  data.setInterp(interp.getSelection());
			  if(!data.getInterp()){
				  data.setFilein_bathy_child(filein_bathy_child.getText());
				  if(data.getCoord())
				   data.setFilein_coord_child(filein_coord_child.getText());
			  }
			  data.setMatch(match.getSelection());
			  if(data.getMatch())
				  data.setNmatch(Integer.parseInt(nmatch.getText()));
			  data.setNraff(Integer.parseInt(nraff.getText()));
			  data.setFill_closed(fill_closed.getSelection());
			  
			  data.setJpiglo(Integer.parseInt(jpiglo.getText()));
			  data.setJpjglo(Integer.parseInt(jpjglo.getText()));
			  data.setJpizoom(Integer.parseInt(jpizoom.getText()));
			  data.setJpjzoom(Integer.parseInt(jpjzoom.getText()));
			  
		}
}
