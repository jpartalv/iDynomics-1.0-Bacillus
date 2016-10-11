/**
 * Project iDynoMiCS (copyright -> see Idynomics.java)
 * 
 */

/**
 * ______________________________________________________
 * @since June 2006
 * @version 1.0
 * @author Andreas Dötsch (andreas.doetsch@helmholtz-hzi.de), Helmholtz Centre for Infection Research (Germany)
 * @author Laurent Lardon (lardonl@supagro.inra.fr), INRA, France
 * @author Sónia Martins (SCM808@bham.ac.uk), Centre for Systems Biology, University of Birmingham (UK)
 */

package simulator.agent;

import idyno.SimTimer;

import java.util.Iterator;
import java.util.LinkedList;
import java.awt.Color;

import utils.ExtraMath;
import utils.LogFile;

import simulator.*;
import simulator.geometry.CollisionEngine;
import simulator.geometry.ContinuousVector;
import simulator.geometry.Domain;
import simulator.geometry.EuclideanVector;
import simulator.geometry.Quaternion;
import simulator.geometry.boundaryConditions.AllBC;

public abstract class LocatedAgent extends ActiveAgent implements Cloneable {

	/* Temporary variables stored in static fields __________________________ */
	/**
	 * @uml.property  name="_diff"
	 * @uml.associationEnd  
	 */
	protected static ContinuousVector  _diff              = new ContinuousVector();
	/**
	 * @uml.property  name="_newLoc"
	 * @uml.associationEnd  
	 */
	protected static ContinuousVector  _newLoc            = new ContinuousVector();

	/* Parameters specific to the agent _____________________________________ */
	protected double                   _radius;
	protected double _totalRadius;
	protected double                   _volume;
	protected double _totalVolume;
	public double				   _capsular_radius = 0.389171508;//0.002948269; //aprox. 1 / 4 of the initial longitude of the bacteria	
	//protected ContinuousVector[]       _orientation;

	/* Agent's location ____________________________________________________ */
	// Agent position and agent movement are expressed with continuous
	// coordinates
	/**
	 * @uml.property  name="_location"
	 * @uml.associationEnd  
	 */
	protected ContinuousVector         _location          = new ContinuousVector();
	
	/**
	 * @uml.property  name="_headLocation"
	 * @uml.associationEnd  
	 */
	protected ContinuousVector         _headLocation          = new ContinuousVector();
	/**
	 * @uml.property  name="_tailLocation"
	 * @uml.associationEnd  
	 */
	protected ContinuousVector         _tailLocation          = new ContinuousVector();
	
	/**
	 * @uml.property  name="_movement"
	 * @uml.associationEnd  
	 */
	protected ContinuousVector         _movement          = new ContinuousVector();
	
	/**
	 * @uml.property  name="_divisionDirection"
	 * @uml.associationEnd  
	 */
	protected ContinuousVector         _divisionDirection = new ContinuousVector();
	protected LinkedList<LocatedAgent> _myNeighbors       = new LinkedList<LocatedAgent>();

	// Index of the agent position on the vectorized grid
	protected int                      _agentGridIndex;
	protected boolean                  _isAttached        = false;

	// Detachment priority
	public double                       detPriority = 0;

	// for timestep issues
	public double                      _timeSinceLastDivisionCheck = Double.MAX_VALUE;

	//sonia 8-12-2010
	// distance based probability from a given neighbour (used in HGT)
	public double					_distProb = 0; 								
	public double					_distCumProb = 0; 	


	/* _______________________ CONSTRUCTOR _________________________________ */

	/**
	 * Empty constructor
	 */
	public LocatedAgent() {
		super();
		_speciesParam = new LocatedParam();
	}
	
	public void randomizeOrientation()
	{
		if (_agentGrid.is3D)
			_headLocation = new ContinuousVector(0, ExtraMath.random.nextDouble() , ExtraMath.random.nextDouble()); //random orientation; 
		else 
			_headLocation = new ContinuousVector(0.5 , 0.5,0); //random orientation;

		_tailLocation = new ContinuousVector(0, 0, 0);

	}

	@SuppressWarnings("unchecked")
	public Object clone() throws CloneNotSupportedException {
		LocatedAgent o = (LocatedAgent) super.clone();

		o._location = (ContinuousVector) this._location.clone();
		o._headLocation = (ContinuousVector) this._headLocation.clone();
		o._tailLocation = (ContinuousVector) this._tailLocation.clone();
		
		
		o._movement = (ContinuousVector) this._movement.clone();
		o._divisionDirection = (ContinuousVector) this._divisionDirection
		.clone();
		o._myNeighbors = (LinkedList<LocatedAgent>) this._myNeighbors.clone();

		o._agentGridIndex = this._agentGridIndex;

		return (Object) o;
	}

	/**
	 * Create a new agent with mutated parameters based on species default
	 * values and specifies its position
	 */
	/**
	 * Create an agent (who a priori is registered in at least one container;
	 * this agent is located !
	 */
	//default orientation is x positive
	public void createNewAgent(ContinuousVector position) {
		try {
			// Get a clone of the progenitor
			LocatedAgent baby = (LocatedAgent) sendNewAgent();
			baby.giveName();

			// randomize its mass
			baby.mutatePop();
			baby.updateSize();

			// Just to avoid to be in the carrier
			position.x += this._totalRadius;
			baby.setLocation(position);
			baby.registerBirth();

		} catch (CloneNotSupportedException e) {
			utils.LogFile.writeLog("Error met in LocAgent:createNewAgent()");
		}
	}

	
	protected void updateOrientationVector(double angle)
	{
		_headLocation = new ContinuousVector(this._location.x, this._location.y + this._radius - this._capsular_radius, this._location.z); 
		_tailLocation = new ContinuousVector(this._location.x, this._location.y - this._radius + this._capsular_radius, this._location.z);
	
		ContinuousVector[] vo = new ContinuousVector[2];
		vo[0] = this._location;
		vo[1] = new ContinuousVector(this._location.x+1,this._location.y,this._location.z+1);
		
		ContinuousVector[] v = new ContinuousVector[2]; 
		v[0] = this._location;
		
		angle = Math.toRadians(90) ;//Math.random(); //in radians
		
		//rotate head from center
		v[1] = this._headLocation;
		this._headLocation = RotateVector(angle,v,vo);
		
		//rotate tail from center
		v[1] = this._tailLocation;
		this._tailLocation = RotateVector(angle,v,vo);
	}

	/**
	 * Register the agent on the agent grid and on the guilds
	 */
	public void registerBirth() {
		// Register on species and reaction guilds
		super.registerBirth();
	}

	public void initFromResultFile(Simulator aSim, String[] singleAgentData) {
		// this routine will read data from the end of the singleAgentData array
		// and then pass the remaining values onto the super class

		// Chemostat "if" added by Sonia 27.10.09
		// Rearranged by Rob 10.01.11

		// find the position to start at by using length and number of values read
		int nValsRead = 5;
		int iDataStart = singleAgentData.length - nValsRead;

		if(Simulator.isChemostat){

			// Rob: this is necessary for the case when biofilm agents in one simulation
			// are transferred into a chemostat for the next.
			_location.set(0, 0, 0);

		}else{

			double newAgentX, newAgentY, newAgentZ;
			newAgentX = Double.parseDouble(singleAgentData[iDataStart]);
			newAgentY = Double.parseDouble(singleAgentData[iDataStart+1]);
			newAgentZ = Double.parseDouble(singleAgentData[iDataStart+2]);
			_location.set(newAgentX, newAgentY, newAgentZ);

		}

		// agent size
		_radius      = Double.parseDouble(singleAgentData[iDataStart+3]);
		_totalRadius = Double.parseDouble(singleAgentData[iDataStart+4]);
		
		// now go up the hierarchy with the rest of the data
		String[] remainingSingleAgentData = new String[iDataStart];
		for (int i=0; i<iDataStart; i++)
			remainingSingleAgentData[i] = singleAgentData[i];

		super.initFromResultFile(aSim, remainingSingleAgentData);
	}

	/* _____________________HIGH-LEVEL METHODS _____________________________ */

	/**
	 * Called at each time step (under the control of the method Step of the
	 * class Agent to avoid multiple calls
	 */
	protected void internalStep() {
		// Compute mass growth over all compartments
		grow();

		// Apply this mass growth of all compounds on global radius and mass
		updateSize();

		// Divide if you have to
		if (willDivide())
			divide();

		// Die if you have to
		if (willDie())
			die(true);
	}

	/**
	 * Update the radius of the agent from the current mass (and then the
	 * volume) of the agent (EPS included)
	 */
	public void updateSize() {
		// Update the totalMass field (sum of the particles masses)
		updateMass();
		if (_totalMass < 0)
			LogFile.writeLog("Warning: negative mass on agent "+_family+", "+_genealogy);

		// Sum of (particles masses / particles density)
		updateVolume();

		// Compute radius according to the volume
		updateRadius();

		//sonia:chemostat
		if(Simulator.isChemostat){
			//don't do the update of attachment/detachment 

		}else{

			// Check if by chance the agent is close enough to a support to be
			// attached

			updateAttachment();
		}
	}

	/**
	 * 
	 */
	public void divide() {
		try {
			// Create a daughter cell
			makeKid();
		} catch (CloneNotSupportedException e) {
			LogFile.writeLog("Error met in LocatedAgent.divide()");
		}
	}

	public boolean willDivide() {
		//jan: commented out since the logic of our simple cell division rule is divide if big enough
		//if (_netGrowthRate<=0) return false;
		// this ensures that the checks for when to divide don't occur too often;
		// at most they will occur at the rate of AGENTTIMESTEP
		_timeSinceLastDivisionCheck += SimTimer.getCurrentTimeStep();
		if (_timeSinceLastDivisionCheck < _agentGrid.getAgentTimeStep())
			return false;

		// at this point we will actually check whether to divide
		_timeSinceLastDivisionCheck = 0;

		return getRadius(false) > ExtraMath.deviateFrom(
				getSpeciesParam().divRadius, getSpeciesParam().divRadiusCV);
	}

	public boolean willDie() {
		if (_totalMass < 0)
			return true;
		return getRadius(false) <= ExtraMath.deviateFrom(
				getSpeciesParam().deathRadius,
				getSpeciesParam().deathRadiusCV);
	}

	/**
	 * Kill an agent. Called by detachment and starving test
	 */
	public void die(boolean isStarving) {
		super.die(isStarving);
	}

	/* ________________________________________________________________ */
	/**
	 * Create a new agent from an existing one
	 * 
	 * @throws CloneNotSupportedException
	 *             Called by LocatedAGent.divide()
	 */
	public void makeKid() throws CloneNotSupportedException {

		// Create the new instance
		LocatedAgent baby = (LocatedAgent) sendNewAgent();
		// Note that mutateAgent() does nothing yet
		baby.mutateAgent();
		
		// Update the lineage
		recordGenealogy(baby);

		// Share mass of all compounds between two daughter cells and compute
		double splitRatio =  getBabyMassFrac();
		divideCompounds(baby, splitRatio);

		// Now register the agent inside the guilds and the agent grid
		baby.registerBirth();
		baby._netVolumeRate = 0;

	}

	public double deltaMass = 0;
	public void divideCompounds(LocatedAgent baby, double splitRatio) {
		// Choose the division plan and apply position modifications
	
		this._radius *= splitRatio;
		baby._radius *= 1- splitRatio;
		
		//relocate centers
		EuclideanVector orientation = new EuclideanVector(_tailLocation,_headLocation);
		orientation = orientation.Normalize();
		orientation = orientation.Times(_radius /*+ _capsular_radius*/);
		
		_location.add(orientation.getContinuousVector());
		
		orientation = new EuclideanVector(baby._tailLocation,baby._headLocation);
		orientation = orientation.Normalize();
		orientation = orientation.Times(baby._radius /*+ _capsular_radius*/);
		baby._location.subtract(orientation.getContinuousVector());
		
		//add slight Variation on orientation
		//ContinuousVector end = new ContinuousVector(ExtraMath.random.nextFloat(),ExtraMath.random.nextFloat(),ExtraMath.random.nextFloat());
		
		double proportion = 0.2;
		
		if (this._agentGrid.is3D)
		{
			torque.mag_y = ExtraMath.random.nextDouble();
			torque.mag_z = ExtraMath.random.nextDouble();
		}	
		else
		{
			if (ExtraMath.random.nextDouble() > 0.5)
				torque.mag_z = 1;
			else
				torque.mag_z = -1;
		}
		
		rotationAngle = ExtraMath.random.nextDouble() * proportion; //multiply by a PROTOCOL magnitude
		
		for (int i = 0; i<particleMass.length; i++) {
			baby.particleMass[i] *= splitRatio;
			this.particleMass[i] *= 1-splitRatio;
		}
	
		// Update radius, mass and volumes
		updateSize();
		baby.updateSize();
	}

	public void transferCompounds(LocatedAgent baby, double splitRatio) {
		// Choose the division plan and apply position modifications
		double m;
		for (int i = 0; i<particleMass.length; i++) {
			m = this.particleMass[i]*splitRatio;
			baby.particleMass[i] += m;
			this.particleMass[i] = this.particleMass[i]-m;
		}

		// Update radius, mass and volumes
		updateSize();
		baby.updateSize();
	}

	public void mutatePop() {
		// Mutate parameters inherited
		super.mutatePop();
		// Now mutate your parameters
	}

	/**
	 * Set movement vector to put a new-created particle
	 * 
	 * @param myBaby
	 * @param distance
	 */
	public void setDivisionDirection(double distance) {
		double phi, theta;

		phi = 2*Math.PI*ExtraMath.getUniRand();
		theta = 2*Math.PI*ExtraMath.getUniRand();

		_divisionDirection.x = distance*Math.sin(phi)*Math.cos(theta);
		_divisionDirection.y = distance*Math.sin(phi)*Math.sin(theta);
		_divisionDirection.z =(_agentGrid.is3D ? distance*Math.cos(phi):0);
	}

	/* ______________________ SHOVING ___________________________________ */

	/**
	 * Mechanical interaction between two located agents
	 * 
	 * @param aGroup :
	 *            neighbourhood of the agent
	 * @param MUTUAL :
	 *            movement shared between 2 agents or applied only to this one
	 * @pull : false for shoving, true for pulling (shrinking biofilm)
	 * @seq : apply immediately the movement or waits the end of the step
	 */
	public double interact(boolean MUTUAL, boolean shoveOnly, boolean seq,
			double gain) {

		move();
		
		// rebuild your neighbourhood
		if (shoveOnly)
			getPotentialShovers(getInteractDistance());
		else
			getPotentialShovers(getInteractDistance() + getShoveRadius());

		Iterator<LocatedAgent> iter = _myNeighbors.iterator();
		while (iter.hasNext()) {
			if (shoveOnly)
				addPushMovement(iter.next(), MUTUAL, gain);
			else
				addSpringMovement(iter.next(), MUTUAL, gain);

		}
		_myNeighbors.clear();

		// Check interaction with surface
		if (_isAttached&!shoveOnly) {

		}

		isMoving();
				
		if (seq)
		{
			//rotate();
			return move();
		}
		else
			return 0;
	}

	/**
	 * Mutual shoving : The movement by shoving of an agent is calculated based
	 * on the cell overlap and added to the agents movement vector. Both agents
	 * are moved of half the overlapping distance in opposite directions.
	 * 
	 * @param aNeighbour
	 *            reference to the potentially shoving neighbour
	 * @return true, if a shoving is detected
	 */
	public boolean addPushMovement(LocatedAgent aNeighbour, boolean isMutual,
			double gain) {
		
		if (aNeighbour == this)
			return false;
		
		/* verify intersection of capsules */
		EuclideanVector bactMe = new EuclideanVector(_tailLocation,_headLocation);
		EuclideanVector bactHim = new EuclideanVector(aNeighbour._tailLocation,aNeighbour._headLocation);
		
		double dotProduct = bactMe.DotProduct(bactHim);
   		double angle = dotProduct / (bactMe.magnitude * bactHim.magnitude); 
    	
   		// if vector are in opposite direction the algorithm fails.
   		if (angle < 0)
   			bactHim = new EuclideanVector(aNeighbour._headLocation,aNeighbour._tailLocation);	
		
   		boolean theyIntersect = CollisionEngine.TestCapsuleCapsule(bactMe, bactHim, _capsular_radius, aNeighbour._capsular_radius);
		if (!theyIntersect)
		{
			return false;
		}
		else
		{
			//calculate translation and rotation
			EuclideanVector forceMe = new EuclideanVector(CollisionEngine.intersectionPointsV[0],CollisionEngine.intersectionPointsV[1]);
			double newMag = forceMe.magnitude - (2* _capsular_radius);
			forceMe = forceMe.Normalize();
			forceMe = new EuclideanVector(forceMe.start,forceMe.mag_x * newMag, 
			forceMe.mag_y * newMag, forceMe.mag_z * newMag);
			
			//forceMe.Times(0.5f);
				
			double[] _center = {_location.x,_location.y,_location.z};
			EuclideanVector N = new  EuclideanVector(forceMe.end,_center);
			EuclideanVector T = forceMe.CrossProduct(N);;
			this.rotationAngle += /*=*/ CollisionEngine.applyForceToCapsule(
			this._location, new EuclideanVector(_tailLocation,_headLocation),
			_capsular_radius, forceMe, -1, null);
			torque = /*T;*/torque.Plus(T);
			//System.out.println(this.rotationAngle+"");
						
			if (isMutual) {
				forceMe.Times(0.5f);
				this.rotationAngle *= 0.5;
				forceMe = forceMe.Times(gain);
				this._movement.add(forceMe.mag_x,forceMe.mag_y,forceMe.mag_z);

				aNeighbour.rotationAngle -= rotationAngle; //= rotationAngle;
				aNeighbour.torque = /*T;*/aNeighbour.torque.Minus(T);

				aNeighbour.rotationAngle *= 0.5;
				aNeighbour._movement.subtract(forceMe.mag_x,forceMe.mag_y,forceMe.mag_z);
			} else {
				forceMe = forceMe.Times(gain);
				this._movement.add(forceMe.mag_x,forceMe.mag_y,forceMe.mag_z);
			}
		 
			return true;
		}
	}
	
	/**
	 * 
	 * @param aNeighbor
	 * @param isMutual
	 * @return
	 */
	public boolean addSpringMovement(LocatedAgent aNeighbour, boolean isMutual,
			double gain) {

		if (aNeighbour == this)
			return false;
		

		/* verify intersection of capsules */
		EuclideanVector bactMe = new EuclideanVector(_tailLocation,_headLocation);
		EuclideanVector bactHim = new EuclideanVector(aNeighbour._tailLocation,aNeighbour._headLocation);
		
		double dotProduct = bactMe.DotProduct(bactHim);
   		double angle = dotProduct / (bactMe.magnitude * bactHim.magnitude); 
    	
   		// if vector are in opposite direction the algorithm fails.
   		if (angle < 0)
   			bactHim = new EuclideanVector(aNeighbour._headLocation,aNeighbour._tailLocation);	
		
		boolean theyIntersect = CollisionEngine.TestCapsuleCapsule(bactMe, bactHim, _capsular_radius, aNeighbour._capsular_radius);
		
		float springFactor = 0.25f;
		if (!theyIntersect)
		{
			//if they don't intersect we need to exert an attractive force.
			EuclideanVector force = new EuclideanVector(CollisionEngine.intersectionPointsV[0],CollisionEngine.intersectionPointsV[1]);
			// if bacteria are close enough make them get closer.
			if (force.magnitude < (this._capsular_radius * 3))
			{
				EuclideanVector orientation = force.Normalize(); 
				if (isMutual) {
					double forceMag = _capsular_radius * 0.5 * springFactor;
					this._movement.add(orientation.mag_x * forceMag,
							orientation.mag_y * forceMag,
							orientation.mag_z * forceMag);
					aNeighbour._movement.add(-orientation.mag_x * forceMag,
							-orientation.mag_y * forceMag,
							-orientation.mag_z * forceMag);
				} else {
					double forceMag = _capsular_radius * springFactor;
					this._movement.add(orientation.mag_x * forceMag,
							orientation.mag_y * forceMag,
							orientation.mag_z * forceMag);
				}
			}

			return false; // they don't intersect at all.
		}
		
		else
		{
			//apply rotation
				EuclideanVector forceMe = new EuclideanVector(CollisionEngine.intersectionPointsV[0],CollisionEngine.intersectionPointsV[1]);
				/**/
				double newMag = forceMe.magnitude - (2* _capsular_radius);
				forceMe = forceMe.Normalize();
				forceMe = new EuclideanVector(forceMe.start,forceMe.mag_x * newMag, 
						forceMe.mag_y * newMag, forceMe.mag_z * newMag);
				/**/
				forceMe.Times(0.5f);
				
				double[] _center = {_location.x,_location.y,_location.z};
				EuclideanVector N = new  EuclideanVector(forceMe.end,_center);
				//EuclideanVector T = force.CrossProduct(N);
				EuclideanVector T = forceMe.CrossProduct(N);;
				this.rotationAngle += CollisionEngine.applyForceToCapsule(
						this._location, new EuclideanVector(_tailLocation,_headLocation),
						_capsular_radius, forceMe, -1, null);
				torque = torque.Plus(T);

				EuclideanVector forceHim = new EuclideanVector(CollisionEngine.intersectionPointsV[1],CollisionEngine.intersectionPointsV[0]);
				/**/
				forceHim = forceHim.Normalize();
				forceHim = new EuclideanVector(forceHim.end,forceHim.mag_x * newMag, 
						forceHim.mag_y * newMag, forceHim.mag_z * newMag);
				/**/
				forceHim.Times(0.5f);
				
				EuclideanVector T2 = forceHim.CrossProduct(N);;
				aNeighbour.rotationAngle -= CollisionEngine.applyForceToCapsule(
						aNeighbour._location, new EuclideanVector(aNeighbour._tailLocation,aNeighbour._headLocation),
						aNeighbour._capsular_radius, forceHim, -1, null);
				aNeighbour.torque = aNeighbour.torque.Plus(T2);
		
			
		if (isMutual) {
				forceMe = forceMe.Times(gain);
				this._movement.add(forceMe.mag_x,forceMe.mag_y,forceMe.mag_z);

				forceHim = forceHim.Times(gain);
				aNeighbour._movement.add(forceHim.mag_x,forceHim.mag_y,forceHim.mag_z);
			} else {
				forceMe = forceMe.Times(2).Times(gain);
				this._movement.add(forceMe.mag_x,forceMe.mag_y,forceMe.mag_z);
			}

			//return true;
			return (_movement.norm()>_radius*gain);
		}
		
		
	}

	/**
	 * 
	 * @return
	 */
	public boolean addSpringAttachment() {
		AllBC mySupport = updateAttachment();
		double d, distance, delta;

		d = computeDifferenceVector(_location, mySupport
				.getOrthoProj(_location));
		_diff.normalizeVector();

		distance = _totalRadius*getShoveFactor();
		delta = d-distance;

		/* Apply elastic interaction _______________________________________ */
		double gain = 0.1;
		if (delta < 0)
			gain = 0.1;
		if (delta > 0)
			gain = 0.1;
		if (delta > _totalRadius)
			gain = 0;

		_diff.times(-delta*gain);
		this._movement.add(_diff);

		if (_movement.norm()>_radius*0.1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param ContinuousVector
	 *            a location
	 * @param ContinuousVector
	 *            a location
	 * @return the shortest movement vector to go from a to b, take into account
	 * the cyclic boundary
	 * @see addOverlapMovement
	 * @see addPullMovement works in 2 and 3D
	 */
	public double computeDifferenceVector(ContinuousVector me,
			ContinuousVector him) {
		double gridLength;

		_diff.x = me.x-him.x;
		// check periodicity in X
		gridLength = _species.domain.length_X;
		if (Math.abs(_diff.x) > .5 * gridLength)
			_diff.x -= Math.signum(_diff.x) * gridLength;

		_diff.y = me.y-him.y;
		// check periodicity in Y
		gridLength = _species.domain.length_Y;

		if (Math.abs(_diff.y) > .5 * gridLength)
			_diff.y -= Math.signum(_diff.y) * gridLength;

		if (_agentGrid.is3D) {
			_diff.z = me.z-him.z;
			// check periodicity in Z
			gridLength = _species.domain.length_Z;
			if (Math.abs(_diff.z) > .5 * gridLength)
				_diff.z -= Math.signum(_diff.z) * gridLength;

		} else {
			_diff.z = 0;
		}
		double d = Math.sqrt(_diff.x * _diff.x + _diff.y * _diff.y + _diff.z
				* _diff.z);

		if (d==0) {
			d = 1e-2*_radius;
			_diff.alea(_agentGrid.is3D);
		}

		return d;
	}
	
	public double computeCapsularDifferenceVector(ContinuousVector me_tail,
			ContinuousVector me_head, double me_capsularRadius, 
			ContinuousVector him_tail, ContinuousVector him_head,
			double him_capsularRadius) 
	{
		EuclideanVector me = new EuclideanVector(me_tail, me_head);
		EuclideanVector him = new EuclideanVector(him_tail, him_head);
		boolean collide = CollisionEngine.TestCapsuleCapsule(me, him, me_capsularRadius, him_capsularRadius);
		
		if (collide)
		{
			return CollisionEngine.distance;
		}
		else
		{
			him = new EuclideanVector(him_head, him_tail);
			CollisionEngine.TestCapsuleCapsule(me, him, me_capsularRadius, him_capsularRadius);
			return CollisionEngine.distance;		
		}
	}

	
	/**
	 * Look for neighbours in a range around you
	 */
	public void getPotentialShovers(double radius) {
		_agentGrid.getPotentialShovers(_agentGridIndex, radius, _myNeighbors);
	}

	/**
	 * Pick randomly a Neighbor from the _myNeigbors collection
	 * 
	 * @return
	 */
	public LocatedAgent pickNeighbor() {
		if (_myNeighbors.isEmpty())
			return null;
		else
			return _myNeighbors.get(ExtraMath.getUniRandInt(0, _myNeighbors
					.size()));
	}

	/**
	 * Find a sibling
	 * 
	 * @param indexSpecies
	 * @return
	 */
	public void findCloseSiblings(int indexSpecies) {
		int nNb;
		boolean test;
		double shoveDist;
		LocatedAgent aNb;

		getPotentialShovers(getInteractDistance());
		nNb = _myNeighbors.size();

		for (int iNb = 0; iNb<nNb; iNb++) {
			aNb = _myNeighbors.removeFirst();
			// test EPS-species
			test = (indexSpecies==aNb.speciesIndex);

			// Test distance
			shoveDist = 2*(getShoveRadius()+aNb.getShoveRadius());
			test = test
			&& computeDifferenceVector(_location, aNb.getLocation()) <= shoveDist;

			if (test & aNb != this)
				_myNeighbors.addLast(aNb);
		}
	}

	double rotationAngle = 0;
	/**
	 * @uml.property  name="torque"
	 * @uml.associationEnd  
	 */
	EuclideanVector torque = new EuclideanVector(_location,_location);
	
	/**
	 * Apply the rotation angle stored 
	 */
	public void rotate()
	{
	
		if (rotationAngle > 0 && torque.magnitude > 0 && 
				rotationAngle < 0.6)
		{ 
			ContinuousVector[] center_head = {_location,_headLocation};
			ContinuousVector[] center_tail = {_location,_tailLocation};
			ContinuousVector[] T1 = {
	        		new ContinuousVector(torque.start[0],torque.start[1],torque.start[2]),
	        		new ContinuousVector(torque.start[0]+torque.mag_x,
	        				torque.start[1] + torque.mag_y,
	        				torque.start[2] + torque.mag_z)};
	     
		    ContinuousVector newHead = RotateVector(rotationAngle,center_head,T1);
		    ContinuousVector newTail = RotateVector(rotationAngle,center_tail,T1);
	   
		    
		    if (!Double.isNaN(newHead.x) && !Double.isNaN(newTail.x))
		    {
		    	this._headLocation = newHead; 
		    	this._tailLocation = newTail;
		    }
		}
		
		rotationAngle = 0;
		torque = new EuclideanVector(_location,_location);
	}
	
	
	/**
	 * Apply the movement stored taking care to respect boundary conditions
	 */
	public double move() {
		if (!_movement.isValid()) {
			LogFile.writeLog("Incorrect movement coordinates");
			_movement.reset();
		}

		if (!_agentGrid.is3D&&_movement.z!=0) {
			_movement.z = 0;
			_movement.reset();
			LogFile.writeLog("Try to move in z direction !");
		}

		//even if there is no movement there could be intersection with the boundaries
		//from growth
		checkBoundariesTailHead();
		_headLocation.add(_movement);
		_tailLocation.add(_movement);
		_location.add(_movement);
		
		//sometimes the capsule is still in a non valid position
		
		AllBC aBoundary = getDomain().testCrossedBoundary(_location);
		boolean testCenter = (aBoundary!=null);
		
		if (testCenter)
		{
			EuclideanVector force = 
			 new EuclideanVector(_location,aBoundary.getOrthoProj(_location));

			
			 //we need a vector pointing inside with the size of the capsular radius
			 ContinuousVector forceNormal = new ContinuousVector(0,0,0); 
			 forceNormal = aBoundary.getShape().getNormalInside(forceNormal);
			 forceNormal.normalizeVector();
			 forceNormal.times(_capsular_radius);
			 force.mag_x += forceNormal.x;
			 force.mag_y += forceNormal.y;
			 force.mag_z += forceNormal.z;
			 
			 _headLocation.add(force.getContinuousVector());
			 _tailLocation.add(force.getContinuousVector());
			 _location.add(force.getContinuousVector());
			 _movement.add(force.getContinuousVector());
		}

		_agentGrid.registerMove(this);

		double delta = _movement.norm();
		_movement.reset();

		return delta/_totalRadius;
	}
	
	/**
	 * @uml.property  name="_movementVector"
	 * @uml.associationEnd  multiplicity="(0 -1)"
	 */
	protected ContinuousVector[] _movementVector; 
	
	
	public void checkBoundariesCenter() {
		// Search a boundary which will be crossed
		_newLoc.set(_location);
		_newLoc.add(_movement);
		AllBC aBoundary = getDomain().testCrossedBoundary(_newLoc);
		int nDim = (_agentGrid.is3D ? 3 : 2);
		boolean test = (aBoundary!=null);
		int counter = 0;

		// Test all boundaries and apply corrections according to crossed
		// boundaries
		while (test) {
			counter++;
			aBoundary.applyBoundary(this, _newLoc);
			aBoundary = getDomain().testCrossedBoundary(_newLoc);

			test = (aBoundary!=null)|(counter>nDim);
			if (counter > nDim)
				System.out.println("LocatedAgent.move() : problem!");
		}
	}
	
	
	public void checkBoundariesTailHead() {
	
		ContinuousVector _newTailLoc = new ContinuousVector();
		_newTailLoc.set(_tailLocation);
		_newTailLoc.add(_movement);
		
		ContinuousVector _newHeadLoc = new ContinuousVector();
		_newHeadLoc.set(_headLocation);
		_newHeadLoc.add(_movement);
		
		AllBC aBoundaryT = getDomain().testCrossedBoundary(_newTailLoc);
		AllBC aBoundaryH = getDomain().testCrossedBoundary(_newHeadLoc);
		
		boolean testHead = (aBoundaryH!=null);
		boolean testTail = (aBoundaryT!=null);
		
		
		if (testHead)
		{
			 EuclideanVector force = 
				 new EuclideanVector(_headLocation,aBoundaryH.getOrthoProj(_headLocation));

			 //we need a vector pointing inside with the size of the capsular radius
			 ContinuousVector forceNormal = new ContinuousVector(0,0,0); 
			 forceNormal = aBoundaryH.getShape().getNormalInside(forceNormal);
			 forceNormal.normalizeVector();
			 forceNormal.times(_capsular_radius);
			 force.mag_x += forceNormal.x;
			 force.mag_y += forceNormal.y;
			 force.mag_z += forceNormal.z;
			 
			 _movement.add(force.getContinuousVector());
			 
			
			 double[] _center = {_location.x,_location.y,_location.z};
				EuclideanVector N = new  EuclideanVector(force.end,_center);
				EuclideanVector T = force.CrossProduct(N);;
				this.rotationAngle += CollisionEngine.applyForceToCapsule(
						this._location, new EuclideanVector(_tailLocation,_headLocation),
						_capsular_radius, force, -1, null);
				torque = torque.Plus(T);
		}
		
		if (testTail)
		{
			 EuclideanVector force = 
				 new EuclideanVector(_tailLocation,aBoundaryT.getOrthoProj(_tailLocation));
		
		
			 //we need a vector pointing inside with the size of the capsular radius
			 ContinuousVector forceNormal = new ContinuousVector(0,0,0); 
			 forceNormal = aBoundaryT.getShape().getNormalInside(forceNormal);
			 forceNormal.normalizeVector();
			 forceNormal.times(_capsular_radius);
			 force.mag_x += forceNormal.x;
			 force.mag_y += forceNormal.y;
			 force.mag_z += forceNormal.z;
			 //force.Plus(_capsular_radius,_capsular_radius,_capsular_radius);
			 
			 _movement.add(force.getContinuousVector());
			 
			 double[] _center = {_location.x,_location.y,_location.z};
				EuclideanVector N = new  EuclideanVector(force.end,_center);
				EuclideanVector T = force.CrossProduct(N);;
				this.rotationAngle += CollisionEngine.applyForceToCapsule(
						this._location, new EuclideanVector(_tailLocation,_headLocation),
						_capsular_radius, force, -1, null);
				torque = torque.Plus(T);
		}

	}
	
	/* ____________________CELL DIVISION __________________________________ */

	/**
	 * Mutation Function If you don't want apply a mutation in a specified
	 * class, do not redefine this method. If you want, you are free to choose
	 * which fields to mutate for each different class by a simple redefinition
	 * 
	 * @param alea
	 */
	public void mutateAgent() {
		// Mutate parameters inherited
		super.mutateAgent();
		// Now mutate your parameters
	}

	/**
	 * Add the reacting CONCENTRATION of an agent on the received grid
	 * 
	 * @param aSpG :
	 *            grid used to sum catalysing mass
	 * @param catalyst
	 *            index : index of the compartment of the cell supporting the
	 *            reaction
	 */
	public void fitMassOnGrid(SpatialGrid aSpG, int catalystIndex) {
		if (isDead)
			return;

		double value = particleMass[catalystIndex]/aSpG.getVoxelVolume();
		if (Double.isNaN(value) | Double.isInfinite(value))
			value = 0;
		aSpG.addValueAt(value, _location);
	}

	/**
	 * Add the total CONCENTRATION of an agent on received grid
	 * 
	 * @param aSpG :
	 *            grid used to sum catalysing mass
	 */
	public void fitMassOnGrid(SpatialGrid aSpG) {
		if (isDead)
			return;

		double value = _totalMass/aSpG.getVoxelVolume();
		if (Double.isNaN(value) | Double.isInfinite(value))
			value = 0;
		aSpG.addValueAt(value, _location);
	}

	public void fitVolRateOnGrid(SpatialGrid aSpG) {
		double value;
		value = _netVolumeRate/aSpG.getVoxelVolume();
		if (Double.isNaN(value) | Double.isInfinite(value))
			value = 0;
		aSpG.addValueAt(value, _location);
	}

	public void fitReacRateOnGrid(SpatialGrid aRateGrid, int reactionIndex) {
		if (isDead)
			return;

		// growthRate is in [fgX.hr-1] so convert to concentration:
		// [fgX.um-3.hr-1 = gX.L-1.hr-1]
		double value = growthRate[reactionIndex]/aRateGrid.getVoxelVolume();

		if (Double.isNaN(value) | Double.isInfinite(value))
			value = 0;

		aRateGrid.addValueAt(value, _location);
	}

	/* _______________ FILE OUTPUT _____________________ */


	public String sendHeader() {
		// return the header file for this agent's values after sending those for super
		StringBuffer tempString = new StringBuffer(super.sendHeader());
		tempString.append(",");

		// location info and radius
		tempString.append("locationX,locationY,locationZ,radius,totalRadius");

		return tempString.toString();
	}

	public String writeOutput() {
		// write the data matching the header file
		StringBuffer tempString = new StringBuffer(super.writeOutput());
		tempString.append(",");

		// location info and radius
		tempString.append(_location.x+","+_location.y+","+_location.z+",");
		tempString.append(_radius+","+_totalRadius);

		return tempString.toString();
	}

	/* _______________ RADIUS, MASS AND VOLUME _____________________ */

	/**
	 * Compute the volume on the basis of the mass and density of different
	 * compounds defined in the cell
	 */
	public void updateVolume() {
		_volume = 0;
		for (int i = 0; i<particleMass.length; i++) {
			_volume += particleMass[i]/getSpeciesParam().particleDensity[i];
		}
		_totalVolume = _volume;
	}

	/**
	 * Compute the radius on the basis of the volume The radius evolution is
	 * stored in deltaRadius (used for shrinking)
	 */
	public void updateRadius() {

		if (_radius == 0)
		{
			if(Simulator.isChemostat){
				_radius = ExtraMath.radiusOfASphere(_volume);
				_totalRadius = ExtraMath.radiusOfASphere(_totalVolume);
	
			}else{
	
				if (_species.domain.is3D) {
					_radius = ExtraMath.radiusOfASphere(_volume);
					_totalRadius = ExtraMath.radiusOfASphere(_totalVolume);
				} else {
					_radius = ExtraMath.radiusOfACylinder(_volume,
							_species.domain.length_Z);
					_totalRadius = ExtraMath.radiusOfACylinder(_totalVolume,
							_species.domain.length_Z);
				}
			}
		}
		else
		{
			double reaction = ((LocatedParam) _speciesParam).reactionKinetic[0][0] ;
			_radius *= 1 + ((1 - reaction) / 60) ;

		}
		
		//also update length 
		if (_radius > 0)
		{
			
			//find head and tail locations given the current radius
			//first get unit vector from current head and tail then multiply by radius
			
			
			double magnitude = _headLocation.distance(_location);
			if (magnitude == 0)
			{
				randomizeOrientation();
				 magnitude = _headLocation.distance(_location);
			}
			
			double magX = ((_headLocation.x - _location.x) / magnitude) * (_radius - _capsular_radius) ;
			double magY = ((_headLocation.y - _location.y) / magnitude) * (_radius - _capsular_radius) ;
			double magZ = ((_headLocation.z - _location.z) / magnitude) * (_radius - _capsular_radius) ;
			_headLocation.x = _location.x + magX;
			_headLocation.y = _location.y + magY;
			_headLocation.z = _location.z + magZ;
			_tailLocation.x = _location.x - magX; 
			_tailLocation.y = _location.y - magY;
			_tailLocation.z = _location.z - magZ;

			
		}
			
	}

	public AllBC updateAttachment() {
		// Search a boundary which will be crossed
		double distance;
		for (AllBC aBoundary : getDomain().getAllBoundaries()) {
			if (aBoundary.isSupport()) {
				distance = aBoundary.getDistance(this._location);
				_isAttached = distance<=(3*this._totalRadius);
				return aBoundary;
			}
		}
		return null;
	}

	public void addMovement(ContinuousVector aMove) {
		this._movement.add(aMove);
	}

	/* __________________ ACCESSORS ___________________________________ */
	public LocatedParam getSpeciesParam() {
		return (LocatedParam) _speciesParam;
	}

	public double getVolume(boolean withCapsule) {
		return (withCapsule ? _totalVolume : _volume);
	}

	public double getRadius(boolean withCapsule) {
		return (withCapsule ? _totalRadius : _radius);
	}

	public double getMass(boolean withCapsule) {
		return (withCapsule ? _totalMass : _totalMass);
	}

	public double getMaximumRadius() {
		return getSpeciesParam().divRadius
		* (1 + getSpeciesParam().divRadiusCV);
	}

	public boolean hasEPS() {
		return false;
	}

	public boolean hasInert() {
		return false;
	}

	public double getShoveFactor() {
		return ((LocatedParam) _speciesParam).shoveFactor;
	}

	public double getShoveRadius() {
		return _totalRadius*((LocatedParam) _speciesParam).shoveFactor;
	}

	public double getInteractDistance() {
		return 2*getShoveRadius()+((LocatedParam) _speciesParam).shoveLimit;
	}

	public double getInteractDistance(LocatedAgent baby) {
		return getShoveRadius() + baby.getShoveRadius()
		+ ((LocatedParam) _speciesParam).shoveLimit;
	}

	public double getBabyMassFrac() {
		return ExtraMath.deviateFrom(getSpeciesParam().babyMassFrac,
				getSpeciesParam().babyMassFracCV);
	}

	public boolean isMoving() {
		return (_movement.norm()>_totalRadius/10);
	}

	public boolean isAttached() {
		return _isAttached;
	}

	public double getActiveFrac() {
		return 1.0;
	}

	public Color getColor() {
		return _species.color;
	}

	public Color getColorCapsule() {
		return Color.green;
	}

	public ContinuousVector getLocation() {
		return _location;
	}
	
	public ContinuousVector getHeadLocation() {
		return _headLocation;
	}
	
	public ContinuousVector getTailLocation() {
		return _tailLocation;
	}

	/**
	 * Comparator used by AgentContainer.erodeBorder()
	 * @author Rob Clegg
	 */
	public static class detPriorityComparator implements java.util.Comparator<Object> {

		public int compare(Object b1, Object b2) {
			return (((LocatedAgent) b1).detPriority>((LocatedAgent) b2).detPriority ? 1 : -1);
		}
	}

	/**
	 * Comparator used by AgentContainer.erodeBorder()
	 * @author Rob Clegg
	 */
	public static class totalMassComparator implements java.util.Comparator<Object> {

		public int compare(Object b1, Object b2) {
			return (((LocatedAgent) b1)._totalMass>((LocatedAgent) b2)._totalMass ? 1 : -1);
		}
	}

	/**
	 * @param aLoc
	 * @return distance bw 2 agents assuming cyclic boundaries
	 */
	public double getDistance(LocatedAgent aLoc) {
		return computeDifferenceVector(_location, aLoc._location);
		/*EuclideanVector A = new EuclideanVector(_tailLocation, _headLocation);
		EuclideanVector B = new EuclideanVector();
		CollisionEngine.ClosestPtSegmentSegment(d1, d2)*/
	}

	public void setLocation(ContinuousVector cc) {

		//sonia:chemostat
		//set the location of the newborns to zero

		if(Simulator.isChemostat){
			cc.set(0,0,0);
			_location.x = cc.x;
			_location.y = cc.y;
			_location.z = cc.z;

		}else{
			
			_headLocation.x += (cc.x - _location.x);
			_headLocation.y += (cc.y - _location.y);
			_headLocation.z += (cc.z - _location.z);
			_tailLocation.x += (cc.x - _location.x);
			_tailLocation.y += (cc.y - _location.y);
			_tailLocation.z += (cc.z - _location.z);
			
			_location.x = cc.x;
			_location.y = cc.y;
			_location.z = cc.z;
		}

	}

	public ContinuousVector getMovement() {
		return _movement;
	}

	public int getGridIndex() {
		return _agentGridIndex;
	}

	public LocatedGroup getGridElement() {
		return _agentGrid.getShovingGrid()[_agentGridIndex];
	}

	public void setGridIndex(int aGridIndex) {
		_agentGridIndex = aGridIndex;
	}

	public Domain getDomain() {
		return _species.domain;
	}
	

	//return end point
	public static ContinuousVector RotateVector(double theta, 
			ContinuousVector[] v, ContinuousVector[] orientation)
    {
		//0 = start, 1 = end of vector
		//normalization of localized euclidean vector
		double norm = orientation[1].distance(orientation[0]);

		double vo_mag_x = (orientation[1].x - orientation[0].x) / norm;
        double vo_mag_y = (orientation[1].y - orientation[0].y) / norm;
        double vo_mag_z = (orientation[1].z - orientation[0].z) / norm;

        double mag_x = v[1].x-v[0].x;
        double mag_y = v[1].y-v[0].y;
        double mag_z = v[1].z-v[0].z;
        Quaternion Q1 = new Quaternion((double)0,mag_x,mag_y,mag_z);
        
        Quaternion Q2 = new Quaternion((float)Math.cos(theta / 2),
	            (float)(/*vo.x*/vo_mag_x * Math.sin(theta / 2)),
	            (float)(/*vo.y*/vo_mag_y * Math.sin(theta / 2)),
	            (float)(/*vo.z*/vo_mag_z * Math.sin(theta / 2)));
       Quaternion conjQ2 = Quaternion.Conjugate(Q2);

        Quaternion Q3;

        Q3 = Quaternion.Multiply(Quaternion.Multiply(Q2,Q1),conjQ2);

        ContinuousVector result = new ContinuousVector(v[0].x + Q3.x, v[0].y + Q3.y, v[0].z + Q3.z);
        return result;
    }

}