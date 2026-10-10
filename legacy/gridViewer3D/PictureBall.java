
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.image.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.awt.Container;



public class PictureBall {

public PictureBall() {

// Créer l’univers

SimpleUniverse universe = new SimpleUniverse();

// Créer une structure contenant les objets

BranchGroup group = new BranchGroup();

// Configurer les couleurs

Color3f black = new Color3f(0.0f, 0.0f, 0.0f);

Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

Color3f red = new Color3f(0.7f, .15f, .15f);

// Configurer la texture

TextureLoader loader = new TextureLoader("Arizona.jpg",

"LUMINANCE", new Container());

Texture texture = loader.getTexture();

texture.setBoundaryModeS(Texture.WRAP);
texture.setBoundaryModeT(Texture.WRAP);
texture.setBoundaryColor( new Color4f( 0.0f, 1.0f, 0.0f, 0.0f ) );

// Configurer les attributs de la texture

// Paramètres possibles : REPLACE, BLEND ou DECAL (ici, MODULATE)

TextureAttributes texAttr = new TextureAttributes();
texAttr.setTextureMode(TextureAttributes.MODULATE);

Appearance ap = new Appearance();
ap.setTexture(texture);
ap.setTextureAttributes(texAttr);

//Configurer le matériau

ap.setMaterial(new Material(red, white, red, black, 1.0f));

// Créer une sphère pour y appliquer les textures

int primflags = Primitive.GENERATE_NORMALS + Primitive.GENERATE_TEXTURE_COORDS;

Sphere sphere = new Sphere(0.5f, primflags, ap);

group.addChild(sphere);

// Créer les éclairages

Color3f light1Color = new Color3f(1f, 1f, 1f);
BoundingSphere bounds =
	new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);

Vector3f light1Direction = new Vector3f(4.0f, -7.0f, -12.0f);

DirectionalLight light1
	= new DirectionalLight(light1Color, light1Direction);
light1.setInfluencingBounds(bounds);
group.addChild(light1);

AmbientLight ambientLight =
	new AmbientLight(new Color3f(.5f,.5f,.5f));
ambientLight.setInfluencingBounds(bounds);
group.addChild(ambientLight);

// Regarder vers l&a sphère pour lui faire face

universe.getViewingPlatform().setNominalViewingTransform();

// Ajouter les groupe d’objets à l’univers

universe.addBranchGraph(group);

}

public static void main(String[] args) {

new PictureBall();

}

}
