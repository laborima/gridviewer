 import java.awt.BorderLayout;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TransformGroup;
import javax.swing.JFrame;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;
  
 public class earthView extends JFrame
 {
     /**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	public earthView()
     {
         super("Allo Houston...");
  
         getContentPane().setLayout(new BorderLayout());
         // creation du canvas
         Canvas3D canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
         getContentPane().add("Center",canvas);
         // création de la scène, regroupement des objets
         BranchGroup scene = createScene();
         scene.compile();
  
         // création de l'univers de la scene 3D
         SimpleUniverse univers = new SimpleUniverse(canvas);
         // on met le plan de projection en arriere par rapport à l'origine
         univers.getViewingPlatform().setNominalViewingTransform();
         // on place la scène dans l'univers "univers"
         univers.addBranchGraph(scene);
  
         //arret du programme lors du clic sur la croix
         setDefaultCloseOperation(EXIT_ON_CLOSE);
     }
  
	 
	
     /*                                     *
      * creation d'un groupement d'objet *
      *                                     */
     public BranchGroup createScene()
     {
         // creation du BG principal
         BranchGroup ObjPrinc = new BranchGroup();
         Appearance AppTexture = AjoutTexture("ocean.jpg");
  
         // Début de l'animation
         TransformGroup TG = new TransformGroup();
         ObjPrinc.addChild(TG);
//       Donner la possibilité a l'objet de bouger
         //TG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
         
         // Ajout de la sphere à l'objet charger de l'animation
         TG.addChild(new Sphere(0.6f,Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS,40 ,AppTexture));
   
         TG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); // -----------(1)
         TG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ); //--------------(2)

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
    	
         
         
         // Configuration du nombre de tour et la vitesse
                 // ici tourne indéfiniment (-1) et prend 60 secondes pour faire un tour
       /*  Alpha alpha = new Alpha(-1,60000);
         RotationInterpolator Rotation = new RotationInterpolator(alpha, TG);
         Rotation.setSchedulingBounds(new BoundingSphere());
         ObjPrinc.addChild(Rotation); */
  
         return ObjPrinc;
     }
     /*
      *    Ajout du texture sur la sphere    *
      *                                    */
     public Appearance AjoutTexture(String NomImage)
     {
         try
         {
             Appearance Apparence = new Appearance();
             // chargement d'une image pour texture
             TextureLoader loader = new TextureLoader(NomImage,this);
             ImageComponent2D image = loader.getImage();
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
  
  
  
 }

