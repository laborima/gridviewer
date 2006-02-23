package configManagerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.jface.wizard.Wizard;

public class configManagerWizard extends Wizard {


	public FilePage page1;
    public SelectedAreaPage page2;
    public String scriptPath;


	
	public configManagerWizard() {
		super();
	    //setNeedsProgressMonitor(true);
	    setWindowTitle( "RNS Relocatable Nesting System Wizard (J. Chanut / M. Laborie)");
       
	}
	
	public void addPages()
    {
        page1 = new FilePage();
        page2 = new SelectedAreaPage();
  
        
        addPage(page1);
        addPage(page2);
        
       // addPage(page3);
    }

	public boolean performFinish() {
		// TODO Auto-generated method stub
		try {
			configManagerData data = configManagerData.getInstance();
			System.out.println(data.getRequest());
			Runtime runtime = Runtime.getRuntime();
			
			File rep = new  File(System.getProperty("user.dir"));

			System.out.println("Running dir:"+rep.getAbsolutePath());
			scriptPath = rep.getAbsolutePath()+"/scripts/make_refinebathy_namelist.scr ";
			Process proc = runtime.exec(scriptPath+data.getRequest());
			
				//proc.waitFor();
				InputStream is = proc.getInputStream();
				InputStreamReader isr2 = new InputStreamReader(is);
				BufferedReader br2 = new BufferedReader(isr2);
				
				String str;
		        while ((str = br2.readLine()) != null) {
		           System.out.println(str);
		            }
		        
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public boolean canFinish() {
		if(page2.isPageComplete())
			return true;
		else
			return false;
	}
	
	
}
