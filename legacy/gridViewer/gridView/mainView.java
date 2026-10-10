package gridView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import observer.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import palette.Palette;
import palette.paletteDialog;
import tools.MathUtil;
import ucar.nc2.Variable;
import Context.Context;
import decorator.IJDecor;
import decorator.introDecor;


/**
 * 
 */

/**
 * @author mlaborie
 *
 */
public class mainView implements Observer {

	private Shell sShell = null;  //  @jve:decl-index=0:visual-constraint="32,12"
	private Button openButton = null;
	private Button redrawButton = null;
	private Text fieldLon = null;
	private Text fieldLat = null;
	private Combo varSelect  = null;
	private Combo timeCombo = null;
	private Combo depthCombo = null;
	private Button putButton = null;
	private Label maxLabel = null;
	private Text maxText = null;
	private Label minLabel = null;
	private Text minText = null;
	private Label maskLabel = null;
	private Text maskText = null;
	private String path = new String("/data/rd_exchange/");
	private CTabFolder tabFolder = null;
	private Menu menu = null;
	
	

 private void createVarCombo() {
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		
		varSelect = new Combo(sShell, SWT.NONE);
		varSelect.setText("Variables");
		varSelect.setEnabled(false);
		varSelect.setLayoutData(gridData);
		varSelect.addSelectionListener(
				  new SelectionAdapter()
				  { 
					  public void widgetSelected(SelectionEvent e){
						  if(getContext()!=null){
						  readVar(null);
						  readData();
						  setMinMaxMask();
						  }
					  }
				  });
   }
	
    private void readVar(String varname){
    	if(varname == null)
			varname = varSelect.getText();
    	getContext().med.var.readVar(varname);
    	selectVar(varname);
    }
 
	private void selectVar(String varname){
		  timeCombo.removeAll();
		  depthCombo.removeAll();
		  depthCombo.setEnabled(false);
		  timeCombo.setEnabled(false);
		  tabFolder.getSelection().setText(varname);
		  if(getContext().med.var.getDim()>2){
			if(getContext().med.var.getVarSize(0)>1){
			timeCombo.setEnabled(true);
			fillCombo(timeCombo,getContext().med.var.getVarSize(0));
			}
			if(getContext().med.var.getDim()>3){
				if(getContext().med.var.getVarSize(1)>1){
				depthCombo.setEnabled(true);
				fillCombo(depthCombo,getContext().med.var.getVarSize(1));
				}
			}
		 }
		  if(getContext().getDecor().getClass().equals(IJDecor.class)){
			  fieldLat.setEnabled(true);
			  fieldLon.setEnabled(true);
			  putButton.setEnabled(true);
		  }
	}
	
	private void openFile(){
		FileDialog dialog = new FileDialog(sShell, SWT.OPEN);
        dialog.setFilterPath(path);
        dialog.setFilterExtensions(new String[] { "*.nc","*.cdf","*" });
        String filename = dialog.open();
        if(filename != null)
        	openNCDFFile(filename,dialog.getFilterPath());
	}
	
	
	private void openNCDFFile(String filename,String path){
			getContext().med.var.loadFile(filename);
			varSelect.removeAll();
			for(int i=0; i<getContext().med.var.getVariables().size();i++){
				Variable v =(Variable) getContext().med.var.getVariables().get(i);
				if(v.getDimensions().size()>=2)
					varSelect.add(v.getName());
			}
				this.path = path;
				varSelect.setEnabled(true);
		        sShell.setText(filename);
	   }
	
	
	private void fillCombo(Combo c, int size){
		for(int i=0;i<size;i++)
			c.add(""+i+"");
	}
	
	
	private void resetForm(){
		timeCombo.setText("Counter");
		timeCombo.setEnabled(false);
		depthCombo.setText("depth");
		depthCombo.setEnabled(false);
		varSelect.setText("Variables");
		varSelect.setEnabled(false);
		fieldLat.setEnabled(false);
		fieldLon.setEnabled(false);
		putButton.setEnabled(false);
		redrawButton.setEnabled(false);
		minText.setEnabled(false);
		maxText.setEnabled(false);
		maskText.setEnabled(false);
	}
	
	/**
	 * This method initializes combo	
	 *
	 */
	private void createTimeCombo() {
		
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		
		timeCombo = new Combo(sShell, SWT.NONE);
		timeCombo.setText("Counter");
		timeCombo.setEnabled(false);
		timeCombo.setLayoutData(gridData);
		timeCombo.addSelectionListener(
				  new SelectionAdapter()
				  { 
					  public void widgetSelected(SelectionEvent e){
								 if(getContext().med.var.setTimeCounter(Integer.parseInt(timeCombo.getText())))
									 readData();
						} 
				  });
	}

	/**
	 * This method initializes combo1	
	 *
	 */
	private void createDepthCombo() {
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		
		depthCombo = new Combo(sShell, SWT.NONE);
		depthCombo.setText("depth");
		depthCombo.setEnabled(false);
		depthCombo.setLayoutData(gridData);
		depthCombo.addSelectionListener(
				  new SelectionAdapter()
				  { 
					  public void widgetSelected(SelectionEvent e){
						 if(getContext().med.var.setDepth(Integer.parseInt(depthCombo.getText())))
							 readData();
					  }
				  });
	}

	private void createCTabFolder(){
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 8;
		tabFolder = new CTabFolder(sShell, SWT.CLOSE);
		tabFolder.setUnselectedCloseVisible(false);
		tabFolder.setSimple(false);
		tabFolder.setLayoutData(gridData);
	    tabFolder.setBorderVisible(true);
	    tabFolder.addSelectionListener(new SelectionAdapter(){ 
	    	 public void widgetSelected(SelectionEvent e){
                   if(getContext()!=null){
		        		  if(getContext().med.var.getVar()!=null){
		        			  varSelect.removeAll();
		        				for(int i=0; i<getContext().med.var.getVariables().size();i++){
		        					Variable v =(Variable) getContext().med.var.getVariables().get(i);
		        					if(v.getDimensions().size()>=2)
		        						varSelect.add(v.getName());
		        				}
		        			  varSelect.setEnabled(true);
		        			  varSelect.setText(getContext().med.var.getVar().getName());
		        			  selectVar(getContext().med.var.getVar().getName());
		        			  setMinMaxMask(); 
		        		  } else {
		        			  resetForm();
		        		  }
		        	  } else {
		        		  resetForm();
		        	  }
				  }} );
		//addContext();
	}
	
	private void addContext() {
		final CTabItem item = new CTabItem(tabFolder, SWT.NONE);
		item.setText("New Context");
		Context c = new Context(tabFolder, SWT.NONE);
		c.registerInterest(this);
		item.setControl(c);
		item.addDisposeListener(new DisposeListener() {
		    public void widgetDisposed(DisposeEvent e) {
		       item.getControl().dispose();
		    }
		});
		resetForm();
		tabFolder.setSelection(item);
	}
	
	
	public void createMenu() {
		
		menu  = new Menu(sShell,SWT.BAR);
	    MenuItem itemFile = new MenuItem(menu,SWT.CASCADE);
	    itemFile.setText("File");
	    Menu menuFile = new Menu(sShell, SWT.DROP_DOWN);
	    MenuItem openItem = new MenuItem(menuFile, SWT.CASCADE);
	    openItem.setText("Open Ntcdf File");
	    openItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            	openFile();
            }});
	    MenuItem saveGifItem = new MenuItem(menuFile, SWT.CASCADE);
	    saveGifItem.setText("Save as Gif");
	    saveGifItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
          	    FileDialog dialog = new FileDialog(sShell, SWT.SAVE);
  		        dialog.setFilterPath("/home/");
  		        dialog.setFilterExtensions(new String[] { "*.gif" });
  		        String filename = dialog.open();
  		        if(filename != null && getContext()!=null){
  		        getContext().SaveImageAsGif(filename);
  		        }
            }
          });
        MenuItem saveItem = new MenuItem(menuFile, SWT.CASCADE);
        saveItem.setText("Save as Jpeg");
        saveItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
          	    FileDialog dialog = new FileDialog(sShell, SWT.SAVE);
  		        dialog.setFilterPath("/home/");
  		        dialog.setFilterExtensions(new String[] { "*.jpg" });
  		        String filename = dialog.open();
  		        if(filename != null && getContext()!=null){
  		        getContext().SaveImageAsJpg(filename);
  		        }
            }
          });
        
        MenuItem closeItem = new MenuItem(menuFile, SWT.CASCADE);
        closeItem.setText("Close");
        closeItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            	System.exit(0);
            }
          });
        
        itemFile.setMenu(menuFile);
	    MenuItem itemView = new MenuItem(menu,SWT.CASCADE);
	    itemView.setText("View");
	    Menu menuView = new Menu(sShell, SWT.DROP_DOWN);
	    MenuItem zoomOut = new MenuItem(menuView,SWT.CASCADE);
	    zoomOut.setText("Zoom Out\tCtrl+Z");
	    zoomOut.setAccelerator(SWT.CTRL+'Z');
	    zoomOut.addListener(SWT.Selection, new Listener() {
	          public void handleEvent(Event e) {
	        	  //TODO: Proxy
	        	  if(getContext()!=null){
	        		  if(getContext().med.var.getVar()!=null)
	        		  {
	        		   readVar(getContext().med.var.getVar().getName());
	        	       readData();
	        	       setMinMaxMask(); //Ca depend (Preference)
	        		  }
	        	  }
	          }
	        });
	    MenuItem newContext = new MenuItem(menuView,SWT.CASCADE);
	    newContext.setText("New context\tCtrl+T");
	    newContext.setAccelerator(SWT.CTRL+'T');
	    newContext.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
          	   addContext();
  		        }
            });
	    MenuItem normalView = new MenuItem(menuView,SWT.CASCADE);
	    normalView.setText("Normal view");
	    normalView.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
          	    if(getContext()!=null)
          	    	getContext().setIJView();
          	        readData();
  		        }
            });
	    MenuItem lonlatView = new MenuItem(menuView,SWT.CASCADE);
	    lonlatView.setText("LonLat view (Experimental)");
	    lonlatView.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
          	    if(getContext()!=null)
          	    	getContext().setLonLatView();	
          	        readData();
  		        }
          });
	    MenuItem threeDView = new MenuItem(menuView,SWT.CASCADE);
	    threeDView.setText("3D view");
	    threeDView.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            	if(getContext()!=null)
            		getContext().setThreeDView();
  		        }
            });
	    
	    itemView.setMenu(menuView);
	    MenuItem itemTools = new MenuItem(menu,SWT.CASCADE);
	    itemTools.setText("Tools");
	    Menu menuTools = new Menu(sShell, SWT.DROP_DOWN);
        itemTools.setMenu(menuTools);
        MenuItem choosePalette = new MenuItem(menuTools,SWT.CASCADE);
        choosePalette.setText("Choose a palette");
        choosePalette.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
          	    paletteDialog dial = new paletteDialog(sShell,getContext().med.palette);
         		 dial.show();	
         		while (!dial.isDisposed())
    				if (!Display.getCurrent().readAndDispatch())
    					Display.getCurrent().sleep();
          	     if(getContext()!=null && getContext().getImageData()!=null)
          	    	 getContext().updatePalette();
            }
            
          });
        
        MenuItem varInfo = new MenuItem(menuTools,SWT.CASCADE);
        varInfo.setText("Variable Infos");
        varInfo.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
          	     if(getContext()!=null && getContext().med.var.getVar() !=null)
          	     {
          	        MessageBox mb = new MessageBox(sShell, 
          	          SWT.ICON_INFORMATION);
          	        mb.setMessage(getContext().med.var.getVarInfo());
          	        mb.open();
          	     }
            }
            
          });
        
        
        MenuItem listpointItem = new MenuItem(menuTools,SWT.CASCADE);
        listpointItem.setText("Load point list");
        listpointItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            	if(getContext().getDecor().getClass().equals(IJDecor.class)){
            	FileDialog dialog = new FileDialog(sShell, SWT.CASCADE);
  		        dialog.setFilterPath(path);
  		        dialog.setFilterExtensions(new String[] { "*" });
  		        String filename = dialog.open();
  		        if(filename != null){
  		        	IJDecor decor = (IJDecor) getContext().getDecor();
		        	decor.getPoints().loadFile(filename);
  		        	path = dialog.getFilterPath();
  		        }
  		        getContext().redraw();
              }
            }
          });
        
        MenuItem clearListpointItem = new MenuItem(menuTools,SWT.CASCADE);
        clearListpointItem.setText("Clear point list");
        clearListpointItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            	if(getContext().getDecor().getClass().equals(IJDecor.class)){
  		        	IJDecor decor = (IJDecor) getContext().getDecor();
		        	decor.getPoints().clearList();
  		        getContext().redraw();
              }
            }
          });
        
        MenuItem procLayoutItem = new MenuItem(menuTools,SWT.CASCADE);
        procLayoutItem.setText("Load proc layout");
        procLayoutItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            	if(getContext().getDecor().getClass().equals(IJDecor.class)){
            		
            	FileDialog dialog = new FileDialog(sShell, SWT.CASCADE);
  		        dialog.setFilterPath(path);
  		        dialog.setFilterExtensions(new String[] { "*" });
  		        String filename = dialog.open();
  		        if(filename != null){
  		        	IJDecor decor = (IJDecor) getContext().getDecor();
  		        	decor.getProcLayout().loadFile(filename);
  		        	path = dialog.getFilterPath();	        
  		        }
  		        getContext().redraw();
            }
            }
          });
        
        
        MenuItem clearProcLayoutItem = new MenuItem(menuTools,SWT.CASCADE);
        clearProcLayoutItem.setText("Clear proc layout");
        clearProcLayoutItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            	if(getContext().getDecor().getClass().equals(IJDecor.class)){
  		        	IJDecor decor = (IJDecor) getContext().getDecor();
		        	decor.getProcLayout().clearList();
  		           getContext().redraw();
              }
            }
          });
        
      
        MenuItem help = new MenuItem(menuTools,SWT.CASCADE);
        help.setText("Help");
        help.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            	Browser browser;
                try {
                	final CTabItem item = new CTabItem(tabFolder, SWT.NONE);
            		item.setText("New Context");
            		browser = new Browser(tabFolder, SWT.NONE);
            		browser.setUrl("http://px-124.mercator-ocean.fr/dokuwiki/doku.php?id=system:tools:gridviewer");
            		item.setControl(browser);
            		item.setText("Help");
            		item.addDisposeListener(new DisposeListener() {
            		    public void widgetDisposed(DisposeEvent e) {
            		       item.getControl().dispose();
            		    }
            		});
            		tabFolder.setSelection(item);
            		resetForm();
                   
             } catch (SWTError err) {
            	  System.out.println(err.getMessage());
                   MessageBox messageBox = new MessageBox(sShell, SWT.ICON_ERROR | SWT.OK);
                   messageBox.setMessage("Browser cannot be initialized.");
                   messageBox.setText("Exit");
                   messageBox.open();
                   System.exit(-1);
                }


            }
            
          });
        sShell.setMenuBar(menu);
         
       
        
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Display display = Display.getDefault();
		mainView thisClass = new mainView();
		thisClass.createSShell();
		boolean draw = true;
		boolean read = false;
		
		for (int i = 0; i < args.length; i++) {
              
			if (args[i].equals("-file") &&  args.length>i+1 ){
            	 File file = new File(args[i+1]);
            	 if(file.isFile()){
            		 thisClass.openNCDFFile(file.getAbsolutePath(),file.getPath());
            		 i++;
            	 } else {
            		 System.out.println("Bad filename: " + args[i+1]);
            		 System.exit(0);
            	 }
            	 
		      }  else if(args[i].equals("-var") && thisClass.getContext().med.var.getFile() != null && args.length>i+1){
		    	  thisClass.readVar(args[i+1]);
		    	  thisClass.varSelect.setText(args[i+1]);
		    	  read = true;
		    	  i++;
		      }else if(args[i].equals("-res")  && args.length>i+2){
				 thisClass.getContext().med.zoom.setSizePic(new Point(Integer.parseInt(args[i+1]),Integer.parseInt(args[i+2])));
				 thisClass.getContext().med.zoom.computeZoom();
				 draw = false;
			     i=i+2;
		      }  
		      
		      else if(args[i].equals("-zoom")  && args.length>i+4){
		    	  Point p1 = new Point(Integer.parseInt(args[i+1]),Integer.parseInt(args[i+2]));
		    	  Point p2 = new Point(Integer.parseInt(args[i+3]),Integer.parseInt(args[i+4]));
		    	  int tmp;
		    	  if(p1.x > p2.x)
					{
						tmp = p1.x;
						p1.x = p2.x;
						p2.x = tmp;
					}
					if(p1.y > p2.y)
					{
						tmp = p1.y;
						p1.y = p2.y;
						p2.y = tmp;
					}
					if((((p2.x-p1.x) >1 && (p2.y-p1.y) >1) || (thisClass.getContext().med.var.getDim()>2 && ((p2.x-p1.x) >1 || (p2.y-p1.y) >1))) 
							) {
						thisClass.getContext().med.zoom.setSelectedArea(new Rectangle(p1.x,p1.y,p2.x,p2.y));
						thisClass.getContext().med.zoom.computeZoom();
	                }
					i=i+4;
		      } else if(args[i].equals("-pal") && args.length>i+1){
	             if(Palette.class.getResource(args[i+1]) != null){
		    	  thisClass.getContext().med.palette.readColorMap(args[i+1]);
		    	  i++;
	             } else {
	            		 System.out.println("Available palette are:");
	            		 BufferedReader in;
						try {
							in = new BufferedReader(new InputStreamReader(Palette.class.getResource("listPalette.txt").openStream()));
							String str;
		         	        while ((str = in.readLine()) != null) {
		         	         System.out.println(str);	
		         	        }
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	            		 System.exit(0);
	            	 }
		      } 
		      else if(args[i].equals("-depth") && args.length>i+1){
		    	  thisClass.getContext().med.var.setDepth(Integer.parseInt(args[i+1]));
		    	  i++;
		      } else if(args[i].equals("-time") &&  args.length>i+1){
		    	  thisClass.getContext().med.var.setTimeCounter(Integer.parseInt(args[i+1]));
		    	  i++;
		      } 
		      else if(args[i].equals("-range") && args.length>i+2 ){
		         thisClass.getContext().med.palette.setRange(Double.parseDouble(args[i+1]),Double.parseDouble(args[i+2]));
		         i=i+2;
		      } else if(args[i].equals("-maskvalue") && args.length>i+1 ){
			         thisClass.getContext().med.palette.setMaskValue(Double.parseDouble(args[i+1]));
			         i++;
			  } else if(args[i].equals("-gif") && args.length>i+1 ){
				  thisClass.getContext().readData();
		          thisClass.getContext().SaveImageAsGif(args[i+1]);
	               i++;
	               draw = false;
			      }
			   else if(args[i].equals("-jpg") && args.length>i+1 ){
			       thisClass.getContext().readData();
		           thisClass.getContext().SaveImageAsGif(args[i+1]);
		           i++;
		           draw = false;
		        }
		     else {
			System.out.println("usage: gridViewer -file {URL}");
			System.out.println("                       [-var {varname}]");
			System.out.println("                       [-zoom {x1 y1 x2 y2}]");
			System.out.println("                       [-pal {palette name}]");
			System.out.println("                       [-depth {depth}]");
			System.out.println("                       [-time {time counter}]");
			System.out.println("                       [-range {min max}]");
			System.out.println("                       [-maskvalue {value}]");
			System.out.println("                       [-res width height (with -gif only)]");
			System.out.println("                       [-gif {filename}]");
			System.out.println("                       [-jpg {filename}]");
			System.out.println("invalid arg: " + args[i]);
			System.exit(0);
          }  // usage error
		}  // for args
		
			
		
		if(draw){
			if(read)
			{
				thisClass.readData();
				thisClass.setMinMaxMask();
			}
		thisClass.sShell.open();	
		while (!thisClass.sShell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		}
		thisClass.dispose();
		
	}
	
	private void dispose() {
		//for(int i=0;i<tabFolder.getItemCount();i++)
		//	(Context) tabFolder.getItem(i).getControl();
	}

	/**
	 * This method initializes sShell
	 */
	private void createSShell() {
			
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 7;
		gridLayout.makeColumnsEqualWidth =true;
		
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		
		GridData gridData2 = new org.eclipse.swt.layout.GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		
		sShell = new Shell();
		sShell.setText("Shell");
		sShell.setLayout(gridLayout);
		sShell.setSize(new org.eclipse.swt.graphics.Point(780,750));
	
		
		
		createMenu();
		openButton = new Button(sShell, SWT.NONE);
		openButton.setText("Open");
		openButton.setLayoutData(gridData);
		openButton.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		    	  openFile();	        
		        } });
		
		createVarCombo();
		createTimeCombo();
		createDepthCombo();	
		
		
		fieldLon = new Text(sShell, SWT.BORDER);
		fieldLon.setLayoutData(gridData);
		fieldLat = new Text(sShell, SWT.BORDER);
		fieldLat.setLayoutData(gridData);
		putButton = new Button(sShell, SWT.NONE);
		putButton.setText("Put");
		putButton.setLayoutData(gridData);
		
		putButton.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		 if(getContext().getDecor().getClass().equals(IJDecor.class)){
			 if(fieldLon.getCharCount()>0 && fieldLat.getCharCount()>0){
		    	   IJDecor decor = (IJDecor) getContext().getDecor();
 		        	decor.getPoints().addPoint(
		        		 new Point(Integer.parseInt(fieldLon.getText()),
		  		    		   Integer.parseInt(fieldLat.getText())));
		              getContext().redraw();
		       }
		    	  }
		        } });
		
		createCTabFolder();

		
		
	
	
		minLabel = new Label(sShell, SWT.NONE);
		minLabel.setText("Minimun Value:");
		minText = new Text(sShell, SWT.BORDER);
		maxLabel = new Label(sShell, SWT.NONE);
		maxLabel.setText("Maximun Value:");
		maxText = new Text(sShell, SWT.BORDER);
		maskLabel = new Label(sShell, SWT.NONE);
		maskLabel.setText("Mask Value:");
		maskText = new Text(sShell, SWT.BORDER);
		redrawButton = new Button(sShell, SWT.NONE);
		redrawButton.setText("Redraw");	
		redrawButton.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		       getContext().med.palette.setRange(Double.parseDouble(minText.getText()),
		    		   Double.parseDouble(maxText.getText()));
		       getContext().med.palette.setMaskValue(Double.parseDouble(maskText.getText()));
		       readData();
		        } });
			
		minText.setLayoutData(gridData2);
		maxText.setLayoutData(gridData2);
		maskText.setLayoutData(gridData2);
		redrawButton.setLayoutData(gridData2);
		
		addContext();
		
		Listener resListener = new Listener() {
		      public void handleEvent(Event e) {
		    	if(getContext()!=null){
		    		if(!getContext().getDecor().getClass().equals(introDecor.class))
		    	getContext().med.zoom.setCanvasRect(tabFolder.getClientArea());
		        getContext().med.zoom.computeDisplay();
		        getContext().med.zoom.computeZoom();
		        getContext().redraw();
		       }
		      }
		    };
			
			sShell.addListener( SWT.Resize, resListener );
		
	}

	
	private void readData(){
		if(getContext()!=null){
		if(getContext().med.var.getFile()!=null && getContext().med.var.getVar()!=null){
			getContext().med.palette.resetNewRange();
			getContext().readData();
			//setMinMaxMask();
			if(getContext().getDecor().getClass().equals(introDecor.class))
				getContext().setIJView();
			getContext().redraw();	
		 }
		}
	}
	
	private void setMinMaxMask(){
		if(getContext()!=null){
			 minText.setEnabled(true);
			 maxText.setEnabled(true);
			 maskText.setEnabled(true);
			 redrawButton.setEnabled(true);
			 minText.setText(""+MathUtil.round(getContext().med.palette.getNewmin())+"");
			 maxText.setText(""+MathUtil.round(getContext().med.palette.getNewmax())+"");
			 maskText.setText(""+getContext().med.palette.getMaskValue()+"");
		}
	}
	
	
	private Context getContext() {
		if(tabFolder.getSelection() !=null){
		if(tabFolder.getSelection().getControl().getClass().equals(Browser.class))
			return null;
		return (Context) tabFolder.getSelection().getControl();
		}
		return null;
	}



	
	public mainView() {
		super();
	}

	public void sendNotify(String s) {
		setMinMaxMask();
	}


	
}

		

