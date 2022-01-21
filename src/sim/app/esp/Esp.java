package sim.app.esp;

import java.awt.Color;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import sim.engine.SimState;
import sim.field.continuous.Continuous3D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.portrayal3d.simple.TransformedPortrayal3D;
import sim.util.Double3D;
import sim.util.MutableDouble2D;

public class Esp extends SimState {

	private static final long serialVersionUID = 1L;
	
	static final double QUADRANT = Math.PI / 2.;
	static final double RAD_DEG = 90. / QUADRANT;
	
	public PrintStream out = System.out; 
	
	public Continuous3D bodies;
	public Continuous3D labels;
	public Map<String,BodyLabel> labelMap = new HashMap<String,BodyLabel>();
	public Map<String,BodyForce> forceMap = new HashMap<String,BodyForce>();
	public Network forces;
	
	public double m = 1200; // kg
	public double v = 16; // m/s
	public double r = 38; // m
	public double r_I = 1; // m
	public double I; // m
	
	public double l = 4; // m
	public double l_1 = 1.5; // m
	public double l_2;
	
	public double w = 1.5; // m
	public double w_1 = 0.5; // m
	public double w_2;
	
	public double h = 1.5; // m
	public double h_1 = 0.5; // m
	public double h_2;
	
	public double g = 9.81; // m/s2
	
	public double u_a = 1.2;
	public double u_b = 0.3;
	public double u_c = 0.3;
	public double u_d = 10000;
	
	public double frictionLimitStartTime = 0.3; // sec
	public boolean frictionLimitEnabled = true;
	
	public double correctingBreakStartTime = 0.5; // sec
	public double correctingBreakStopTime = 1.0; // sec
	public double correctingBreakCoeff = 1.2; 
	public boolean correctingBreakEnabled = true;
		
	public boolean sideSlipEnabled = true;
	public double u_max_sideSlip = 0.9;
	public double sideSlip_u_max = 0.1;
	public double sideSlip = 0;
	
	public double dt = 1E-3; // s
	
	public double endTime = 1.1; // sec
	
	public long printStep = 10;
	
	TransformedPortrayal3D wireFrameBoxRotated = null;
	
	public Esp(long seed) 
	{
		super(seed);
/*		
		try {
			out = new PrintStream("esp.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			out = System.out; 
		}
*/		
		bodies = new Continuous3D(2*r,2*(r+w),2*(r+l), 2*h);
		labels = new Continuous3D(2*r,2*(r+w),2*(r+l), 2*h);
		forces = new Network();
	}

	public void start()
    {
		super.start();	

		l_2 = l - l_1;
		w_2 = w - w_1;
		h_2 = h - h_1;
		I = 1./2. * m * r_I*r_I;
		
		bodies = new Continuous3D(2*r,2*(r+w),2*(r+l), 2*h);
		
		Double3D startLocation = new Double3D(r,0,0);
		
        Body b = new Body(this, new MutableDouble2D(r,0), QUADRANT, new MutableDouble2D(0,v), v, r);
        bodies.setObjectLocation(b, startLocation); 

		labels = new Continuous3D(2*r,2*(r+w),2*(r+l), 2*h);
        
        // -esp.w_2,-esp.l_2,-esp.h_1,esp.w_1,esp.l_1,esp.h_2
        
        labelMap.put("G", new BodyLabel("G", Color.CYAN, 0, 0, 0));
        labelMap.put("A", new BodyLabel("A", Color.YELLOW, w_1,  l_1, -h_1)); 
        labelMap.put("B", new BodyLabel("B", Color.YELLOW,-w_2,  l_1, -h_1)); 
        labelMap.put("C", new BodyLabel("C", Color.YELLOW,-w_2, -l_2, -h_1)); 
        labelMap.put("D", new BodyLabel("D", Color.YELLOW, w_1, -l_2, -h_1)); 
        
        labels.setObjectLocation(labelMap.get("G"), startLocation); 
        labels.setObjectLocation(labelMap.get("A"), startLocation); 
        labels.setObjectLocation(labelMap.get("B"), startLocation); 
        labels.setObjectLocation(labelMap.get("C"), startLocation); 
        labels.setObjectLocation(labelMap.get("D"), startLocation); 
        labels.setObjectLocation("0", new Double3D(0,0,0));
        
        forces = new Network();
        forceMap.put("G", new BodyForce("G",   0,    0,    0, 1E-3, Color.YELLOW,0.2, 0.01, 0.1));
        forceMap.put("A", new BodyForce("A", w_1,  l_1, -h_1, 1E-3, Color.GREEN, 0.2, 0.01, 0.1));
        forceMap.put("B", new BodyForce("B",-w_2,  l_1, -h_1, 1E-3, Color.GREEN, 0.2, 0.01, 0.1));
        forceMap.put("C", new BodyForce("C",-w_2, -l_2, -h_1, 1E-3, Color.GREEN, 0.2, 0.01, 0.1));
        forceMap.put("D", new BodyForce("D", w_1, -l_2, -h_1, 1E-3, Color.GREEN, 0.2, 0.01, 0.1));
        forceMap.put("R", new BodyForce("R",   0,    0,    0, 1E-3, Color.YELLOW,0.2, 0.01, 0.1));
        
        createForceNodeEdge(forceMap.get("G"),startLocation);
        createForceNodeEdge(forceMap.get("A"),startLocation);
        createForceNodeEdge(forceMap.get("B"),startLocation);
        createForceNodeEdge(forceMap.get("C"),startLocation);
        createForceNodeEdge(forceMap.get("D"),startLocation);
        createForceNodeEdge(forceMap.get("R"),startLocation);
        
        schedule.scheduleRepeating(0.0,b,dt);		
    }

	public void createForceNodeEdge(BodyForce bf, Double3D startLocation)
	{
        labels.setObjectLocation(bf.fromNodeObject, startLocation); 
        labels.setObjectLocation(bf.toNodeXObject, startLocation); 
        labels.setObjectLocation(bf.toNodeYObject, startLocation); 
        labels.setObjectLocation(bf.toNodeZObject, startLocation); 
        forces.addNode(bf.fromNodeObject);
        forces.addNode(bf.toNodeXObject);
        forces.addNode(bf.toNodeYObject);
        forces.addNode(bf.toNodeZObject);
        bf.edgeX = new Edge(bf.toNodeXObject, bf.fromNodeObject, bf);
        bf.edgeY = new Edge(bf.toNodeYObject, bf.fromNodeObject, bf);
        bf.edgeZ = new Edge(bf.toNodeZObject, bf.fromNodeObject, bf);
        forces.addEdge(bf.edgeX);
        forces.addEdge(bf.edgeY);
        forces.addEdge(bf.edgeZ);
	}
	
	public void kill()
	{
		super.kill();
		
		if( !System.out.equals(out) )
		{
			out.close();
		}
	}
	
	public MutableDouble2D newMutableDouble2D_Polar(double r, double theta)
	{
		MutableDouble2D vector2D = new MutableDouble2D(r,0);
		vector2D.rotate(theta);
		
		return vector2D;
	}
	
	public double u(double F)
	{
		return u_a - u_c / (F/u_d + u_b);
	}
	
	public MutableDouble2D simpleBeam(double F, double a, double b, double T)
	{
		double F_a = (F*b + T)/(a+b);
		return new MutableDouble2D( F_a, F-F_a );
	}
	
    public static void main(String[] args)
    {
    	doLoop(Esp.class, args);
    	System.exit(0);
    }

	public boolean isSideSlipEnabled() {
		return sideSlipEnabled;
	}

	public void setSideSlipEnabled(boolean sideSlipEnabled) {
		this.sideSlipEnabled = sideSlipEnabled;
	}

	public double getU_max_sideSlip() {
		return u_max_sideSlip;
	}

	public void setU_max_sideSlip(double u_max_sideSlip) {
		this.u_max_sideSlip = u_max_sideSlip;
	}

	public double getSideSlip_u_max() {
		return sideSlip_u_max;
	}

	public void setSideSlip_u_max(double sideSlip_u_max) {
		this.sideSlip_u_max = sideSlip_u_max;
	}

	public double getSideSlip() {
		return sideSlip;
	}

	public boolean isFrictionLimitEnabled() {
		return frictionLimitEnabled;
	}

	public void setFrictionLimitEnabled(boolean frictionLimitEnabled) {
		this.frictionLimitEnabled = frictionLimitEnabled;
	}    
	
	public double getFrictionLimitStartTime() {
		return frictionLimitStartTime;
	}

	public void setFrictionLimitStartTime(double frictionLimitStartTime) {
		this.frictionLimitStartTime = frictionLimitStartTime;
	}
    
	public boolean isCorrectingBreakEnabled() {
		return correctingBreakEnabled;
	}

	public void setCorrectingBreakEnabled(boolean correctingBreakEnabled) {
		this.correctingBreakEnabled = correctingBreakEnabled;
	}
	
	public double getCorrectingBreakStartTime() {
		return correctingBreakStartTime;
	}

	public void setCorrectingBreakStartTime(double correctingBreakStartTime) {
		this.correctingBreakStartTime = correctingBreakStartTime;
	}

	public double getCorrectingBreakStopTime() {
		return correctingBreakStopTime;
	}

	public void setCorrectingBreakStopTime(double correctingBreakStopTime) {
		this.correctingBreakStopTime = correctingBreakStopTime;
	}

	public double getCorrectingBreakCoeff() {
		return correctingBreakCoeff;
	}

	public void setCorrectingBreakCoeff(double correctingBreakCoeff) {
		this.correctingBreakCoeff = correctingBreakCoeff;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public double getM() {
		return m;
	}

	public void setM(double m) {
		this.m = m;
	}

	public double getR_I() {
		return r_I;
	}

	public void setR_I(double r_I) {
		this.r_I = r_I;
	}

	public double getI() {
		return I;
	}

	public double getV() {
		return v;
	}

	public void setV(double v) {
		this.v = v;
	}

	public double getR() {
		return r;
	}

	public void setR(double r) {
		this.r = r;
	}

	public double getL() {
		return l;
	}

	public void setL(double l) {
		this.l = l;
	}

	public double getL_1() {
		return l_1;
	}

	public void setL_1(double l_1) {
		this.l_1 = l_1;
	}

	public double getW() {
		return w;
	}

	public void setW(double w) {
		this.w = w;
	}

	public double getW_1() {
		return w_1;
	}

	public void setW_1(double w_1) {
		this.w_1 = w_1;
	}

	public double getH() {
		return h;
	}

	public void setH(double h) {
		this.h = h;
	}

	public double getH_1() {
		return h_1;
	}

	public void setH_1(double h_1) {
		this.h_1 = h_1;
	}

	public double getDt() {
		return dt;
	}

	public void setDt(double dt) {
		this.dt = dt;
	}

	public long getPrintStep() {
		return printStep;
	}

	public void setPrintStep(long printStep) {
		this.printStep = printStep;
	}

	public double getU_a() {
		return u_a;
	}

	public void setU_a(double u_a) {
		this.u_a = u_a;
	}

	public double getU_b() {
		return u_b;
	}

	public void setU_b(double u_b) {
		this.u_b = u_b;
	}

	public double getU_c() {
		return u_c;
	}

	public void setU_c(double u_c) {
		this.u_c = u_c;
	}

	public double getU_d() {
		return u_d;
	}

	public void setU_d(double u_d) {
		this.u_d = u_d;
	}

	public double getL_2() {
		return l_2;
	}

	public double getW_2() {
		return w_2;
	}

	public double getH_2() {
		return h_2;
	}

	public double getG() {
		return g;
	}

}
