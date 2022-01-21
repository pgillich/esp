package sim.app.esp;

import javax.media.j3d.Transform3D;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.Double3D;
import sim.util.MutableDouble2D;
import sim.app.esp.Esp;

public class Body implements Steppable {

	private static final long serialVersionUID = 1L;

	public MutableDouble2D location; // m
	public double alpha; // rad

	public MutableDouble2D v; // m/s
	
	public double omega; // 1/s
	public double omega_start;
	
	double T_x_corr = 0;
	
	public Body(Esp s, MutableDouble2D location, double alpha, MutableDouble2D v, double va, double r) 
	{
		super();
		this.location = location;
		this.alpha = alpha;
		this.v = v;
		omega_start = this.omega = va / r;
		
		s.out.printf("%7s %7s %7s %7s %7s %7s %7s %7s\n", "time", "x", "y", "v.x", "v.y", "alpha", "v_alpha", "F_cp");
	}

	@Override
	public void step(SimState s) 
	{
		Esp state = (Esp)s;
		double dt = state.dt;

		// Direction unit vector
		MutableDouble2D direction = state.newMutableDouble2D_Polar(1,alpha);

		// Side (external) unit vector
		MutableDouble2D direction_x = state.newMutableDouble2D_Polar(1,alpha-state.QUADRANT);
		
		// Direction part of v (length)
		double v_alpha = v.dot(direction);

		// Side part of v (length)
		double v_alpha_x = v.dot(direction_x);
		
		// Gravity
		double G = state.m * state.g;
		
		// Centripetal force (length)
		double F_cp = state.m * v_alpha*v_alpha / state.r;
		
	// Side slip
		
		// Max u for side slip (greater than 10% slip)
		double u_slip = state.u_max_sideSlip;
		
		// side slip
		double slip = 1 - v_alpha / v.length();
		state.sideSlip = slip;
		
		// u under 10% slip
		if(slip < state.sideSlip_u_max)
		{
			u_slip = slip * state.u_max_sideSlip / state.sideSlip_u_max;
		}
			
		// Slip force
		double F_slip = G * u_slip;
		
		// Add to Fcp (hacking)
		if(state.sideSlipEnabled)
		{
			F_cp += F_slip;
		}
	
	// Front/rear	
		
		// Calculating front/rear distribution of cetripetal force (x axis). A simple beam calculation
		MutableDouble2D F_cp_ABCD = state.simpleBeam(F_cp, state.l_1, state.l_2, 0);
		double F_cp_AB = F_cp_ABCD.x;
		double F_cp_DC = F_cp_ABCD.y;
		
		// Calculating front/rear distribution of gravity (y axis). A simple beam calculation
		MutableDouble2D G_ABCD = state.simpleBeam(G, state.l_1, state.l_2, T_x_corr);	
		double G_AB = G_ABCD.x;
		double G_DC = G_ABCD.y;
		
	// Front	
		
		// Rate of front x/y forces for ideal (non-slip) situation
		double fg_AB = F_cp_AB / G_AB;
		double fg_AB_alpha = Math.atan2(G_AB, F_cp_AB) * state.RAD_DEG;
		
		// Calculating right/left distribution of gravity on front.
		MutableDouble2D G_AB_ = state.simpleBeam(G_AB, state.w_1, state.w_2, 0);
		double G_A = G_AB_.x;
		double G_B = G_AB_.y;		
		
		// Calculation max. friction forces (x axis)
		double F_s_A_max = G_A * state.u(G_A); 
		double F_s_B_max = G_B * state.u(G_B); 
		
		// Required right/front friction force for stability (ideal)
		double F_A_x_0 = G_A * fg_AB;
		
		// Required left/front friction force for stability (ideal)
		double F_B_x_1 = G_B * fg_AB;
		
		// Limiting the left/front (internal side) friction force to max. friction force
		double F_B_x = F_B_x_1;
		if( state.frictionLimitEnabled && state.schedule.getTime() > state.frictionLimitStartTime )
		{
			F_B_x = Math.min(F_B_x_1, F_s_B_max);
		}
		
		// Required right/front (external side) friction force for stability
		double F_A_x_1 = F_cp_AB - F_B_x;
		
		// Limiting the right/front friction force to max. friction force
		double F_A_x = F_A_x_1;
		if( state.frictionLimitEnabled && state.schedule.getTime() > state.frictionLimitStartTime )
		{
			F_A_x = Math.min(F_A_x_1, F_s_A_max);
		}
		
		// Remained force for unstable moving (front)
		double F_AB_x = F_cp_AB - (F_A_x + F_B_x);
		
	// Rear	
		
		// Rate of rear x/y forces for ideal (non-slip) situation
		double fg_DC = F_cp_DC / G_DC;
		double fg_DC_alpha = Math.atan2(G_DC, F_cp_DC) * state.RAD_DEG;
		
		// Calculating right/left distribution of gravity on rear.
		MutableDouble2D G_DC_ = state.simpleBeam(G_DC, state.w_1, state.w_2, 0);
		double G_D = G_DC_.x;
		double G_C = G_DC_.y;
		
		// Calculation max. friction forces (x axis)
		double F_s_D_max = G_D * state.u(G_D); 
		double F_s_C_max = G_C * state.u(G_C); 
		
		// Required right/rear friction force for stability (ideal)
		double F_D_x_0 = G_D * fg_DC;

		// Required left/rear friction force for stability (ideal)
		double F_C_x_1 = G_C * fg_DC;
		
		// Limiting the left/rear (internal side) friction force to max. friction force
		double F_C_x = F_C_x_1;
		if( state.frictionLimitEnabled && state.schedule.getTime() > state.frictionLimitStartTime )
		{
			F_C_x = Math.min(F_C_x_1, F_s_C_max);
		}
		
		// Required right/rear (external side) friction force for stability
		double F_D_x_1 = F_cp_DC - F_C_x;
		
		// Limiting the right/rear friction force to max. friction force
		double F_D_x = F_D_x_1;
		if( state.frictionLimitEnabled && state.schedule.getTime() > state.frictionLimitStartTime )
		{
			F_D_x = Math.min(F_D_x_1, F_s_D_max);
		}
		
		// Remained force for unstable moving (rear)
		double F_DC_x = F_cp_DC - (F_D_x + F_C_x);
		
	// a, torque, v, location	
		
		// Remained force on x axis
		double F_x = F_AB_x + F_DC_x;
		
		// Acceleration on r
		double a_r = (F_cp - F_x) / state.m;
		
		// acceleration vector
		MutableDouble2D a = state.newMutableDouble2D_Polar( a_r, alpha+Esp.QUADRANT);
	
		// Rotating torque
		double T_y = F_DC_x * state.l_2 - F_AB_x * state.l_1;
		
		// Required front/right braking force
		double  F_b = T_y / state.w_1;
		
	// Correcting brake
		
		// Correcting force
		double F_corr = 0;
		if( state.correctingBreakEnabled && 
				state.schedule.getTime() > state.correctingBreakStartTime &&
				state.schedule.getTime() < state.correctingBreakStopTime
				)
		{
			F_corr = F_b * state.correctingBreakCoeff;
		}
		
		// Correcting torque
		double T_y_corr = -F_corr * state.w_1;
		T_x_corr = F_corr * state.h_1;
		
		T_y += T_y_corr;
		
		// Correcting acceleration
		MutableDouble2D a_corr = state.newMutableDouble2D_Polar( - F_corr / state.m, alpha);
		
		a.addIn(a_corr);
		
	// dt
		
		// speed change vector
		MutableDouble2D dv = new MutableDouble2D(a);
		dv.multiplyIn(dt);
		
		// new speed
		v.addIn(dv);

		// location change vector
		MutableDouble2D dlocation = new MutableDouble2D(v);
		dlocation.multiplyIn(dt);
		
		// new location
		location.addIn(dlocation);
		
		// new alpha
		double d_omega = T_y / state.I;
		omega += d_omega * dt;
		alpha += omega * dt;
		
		// debug
		
		if( state.schedule.getSteps() % state.printStep == 0 )
		{
			state.out.printf("%7.3f %7.3f %7.3f %7.3f %7.3f %7.1f %7.3f %7.0f\n", state.schedule.getTime(), location.x, location.y, v.x, v.y, alpha*Esp.RAD_DEG, v_alpha, F_cp);
			
			Double3D newLocation = new Double3D(location.x, location.y, 0);
			Transform3D rz = new Transform3D();
			rz.rotZ(alpha-Esp.QUADRANT);
			
			state.bodies.setObjectLocation(this, newLocation);
			if(state.wireFrameBoxRotated != null)
			{
				state.wireFrameBoxRotated.setTransform(rz);
			}

			Bag labelObjs = state.labels.getAllObjects();
			for(int lo=0; lo<labelObjs.numObjs; lo++)
			{
				if( "0".equals(labelObjs.get(lo)) )
				{
					state.labels.setObjectLocation("0",new Double3D(
							state.r * Math.cos(omega_start*state.schedule.getTime()),
							state.r * Math.sin(omega_start*state.schedule.getTime()), 0));
				}
				else
				{
					state.labels.setObjectLocation(labelObjs.get(lo), newLocation);
					
					if(labelObjs.get(lo) instanceof BodyLabel)
					{
						BodyLabel bl = (BodyLabel)labelObjs.get(lo);
						bl.portrayal.setTransform(rz);
					}
				}
			}
			
			for (BodyForce bf : state.forceMap.values()) 
			{
				Double3D fromNodeLocation = bf.transformFromNodeLocation(newLocation, alpha-Esp.QUADRANT);
				state.labels.setObjectLocation(bf.fromNodeObject,fromNodeLocation);
				
				if(bf.fromNodeObject.equals("F_G"))
				{
					setForces(state, bf, fromNodeLocation, -F_cp, 0, G);
				}
				else if(bf.fromNodeObject.equals("F_A"))
				{
					setForces(state, bf, fromNodeLocation, F_A_x, F_corr, -G_A);
				}
				else if(bf.fromNodeObject.equals("F_B"))
				{
					setForces(state, bf, fromNodeLocation, F_B_x, 0, -G_B);
				}
				else if(bf.fromNodeObject.equals("F_C"))
				{
					setForces(state, bf, fromNodeLocation, F_C_x, 0, -G_C);
				}
				else if(bf.fromNodeObject.equals("F_D"))
				{
					setForces(state, bf, fromNodeLocation, F_D_x, 0, -G_D);
				}
				else if(bf.fromNodeObject.equals("F_R"))
				{
					setForces(state, bf, fromNodeLocation, F_x, 0, 0);
				}				
			}
		}
		
		if(state.schedule.getTime() > state.endTime)
		{
			state.kill();
		}
	}

	public void setForces(Esp state, BodyForce bf, Double3D fromNodeLocation, double Fx, double Fy, double Fz)
	{
		Double3D toNodeXLocation = bf.transformToNodeLocation(fromNodeLocation, alpha-Esp.QUADRANT, Fx, 0, 0);
		state.labels.setObjectLocation(bf.toNodeXObject,toNodeXLocation);

		Double3D toNodeYLocation = bf.transformToNodeLocation(fromNodeLocation, alpha-Esp.QUADRANT, 0, Fy, 0);
		state.labels.setObjectLocation(bf.toNodeYObject,toNodeYLocation);

		Double3D toNodeZLocation = bf.transformToNodeLocation(fromNodeLocation, alpha-Esp.QUADRANT, 0, 0, Fz);
		state.labels.setObjectLocation(bf.toNodeZObject,toNodeZLocation);
	}
	
	public MutableDouble2D getLocation() {
		return location;
	}

	public double getAlpha() {
		return alpha;
	}

	public MutableDouble2D getV() {
		return v;
	}

	public double getOmega() {
		return omega;
	}

	double sin(double a)
	{
		return Math.sin(a);
	}

	double cos(double a)
	{
		return Math.cos(a);
	}
	
	double sqrt(double x)
	{
		return Math.sqrt(x);
	}
}
