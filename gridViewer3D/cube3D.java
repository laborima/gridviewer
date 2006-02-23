import java.awt.Frame;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.*;


public class cube3D {

	private SimpleUniverse univ = null;
	private BranchGroup mainBranch = null;

	
	public cube3D() {
		super();
		// TODO Auto-generated constructor stub
	}


	
	public void createUniverse(){
		Transform3D t3d = new Transform3D();
		univ = new SimpleUniverse();
		mainBranch = new BranchGroup();
		
		
		t3d.rotX(10);
		t3d.rotY(180);
	
		
		ColorCube cube = new ColorCube();
		univ.getViewingPlatform().setNominalViewingTransform();
        
		
		mainBranch.addChild(new TransformGroup(t3d));
		mainBranch.addChild(cube);
		univ.addBranchGraph(mainBranch);
		
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
       cube3D c = new cube3D();
       c.createUniverse();
	}

}
