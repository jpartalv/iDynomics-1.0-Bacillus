/**
 * Project iDynoMiCS (copyright -> see Idynomics.java)
 * ______________________________________________________
 * Create a 3D scene for the PovRay rendering engine
 */

/**
 * @since Feb 2007
 * @version 1.0
 * @author João Xavier (xavierj@mskcc.org), Memorial Sloan-Kettering Cancer Center (NY, USA)
 * @author Laurent Lardon (lardonl@supagro.inra.fr), INRA, France
 */

package povray;

import idyno.SimTimer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import simulator.Simulator;
import simulator.agent.Species;
import simulator.geometry.Domain;


public class Povray3DScene implements Serializable {

	// Serial version used for the serialisation of the class
	private static final long   serialVersionUID = 1L;

	/* ___________ _________________________ */
	/**
	 * @uml.property  name="mySim"
	 * @uml.associationEnd  
	 */
	public Simulator            mySim;
	final private static String INCHEADER        = "sceneheader.inc";
	final private static String INCFOOTER        = "scenefooter.inc";

	/**
	 * @uml.property  name="_camera"
	 * @uml.associationEnd  
	 */
	private Camera              _camera;
	/**
	 * @uml.property  name="_background"
	 * @uml.associationEnd  
	 */
	private Background          _background;
	/**
	 * @uml.property  name="_lightSource"
	 * @uml.associationEnd  multiplicity="(0 -1)"
	 */
	private LightSource[]       _lightSource;
	/**
	 * @uml.property  name="_biofilm"
	 * @uml.associationEnd  
	 */
	private Biofilm3D           _biofilm;
	/**
	 * @uml.property  name="_domain"
	 * @uml.associationEnd  
	 */
	private Domain              _domain;

	private double              _x;

	private double _y;

	private double _z;
	private static double       _scaling;
	
	//private int iteration;

	/* _________________ CONSTRUCTOR _________________________ */
	public Povray3DScene(Simulator aSim,String domainName) {
		mySim = aSim;
		_domain = aSim.world.getDomain(domainName);
		_scaling = _domain.getLongestSize();
		_x = _domain.length_X/_scaling;
		_y = _domain.length_Y/_scaling;
		_z = _domain.length_Z/_scaling;

		//iteration = SimTimer.getCurrentIter();
		
		initializeScene();
	}

	/**
	 * Initialise scene parameters (camera, background and light)
	 */
	private void initializeScene() {
		// Set the camera
		_camera = new Camera();
		_lightSource = new LightSource[2];

		// location and orientation (where is and where it looks)
		// NOTE THAT POVRAY USES A LEFT-HAND COORDINATE SYSTEM
		// (x is left/right, y is up/down, and z is into screen;
		//  this means compared to the idyno representation, povray swaps x and y)

		if (_domain.is3D){
			_camera.setLocation(0, _y*1.5, -_x*1.5);

			_lightSource[0] = new LightSource();
			_lightSource[0].setLocation(_y, _x, -_z);
			_lightSource[0].setColor(1f, 1f, 1f);

			_lightSource[1] = new LightSource();
			_lightSource[1].setLocation(-_y, _x, -_z);	
			_lightSource[1].setColor(1f, 1f, 1f);		
		}else{
			_camera.setLocation(0, 0, -_x*1.5);

			_lightSource[0] = new LightSource();
			_lightSource[0].setLocation(_z, 0, -_x*1.5);
			_lightSource[0].setColor(1f, 1f, 1f);

			_lightSource[1] = new LightSource();
			_lightSource[1].setLocation(-_z, 0, -_x*1.5);
			_lightSource[1].setColor(1f, 1f, 1f);
		}

		_camera.setLook_at(0, 0, 0);

		// these set the aspect ratio of the view
		_camera.setUp(0, 1, 0);
		_camera.setRight(-1.33f, 0, 0);
		_camera.setAngle(60);

		// Set the background
		_background = new Background();
		_background.setColor(0.5f, 0.5f, 0.5f);

	}


	/**
	 * Write the complete model state to a file (including camera, lights and
	 * boxes)
	 * 
	 * @param fileName
	 * @throws IOException
	 */
	public void writeModelStateFull(String fileName) throws IOException {

		File sysData = new File(fileName);
		FileWriter fr = new FileWriter(sysData);

		fr.write(_camera.toString());
        
		//String infoText = this.getSimulationInfo();
		//fr.write(infoText);
				
		fr.write(_background.toString());
		fr.write(_lightSource[0].toString());
		fr.write(_lightSource[1].toString());

		String _camera_location = ""; 
		if (_domain.is3D)
			_camera_location = "<0, " +_y*1.5+ ", " + -_x*1.5 + ">;";
		else
			_camera_location = "<0, 0, " + -_x*1.5 + ">;";
		
		_biofilm = new Biofilm3D(this);
		//_biofilm.modelStateToFile(fr);
		_biofilm.modelStateToFile(fr,_camera_location);

		fr.close();
	}
	
	private String getSimulationInfo()
	{
		String _camera_location = ""; 
		if (_domain.is3D)
			_camera_location = "<0, " +_y*1.5+ ", " + -_x*1.5 + ">;";
		else
			_camera_location = "<0, 0, " + -_x*1.5 + ">;";
			
		
		return "#declare camera_location = " + _camera_location + "\n" +
		"#declare camera_look_at = < 0.0,  0.0,  0.0 >;\n" +
		"#declare camera_sky = y;\n" +
		"#declare Z = vnormalize(camera_look_at - camera_location);\n" +
		"#declare X = vnormalize(vcross(vnormalize(camera_sky), Z));\n" +
		"#declare Y = vnormalize(vcross(Z, X));\n" +
		"text {ttf \"Arial\", \"Iteration :" + String.valueOf(SimTimer.getCurrentIter()) +
		"\n Time: " + String.valueOf(SimTimer.getCurrentTime()) + " hrs. \", 0.01, 0\n" +
		"    pigment {rgb 1} finish {ambient 1 diffuse 0} no_shadow\n" +
		"    scale 0.05 translate <.2, -.4, 1> // Adjust position and size\n" +
		"    matrix <-X.x, -X.y, -X.z, Y.x, Y.y, Y.z, Z.x, Z.y, Z.z, 0, 0, 0>\n" +
		"    scale 1/100 translate camera_location}\n";
		
	}

	/**
	 * Write the include files. For use with writeModelState
	 * 
	 * @param dir directory to write the include file
	 * @throws IOException
	 */
	public File[] writePovrayIncFiles(String dir) throws IOException {
		_biofilm = new Biofilm3D(this);

		// header include file
		java.io.File header = new java.io.File(dir+INCHEADER);
		FileWriter fr = new FileWriter(header);
		fr.write(_camera.toString());
		
		//String infoText = this.getSimulationInfo();
		//fr.write(infoText);

		
		fr.write(_background.toString());
		fr.write(_lightSource[0].toString());
		fr.write(_lightSource[1].toString());
		_biofilm.biofilmHeaderToFile(fr);

		// bvm 27.1.2009: define colors based on species name; for use in macros
		// (uses the progenitor, which is able to write colors for different individual states)
		for (Species aSpecies : mySim.speciesList)
			aSpecies.getProgenitor().writePOVColorDefinition(fr);

		fr.close();

		// footer include file
		java.io.File footer = new java.io.File(dir+INCFOOTER);
		fr = new FileWriter(footer);
		_biofilm.biofilmFooterToFile(fr);
		fr.close();

		// prepare file array to return
		File[] incs = { header, footer };
		return incs;
	}

	/**
	 * Write the present state using include files for camera, background lights
	 * and solid surface. Using this method instead of writeModelStateFull,
	 * which includes the full scene information in each .pov file created at an
	 * iteration, allows changing scene properties after the simulation
	 * 
	 * @param fileName
	 * @return the file written
	 * @throws IOException
	 */
	public File writeModelState(String fileName) throws IOException {
		_biofilm = new Biofilm3D(this);

		File sysData = new File(fileName);
		FileWriter fr = new FileWriter(sysData);


		fr.write("#include \""+"colors.inc"+"\"\n");

		fr.write("#include \""+INCHEADER+"\"\n");

		try {
			_biofilm.particlesToFile(fr);
		} catch(Exception e) {
			_biofilm.particlesToFile(fr);
		}

		fr.write("#include \""+INCFOOTER+"\"\n");
		fr.close();
		
		//Runtime.getRuntime().exec("C:/Users/Rosa/AppData/Roaming/POV-Ray/v3.6/bin/pvengine.exe /render " + "\"" +fileName + "\"");
		
		return sysData;
	}

	/* ___________ _________________________ */
	/**
	 * Make top perspective
	 */
	public void setTopPerspective() {
		_camera.setLocation(0, _y*1.56f, 0);
		_lightSource[1].setLocation(_z, 0, 0);
	}

	/**
	 * Make side perspective
	 */
	public void setSidePerspective() {
		_camera.setLocation(0, _y, _x*1.17f);
		_lightSource[1].setLocation(_z, 0, 0);
		_camera.setLook_at(0, -_y*0.5, 0);
	}

	/**
	 * Make angle perspective
	 */
	public void setAnglePerspective() {
		_camera.setLocation(_z, _y, _x*2.5f);
		_lightSource[1].setLocation(_z, 0, 0);
		_camera.setLook_at(0, 0, 0);
	}

	/**
	 * Add a cell to the biofilm, to be used with scenes constructed using the
	 * Povray3DScene(float x, float y, float z, int n) constructor
	 * 
	 * @param x coordinate
	 * @param y coordinate
	 * @param z coordinate
	 * @param rad radius
	 * @param r red
	 * @param g green
	 * @param b blue
	 */
	public void addCell(float x, float y, float z, float rad, int r, int g, int b) {
		_biofilm.addCell(x, y, z, rad, r, g, b);
	}

	/**
	 * @return
	 */
	protected double getX() {
		return _x;
	}

	/**
	 * @return
	 */
	protected double getY() {
		return _y;
	}

	/**
	 * @return
	 */
	protected double getZ() {
		return _z;
	}

	Domain getDomain() {
		return _domain;
	}

	/**
	 * @return Returns the _scaling.
	 */
	public static double getScaling() {
		return _scaling;
	}

	/**
	 * @param os
	 * @throws IOException
	 */
	public static void serializeStaticState(ObjectOutputStream os) throws IOException {
		os.writeDouble(_scaling);
	}

	/**
	 * @param os
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void deserializeStaticState(ObjectInputStream os) throws IOException,
	ClassNotFoundException {
		_scaling = os.readFloat();
	}

}
