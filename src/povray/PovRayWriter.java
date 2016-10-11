/**
 * Project iDynoMiCS (copyright -> see Idynomics.java)
 * ______________________________________________________
 * Writer of povray files (rendering engines of 3D scenes)
 */

/**
 * @since Feb 2007
 * @version 1.0
 * @author João Xavier (xavierj@mskcc.org), Memorial Sloan-Kettering Cancer Center (NY, USA)
 * @author Laurent Lardon (lardonl@supagro.inra.fr), INRA, France
 */

package povray;

import de.schlichtherle.io.File;

// import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import utils.LogFile;

import simulator.Simulator;


public class PovRayWriter implements Serializable {

	// Serial version used for the serialisation of the class
	private static final long serialVersionUID = 1L;

	/**
	 * @uml.property  name="_povRay"
	 * @uml.associationEnd  
	 */
	private Povray3DScene     _povRay;
	private String            dir;
	//private int               fileIndex        = 0;
	private File              _vArchive;

	/*
	 * (non-Javadoc)
	 */
	public void initPovRay(Simulator aSim, String outPath) {
		try {
			dir = outPath+File.separator;
			// Create the PovRay description object
			_povRay = new Povray3DScene(aSim, aSim.world.domainList.get(0).getName());
			_povRay.writePovrayIncFiles(dir+"lastIter"+File.separator);			

			//make dir and add files
			new File(dir+"povray").mkdir();
			
			// Create the archive file
			_vArchive = new File(dir+"povray"+".zip");
			File incFile=new File(dir+"lastIter"+File.separator+"sceneheader.inc");
			incFile.copyTo(new File(dir+"povray.zip"+File.separator+"sceneheader.inc"));
			incFile.copyTo(new File(dir+"povray/sceneheader.inc"));

			incFile=new File(dir+"lastIter"+File.separator+"scenefooter.inc");
			incFile.copyTo(new File(dir+"povray.zip"+File.separator+"scenefooter.inc"));
			incFile.copyTo(new File(dir+"povray/scenefooter.inc"));

			File.update(_vArchive);
			
			
			

		} catch (Exception e) {
			LogFile.writeError(e.getLocalizedMessage(), "PovRayWriter.initPovRay()");
		}
	}

	// bvm 27.1.2009 added passing in of current run iteration
	public void write(int fileIndex) {
		try {
			// Create the povray file
			File f = new File(_povRay.writeModelState(dir+"lastIter"+File.separator+"it(last).pov"));

			// Copy the povray file inside the archive
			f.copyTo(new File(dir+"povray.zip"+File.separator+"it("+fileIndex+").pov"));
			File povFile = new File(dir+"povray/it("+fileIndex+").pov");
			povFile.setReadable(true);
			f.copyTo(povFile);
			
			
			File.update(_vArchive);

			
			//Runtime.getRuntime().exec("C:/Users/Rosa/AppData/Roaming/POV-Ray/v3.6/bin/pvengine.exe /render " + "\""+dir+"povray/it("+fileIndex+").pov\"");

			// Increase the iteration number
			//fileIndex++;
		} catch (IOException e) {
			System.out.println("Error trying to write povRayFile");
		}
	}
	
	

}
