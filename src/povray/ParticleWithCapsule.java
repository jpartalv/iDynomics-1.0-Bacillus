
/**
 * Project iDynoMiCS (copyright -> see Idynomics.java)
 */

/**
 * @since Feb 2007
 * @version 1.0
 * @author João Xavier (xavierj@mskcc.org), Memorial Sloan-Kettering Cancer Center (NY, USA)
 * @author Laurent Lardon (lardonl@supagro.inra.fr), INRA, France
 */


package povray;

import java.awt.Color;
import java.io.Serializable;

import simulator.geometry.ContinuousVector;
import simulator.agent.LocatedAgent;



public class ParticleWithCapsule implements Serializable{
	// Serial version used for the serialisation of the class
	private static final long serialVersionUID = 1L;

	/**
	 * @uml.property  name="center"
	 * @uml.associationEnd  
	 */
	private VectorProperty center;
	/**
	 * @uml.property  name="head"
	 * @uml.associationEnd  
	 */
	private VectorProperty head;
	/**
	 * @uml.property  name="tail"
	 * @uml.associationEnd  
	 */
	private VectorProperty tail;
	private String capsular_radius = "";

	private double _radiusCore;

	private double _radiusCapsule;
	/**
	 * @uml.property  name="_colorCore"
	 * @uml.associationEnd  
	 */
	private VectorProperty _colorCore;

	/**
	 * @uml.property  name="_colorCapsule"
	 * @uml.associationEnd  
	 */
	private VectorProperty _colorCapsule;
	private String _nameCore;

	private String _nameCapsule;
	private boolean _hasCapsule;
	private double _activeFrac;

	/* _________________ CONSTRUCTOR _________________________ */
	public ParticleWithCapsule() {
		center = new VectorProperty("");
		head = new VectorProperty("");
		tail = new VectorProperty("");
		_colorCore = new VectorProperty("color rgb");
		capsular_radius = "0";
	}

	public ParticleWithCapsule(LocatedAgent p) {
		center = new VectorProperty("");
		head = new VectorProperty("");
		tail = new VectorProperty("");
		//capsular_radius = String.valueOf(p._capsular_radius);
		setCapsularRadius(p._capsular_radius);
		setCenter(p.getLocation());
		
		_colorCore = new VectorProperty("color rgb");
		setColorCore(p.getColor());
		setCoreRadius(p.getRadius(true));

		// bvm 27.1.2009 for using color definitions

		setNameCore(p.getName());
		setActiveFrac(p.getActiveFrac());

		setHead(p.getHeadLocation());
		setTail(p.getTailLocation());

		
		_hasCapsule = p.hasEPS();
		//TODO
		// NOTE: if this is set to true, need to modify the agent.Species routine
		// that creates color definitions so that the '-capsule' colors are defined
		_hasCapsule = false;
		if (_hasCapsule) {
			_radiusCapsule = p.getRadius(true)/Povray3DScene.getScaling();
			_colorCapsule = new VectorProperty("rgbf");
			setColorCapsule(p.getColorCapsule());
			// bvm 27.1.2009 for using color definitions
			setNameCapsule(p.getSpecies().speciesName+"-capsule");
		}
	}

	/**
	 * @param color
	 */
	public void setColorCore(Color c) {
		_colorCore.setValues(((float) c.getRed()) / 255,
				((float) c.getGreen()) / 255, ((float) c.getBlue()) / 255);
	}

	/**
	 * For now sets capsule to gray
	 * 
	 * @param fs
	 */
	public void setColorCapsule(Color c) {
		float r = ColorMaps.brightenValue(((float) c.getRed()) / 255, 0.5f);
		float g = ColorMaps.brightenValue(((float) c.getGreen()) / 255, 0.5f);
		float b = ColorMaps.brightenValue(((float) c.getBlue()) / 255, 0.5f);
		_colorCapsule.setValues(r, g, b, 0.999f);
	}

	/**
	 * @param theName
	 */
	public void setNameCore(String theName) {
		_nameCore = theName;
	}

	/**
	 * @param theName
	 */
	public void setNameCapsule(String theName) {
		_nameCapsule = theName;
	}

	/**
	 * @param activeFrac
	 */
	public void setActiveFrac(double activeFrac) {
		_activeFrac = activeFrac;
	}

	/**
	 * @param fs
	 */
	public void setCenter(ContinuousVector c) {
		double s = Povray3DScene.getScaling();
		center.setValues(c.x/s, c.y/s, c.z/s);
	}
	
	public void setHead(ContinuousVector c) {
		double s = Povray3DScene.getScaling();
		head.setValues(c.x/s, c.y/s, c.z/s);
		//head.setValues(center._values[0] , center._values[1] + this._radiusCore - Double.parseDouble(this.capsular_radius), center._values[2]); 
	}
	
	public void setTail(ContinuousVector c) {
		double s = Povray3DScene.getScaling();
		tail.setValues(c.x/s, c.y/s, c.z/s);
		//tail.setValues(center._values[0] , center._values[1] - this._radiusCore + Double.parseDouble(this.capsular_radius), center._values[2]); 
	}

	/**
	 * @param fs
	 */
	public void setCoreRadius(double fs) {
		_radiusCore = fs/Povray3DScene.getScaling();
	}
	
	public void setCapsularRadius(double fs) {
		capsular_radius = String.valueOf(fs/Povray3DScene.getScaling());
	}


	/*public String toString() {

		// bvm 27.1.2009: modified this output to use color definitions and
		// textures rather than pigments
		String core = "sphere {\n"
			+ "\t "	+ center + "\n"
			+ "\t "	+ _radiusCore + "\n"
			+ "\t pigment { " + _nameCore + "*" + _activeFrac + " }\n"
			+ "}\n";

		if (_hasCapsule) {
			String capsule = "sphere {\n"
				+ "\t " + center + "\n"
				+ "\t " + _radiusCapsule + "\n"
				+ "\t pigment { " + _nameCapsule + "*" + _activeFrac + " }\n"
				+ "}\n";
			return core + capsule;
		}

		return core;
	}*/
	
	
	public String toString() {

		// bvm 27.1.2009: modified this output to use color definitions and
		// textures rather than pigments
		
		String core= "sphere {" + tail + "\t " +  capsular_radius /*_radiusCore*/ +  "\t pigment { " + _nameCore /*+ "*" + "0.3"*//*_activeFrac*/ + " }"+ "}\n" +
		"sphere {" + head + "\t " +  capsular_radius /*_radiusCore*/ +  "\t pigment { " + _nameCore/* + "*" + "5"*/ /*_activeFrac*/ + " }"+ "}\n" +
		"cylinder {" + tail + "\t " + head + "\t " + capsular_radius /*_radiusCore*/ +  "\t pigment { " + _nameCore /*+ "*" + _activeFrac*/ + " }"+ "}\n";
		
		if (_hasCapsule) {
			String capsule = "sphere {\n"
				+ "\t " + center + "\n"
				+ "\t " + _radiusCapsule + "\n"
				+ "\t pigment { " + _nameCapsule + "*" + _activeFrac + " }\n"
				+ "}\n";
			return core + capsule;
		}

		return core;
	}
}
