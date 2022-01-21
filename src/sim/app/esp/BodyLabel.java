package sim.app.esp;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import sim.portrayal3d.simple.LabelledPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.portrayal3d.simple.TransformedPortrayal3D;

public class BodyLabel 
{
	public String name;
	public TransformedPortrayal3D portrayal; 
	
	public BodyLabel(String name, Color color, double ox, double oy, double oz)
	{
		this.name = name;
/*		
		portrayal = new TransformedPortrayal3D(
				new LabelledPortrayal3D(new SpherePortrayal3D(color,0.1f), (float)ox, (float)oy, (float)oz, 
						new Font("SansSerif",Font.PLAIN, 12), name, color, 1.0f, false)
				);
*/				
		Font font = new Font("SansSerif",Font.BOLD, 24);
		LabelledPortrayal3D lpr3d = new LabelledPortrayal3D(new SpherePortrayal3D(color,0.1f), ox, oy, oz, 
				font, name, color, false);
		lpr3d.setLabelScale(0.15);
		portrayal = new TransformedPortrayal3D(lpr3d);
	}
	
	public String toString() {return name;}
}
