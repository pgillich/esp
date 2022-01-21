package sim.app.esp;

import java.awt.Color;
import java.awt.Font;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JFrame;

import sim.app.tutorial6.Tutorial6;
import sim.display.Controller;
import sim.display.GUIState;
import sim.display3d.Display3D;
import sim.engine.SimState;
import sim.portrayal3d.SimplePortrayal3D;
import sim.portrayal3d.continuous.ContinuousPortrayal3D;
import sim.portrayal3d.network.ArrowEdgePortrayal3D;
import sim.portrayal3d.network.NetworkPortrayal3D;
import sim.portrayal3d.network.SpatialNetwork3D;
import sim.portrayal3d.simple.LabelledPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.portrayal3d.simple.TransformedPortrayal3D;
import sim.portrayal3d.simple.WireFrameBoxPortrayal3D;


import sim.util.*;

public class EspWithUI extends GUIState {

	public Display3D display;
	public JFrame displayFrame;

	ContinuousPortrayal3D bodyPortrayal = new ContinuousPortrayal3D();
	ContinuousPortrayal3D labelPortrayal = new ContinuousPortrayal3D();
	NetworkPortrayal3D forcePortrayal = new NetworkPortrayal3D();

	public static void main(String[] args) 
	{
		new EspWithUI().createController();
	}

	public EspWithUI() {
		super(new Esp(System.currentTimeMillis()));
	}

	public EspWithUI(SimState state) {
		super(state);
	}

	public static String getName() {
		return "ESP";
	}

	public Object getSimulationInspectedObject() { return state; }
	
	public void start() {
		super.start();
		setupPortrayals();
	}

	public void load(SimState state) {
		super.load(state);
		setupPortrayals();
	}

	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;
	}

	public void setupPortrayals() {
		display.destroySceneGraph();
		Esp esp = (Esp) state;
		bodyPortrayal.setField(esp.bodies);

		// make individual portrayals of proper size
		Bag objsBody = esp.bodies.getAllObjects();

		// Color colors[] = {Color.yellow, Color.white, Color.green ,
		// Color.blue, Color.red, Color.orange,
		// Color.magenta, Color.cyan, Color.pink, Color.white};

		/*
		 * for(int i=0;i<10;i++) bodyPortrayal.setPortrayalForObject(
		 * objs.objs[i], new SpherePortrayal3D(colors[i], // objs.objs[i], new
		 * SpherePortrayal3D(loadImage(imageNames[i]), (float)
		 * (Math.log(Tutorial6.DIAMETER[i])*50), 50));
		 */

/*		
			TransformedPortrayal3D trans = new TransformedPortrayal3D(
					new BoxPortrayal3D(Color.GRAY, 1)
			);

			trans.scale(esp.w, esp.l, esp.h);
			
			trans.rotateZ(90.0); // move pole from Y axis up to Z axis
			bodyPortrayal.setPortrayalForObject(objs.objs[0], trans);
*/		
/*		
		SimplePortrayal3D wireFrameBox = new LabelledPortrayal3D(
			new WireFrameBoxPortrayal3D(-esp.w_2,-esp.l_2,-esp.h_1,esp.w_1,esp.l_1,esp.h_2,Color.GRAY)
			,(float)esp.w_1,  (float)esp.l_1, (float)-esp.h_1,new Font("SansSerif",Font.PLAIN, 12),"X",Color.PINK,1,false
			);
*/		
		SimplePortrayal3D wireFrameBox = new WireFrameBoxPortrayal3D(-esp.w_2,-esp.l_2,-esp.h_1,esp.w_1,esp.l_1,esp.h_2,Color.GRAY);
		
		SimplePortrayal3D sphere = new SpherePortrayal3D(Color.MAGENTA,0.1f);
//		SimplePortrayal3D sphere = new WireFrameBoxPortrayal3D(-0.1,-0.1,-0.1,0.1,0.1,0.1,Color.MAGENTA);
		
		esp.wireFrameBoxRotated = new TransformedPortrayal3D(wireFrameBox);
//		bodyPortrayal.setPortrayalForObject(objs.objs[0], esp.wireFrameBoxRotated);
//		bodyPortrayal.setPortrayalForAll(esp.wireFrameBoxRotated);
		bodyPortrayal.setPortrayalForObject(objsBody.objs[0], esp.wireFrameBoxRotated);
		

		labelPortrayal.setField(esp.labels);
		Bag labelObjs = esp.labels.getAllObjects();
		for(int lo=0; lo<labelObjs.numObjs; lo++)
		{
			if(labelObjs.get(lo) instanceof BodyLabel)
			{
				BodyLabel bl = (BodyLabel)labelObjs.get(lo);
				labelPortrayal.setPortrayalForObject(bl,bl.portrayal);
			}
		}

		for (BodyForce bf : esp.forceMap.values()) {
			labelPortrayal.setPortrayalForObject(bf.fromNodeObject,bf.fromNodePortrayal);
			labelPortrayal.setPortrayalForObject(bf.toNodeXObject,bf.toNodeXPortrayal);
			labelPortrayal.setPortrayalForObject(bf.toNodeYObject,bf.toNodeYPortrayal);
			labelPortrayal.setPortrayalForObject(bf.toNodeZObject,bf.toNodeZPortrayal);
		}

//		labelPortrayal.setPortrayalForAll(new SpherePortrayal3D(Color.CYAN,0.5f));
		
//		labelPortrayal.setPortrayalForObject(esp.labels.getAllObjects().get(0), new SpherePortrayal3D(Color.YELLOW,0.5f));
		
		forcePortrayal.setField( new SpatialNetwork3D( esp.labels, esp.forces ));
		forcePortrayal.setPortrayalForAll(new ArrowEdgePortrayal3D(0.1f));	
		
		display.reset();
		display.createSceneGraph();
	}

	public void init(Controller c) {
		super.init(c);

		Esp esp = (Esp) state;
		bodyPortrayal.setField(esp.bodies);
		labelPortrayal.setField(esp.labels);

		display = new Display3D(600, 600, this, 1);
		display.attach(bodyPortrayal, "Body");
		display.attach(labelPortrayal, "Labels");
		display.attach(forcePortrayal, "Forces");
		display.translate(-esp.r+esp.l, -esp.l*2, 0);
		display.scale(1.0 / (esp.l*3)); 
//		display.translate(esp.r, -esp.l, 0);
//		display.scale(1.0 / (esp.l*3));


		
		displayFrame = display.createFrame();
		c.registerFrame(displayFrame); // register the frame so it appears in
										// the "Display" list
		displayFrame.setVisible(true);
	}

}
