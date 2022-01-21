package sim.app.esp;

import java.awt.Color;
import java.awt.Font;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import sim.field.network.Edge;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.portrayal3d.simple.TransformedPortrayal3D;
import sim.util.Double3D;
import sim.util.MutableDouble3D;
import sim.util.Valuable;

public class BodyForce implements Valuable
{
	public Vector3d fromNodeOffset;
	
	public double scale;
	
	public String fromNodeObject;
	public SpherePortrayal3D fromNodePortrayal;
	
	public String toNodeXObject;
	public SpherePortrayal3D toNodeXPortrayal;
	public String toNodeYObject;
	public SpherePortrayal3D toNodeYPortrayal;
	public String toNodeZObject;
	public SpherePortrayal3D toNodeZPortrayal;

	public double weight;
	
	public Edge edgeX;
	public Edge edgeY;
	public Edge edgeZ;
	
	public BodyForce(String name, double ox, double oy, double oz, double scale, Color color, double fromNodeScale, double toNodeScale, double weight)
	{
		fromNodeOffset = new Vector3d(ox,oy,oz);
		
		this.scale = scale;
		
		fromNodeObject = "F_"+name;
		fromNodePortrayal = new SpherePortrayal3D(color,(float)fromNodeScale);

		toNodeXObject = fromNodeObject+"_X";
		toNodeXPortrayal = new SpherePortrayal3D(color,(float)toNodeScale);

		toNodeYObject = fromNodeObject+"_Y";
		toNodeYPortrayal = new SpherePortrayal3D(color,(float)toNodeScale);
		
		toNodeZObject = fromNodeObject+"_Z";
		toNodeZPortrayal = new SpherePortrayal3D(color,(float)toNodeScale);
		
		this.weight = weight;
	}

	public Double3D transformFromNodeLocation(Double3D bodyLocation, double rotZ)
	{
		Vector3d location3d = new Vector3d(bodyLocation.x, bodyLocation.y, bodyLocation.z);
		Vector3d newLocation = new Vector3d();
		
		Transform3D transform = new Transform3D();
		transform.rotZ(rotZ);
		transform.transform(fromNodeOffset,newLocation);
		
		return new Double3D(newLocation.x+location3d.x,newLocation.y+location3d.y,newLocation.z+location3d.z);
	}
	
	public Double3D transformToNodeLocation(Double3D fromNodeLocation, double rotZ, double fx, double fy, double fz)
	{
		Vector3d location3d = new Vector3d(fromNodeLocation.x, fromNodeLocation.y, fromNodeLocation.z);
		Vector3d newLocation = new Vector3d();
		Vector3d offsetF = new Vector3d(fx,fy,fz);
		
		Transform3D transform = new Transform3D();
		transform.rotZ(rotZ);
		transform.transform(offsetF,newLocation);
		
		return new Double3D(newLocation.x*scale+location3d.x,newLocation.y*scale+location3d.y,newLocation.z*scale+location3d.z);
	}
	 
	
	@Override
	public double doubleValue() {return weight;}
}
