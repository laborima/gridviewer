package ThreeD;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.media.j3d.Alpha;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.RotationInterpolator;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TransformGroup;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;

import palette.Palette;
import tools.ImageConverter;
import Context.Mediator;
import Context.zoomManager;
import PlotStrategy.ImageDataStrategy;
import PlotStrategy.arrayImageDataIJ;
import arrayData.varData;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;

  
 public class threeDView 
 {
     /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private SimpleUniverse univers = null;
	private Canvas3D canvas = null;
	
	public Mediator med;
	private ImageDataStrategy strategy;
	private varData var;
	private zoomManager zoom;
	private Palette palette;
	public Menu menu;
	private ImageData data = null;
	

	
	public threeDView()
    {	
		File f = new File("/SORTIES_REFERENCE/PSY3/glo/");
		String path= f.listFiles()[f.listFiles().length-1].getPath();
		System.out.println("Selected File: "+path);
		//this.setVisible(true);
		
		
		
		createContext(path);

		
		
    }
  
	
	public void createCanvas(){
//		 create AWT wrapper
		canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());	
		
    	canvas.addMouseListener(new MouseAdapter() { 
		public void mouseClicked(MouseEvent e) {
			System.exit(0);
		}
		public void mousePressed(MouseEvent e)
		{	
			System.exit(0);
		}
		
		});
          // getContentPane().add("Center",canvas);
         // création de la scène, regroupement des objets
		BranchGroup scene=createScene();
         scene.compile();
  
         // création de l'univers de la scene 3D
         univers = new SimpleUniverse(canvas);
         // on met le plan de projection en arriere par rapport à l'origine
         univers.getViewingPlatform().setNominalViewingTransform();
         // on place la scène dans l'univers "univers"
         univers.addBranchGraph(scene);
  
         //arret du programme lors du clic sur la croix
       //  setDefaultCloseOperation(EXIT_ON_CLOSE);
	}


	/*                                     *
      * creation d'un groupement d'objet *
      *                                     */
     public BranchGroup createScene()
     {
         // creation du BG principal
         BranchGroup ObjPrinc = new BranchGroup();
         Appearance AppTexture = AjoutTexture();
  
         // Début de l'animation
         TransformGroup TG = new TransformGroup();
         ObjPrinc.addChild(TG);
//       Donner la possibilité a l'objet de bouger
         //TG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); 
         // Ajout de la sphere à l'objet charger de l'animation
         TG.addChild(new Sphere(0.4f,Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS,40 ,AppTexture));
   
         TG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); // -----------(1)
         TG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ); //--------------(2)

//     
     /*    
         MouseRotate myMouseRotate = new MouseRotate();
         myMouseRotate.setTransformGroup(TG);  // ---------------(3)
         myMouseRotate.setSchedulingBounds(new BoundingSphere());  // ---------------(4)
         ObjPrinc.addChild(myMouseRotate);

         MouseTranslate myMouseTranslate = new MouseTranslate();
         myMouseTranslate.setTransformGroup(TG);  // ---------------(5)
         myMouseTranslate.setSchedulingBounds(new BoundingSphere());  // ---------------(6)
         ObjPrinc.addChild(myMouseTranslate);

         MouseZoom myMouseZoom = new MouseZoom();
         myMouseZoom.setTransformGroup(TG);  // ---------------(7)
         myMouseZoom.setSchedulingBounds(new BoundingSphere());  // ---------------(8)
         ObjPrinc.addChild(myMouseZoom);
    	*/
         //Configuration du nombre de tour et la vitesse
         // ici tourne indéfiniment (-1) et prend 60 secondes pour faire un tour
		 Alpha alpha = new Alpha(-1,30000);//new Alpha(-1,30000);
		 RotationInterpolator Rotation = new RotationInterpolator(alpha, TG);
		 Rotation.setSchedulingBounds(new BoundingSphere());
		 TG.addChild(Rotation);

         
         return ObjPrinc;
     }
     /*
      *    Ajout du texture sur la sphere    *
      *                                    */
     public Appearance AjoutTexture()
     {
         try
         {
             Appearance Apparence = new Appearance();
             // chargement d'une image pour texture
             //TextureLoader loader = new TextureLoader(filename,frame);
        
             
             ImageComponent2D image = new ImageComponent2D(
            		 ImageComponent2D.FORMAT_RGBA, ImageConverter.convertToAWT(data.scaledTo(1024,1024)) );
             //image.set(buffer);
             //loader.getImage();
             
             // instanciation de l'objet texture
             Texture2D texture = new Texture2D(Texture.BASE_LEVEL,Texture.RGBA,image.getWidth(),image.getHeight());
             texture.setImage(0, image);
             // ajout de la texture à l'apparence
             texture.setEnable(true);
        	 texture.setMagFilter(Texture.NICEST);
        	 texture.setMinFilter(Texture.NICEST);
             Apparence.setTexture(texture);
             Apparence.setTextureAttributes(new TextureAttributes());
  
             return Apparence;
         }
         catch(NullPointerException e1)
         {
             // Creation d'une texture uni dans le cas ou le fichier image n'exciste pas
             Appearance Apparence = new Appearance();
             ColoringAttributes bleu = new ColoringAttributes();
             bleu.setColor(0.0f,0.0f,0.8f);
             Apparence.setColoringAttributes(bleu);
             return Apparence;
         }
     }


 	public void createContext(String path) {
 
 		
		
		med = new Mediator();
	    var = new varData(med);
	    zoom = new zoomManager(med);
	    palette = new Palette(med);
	    strategy = new arrayImageDataIJ(med);
	      
	    med.registerPalette(palette);
	    med.registerVar(var);
	    med.registerZoom(zoom);      
	   
		med.registerStrategy(strategy);
		
		med.zoom.setCanvasRect(new Rectangle(0,0,722,512));
		med.zoom.setDisplayRect(new Rectangle(0,0,721,360));

		med.var.loadFile(path);
		med.var.readVar("temperature");
		med.zoom.computeZoom();
		
		ImageData imageData = strategy.createImageDataStrategy();
		data = new ImageData(720,360,8,
				med.palette.getPaletteData());
		int size= imageData.height*imageData.width;
		int[] tmp = new int[size];
		imageData.getPixels(0,0,size,tmp,0);
		data.setPixels(0,0,size,tmp,0);
		createCanvas();
		//context.setIntroView();
	}
     



	public void redraw() {
		// TODO Auto-generated method stub
		univers.cleanup();
		createCanvas();
		
	}

	
	
        
	
	public static void main(String[] args) {
		
		System.out.println("3DView MLaborie Present:");
	   threeDView thisClass = new threeDView();
	  // thisClass.createContext(path);
	 //  thisClass.redraw();
		//thisClass.setTitle(path);
	    Frame f = new Frame();
	    Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension d = tk.getScreenSize();
		Window myWindow = new Window(f);
		myWindow.setSize(d);
		myWindow.add(thisClass.getCanvas());
		myWindow.setVisible(true);
		
	}


	private Canvas3D getCanvas() {
		// TODO Auto-generated method stub
		return canvas;
	}


	public void dispose() {
		// TODO Auto-generated method stub
		//frame.dispose();
		univers.cleanup();
	}
  
  
  
 }

 

