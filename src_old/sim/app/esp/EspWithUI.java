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

		SimplePortrayal3D wireFrameBox = new WireFrameBoxPortrayal3D(-esp.w_2,-esp.l_2,-esp.h_1,esp.w_1,esp.l_1,esp.h_2,Color.GRAY);
		
		esp.wireFrameBoxRotated = new TransformedPortrayal3D(wireFrameBox);
		bodyPortrayal.setPortrayalForObject(objsBody.objs[0], esp.wireFrameBoxRotated);

		labelPortrayal.setField(esp.labels);
		forcePortrayal.setField( new SpatialNetwork3D( esp.labels, esp.forces ));
		
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
			
			forcePortrayal.setPortrayalForObject(bf.edgeX, bf.edgePortrayal);
			forcePortrayal.setPortrayalForObject(bf.edgeY, bf.edgePortrayal);
			forcePortrayal.setPortrayalForObject(bf.edgeZ, bf.edgePortrayal);
		}

		// TODO MASON bug
		forcePortrayal.setPortrayalForAll(new ArrowEdgePortrayal3D(0.1f,ArrowEdgePortrayal3D.appearanceForColors(Color.GREEN, null, Color.GREEN, Color.GREEN, 1.0f, 1.0f)));	
		
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
		
		display.rotateX(-45);
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
