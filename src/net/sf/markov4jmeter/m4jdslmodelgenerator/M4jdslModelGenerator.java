package net.sf.markov4jmeter.m4jdslmodelgenerator;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import m4jdsl.ApplicationModel;
import m4jdsl.ApplicationState;
import m4jdsl.ApplicationTransition;
import m4jdsl.BehaviorMix;
import m4jdsl.BehaviorModel;
import m4jdsl.M4jdslFactory;
import m4jdsl.MarkovState;
import m4jdsl.Service;
import m4jdsl.SessionLayerEFSM;
import m4jdsl.WorkloadIntensity;
import m4jdsl.WorkloadModel;
import m4jdsl.impl.M4jdslPackageImpl;
import net.sf.markov4jmeter.behaviormodelextractor.BehaviorModelExtractor;
import net.sf.markov4jmeter.behaviormodelextractor.extraction.ExtractionException;
import net.sf.markov4jmeter.behaviormodelextractor.extraction.parser.ParseException;
import net.sf.markov4jmeter.behaviormodelextractor.extraction.parser.Parser;
import net.sf.markov4jmeter.behaviormodelextractor.extraction.parser.SessionData;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.ApplicationModelGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.BehaviorMixGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.BehaviorModelsGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.WorkloadIntensityGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.AbstractProtocolLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.AbstractSessionLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.FlowSessionLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.GuardsAndActionsGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.HTTPProtocolLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.XmiEcoreHandler;

import org.eclipse.xtext.xtext.ecoreInference.TransformationException;


/**
 * This is the main class of the M4J-DSL Model Generator, which builds models
 * that comply to the M4J-DSL. Each model represents a Markov4JMeter workload
 * model, including workload intensity, Application Layer, Behavior Models and
 * Behavior Mix.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class M4jdslModelGenerator {


    /* *****************************  constants  **************************** */


    /** Suffix of Flow-DSL files. */
    private final static String FLOW_FILE_SUFFIX = ".flows";

    /** Property key for Behavior Model definitions. */
    private final static String PKEY_BEHAVIOR_MODELS = "behaviorModels";

    /** Usage information for this application. */
    private final static String USAGE = "Usage: %s "  // %s = application name;
            + "<workloadIntensity.properties> "
            + "<behaviorModels.properties> "
            + "<flowsDirPath> "
            + "<xmiOutputFile> "
            + "<graphOutputFile>";


    /* --------------------------  error messages  -------------------------- */


    /** Error message for the case that a required <code>File</code> instance
     *  denoting a directory is <code>null</code>. */
    private final static String ERROR_DIRECTORY_FILE_IS_NULL =
            "directory file is null";

    /** Error message for the case that a passed <code>File</code> instance,
     *  which is expected to denote a directory, is a regular file. */
    private final static String ERROR_DIRECTORY_FILE_DENOTES_NO_DIRECTORY =
            "file \"%s\" does not denote a directory";

    /** Error message for the case that a parameter sequence for a Behavior
     *  Model has too few parameters. */
    private final static String ERROR_PARAMETER_SEQUENCE_HAS_TOO_FEW_PARAMS =
            "Behavior Model parameter sequence \"%s\" has too few parameters";

    /** Error message for the case that no Behavior Model parameters are
     *  defined in a properties file. */
    private final static String ERROR_PARAMETERS_UNDEFINED =
            "Behavior Model parameters are undefined";

    /** Error message for the case that a Behavior Model frequency is invalid.*/
    private final static String ERROR_INVALID_FREQUENCY =
            "invalid frequency value \"%s\" for Behavior Model \"%s\"";


    /* *************************  global variables  ************************* */


    /** Instance for creating M4J-DSL model elements. */
    private final M4jdslFactory m4jdslFactory;
    

   

    /* ***************************  constructors  *************************** */


    /**
     * Constructor for an M4jdslModelGenerator.
     */
    public M4jdslModelGenerator () {

        M4jdslPackageImpl.init();
        this.m4jdslFactory = M4jdslFactory.eINSTANCE;
    }


    /* **************************  public methods  ************************** */


    /**
     * Creates an M4J-DSL model which builds on Flow information; additionally,
     * a DOT graph which illustrates the states and transitions of the Session
     * Layer EFSM will be generated.
     *
     * @param workloadIntensityProperties
     *     properties file which includes the workload intensity definition.
     * @param behaviorModelsProperties
     *     properties file which includes the Behavior Models definitions.
     * @param flowsDirectoryPath
     *     path to the directory which contains the Flow files.
     * @param graphOutputPath
     *     path to the graph output file.
     * @param sessionsCanBeExitedAnytime
     *     <code>true</code> if and only if sessions can be exited at any time,
     *     which is generally given in Web applications; if this flag is set
     *     <code>true</code>, transitions to the exit state will be installed
     *     for all states of the Application Layer.
     * @param useFullyQualifiedNames
     *     <code>true</code> if and only if fully qualified state names shall
     *     be used; if this flag is <code>false</code>, plain Node names will be
     *     used as state names, without any related Flow names being added as
     *     prefixes.
     *
     * @return
     *     the newly created M4J-DSL model.
     *
     * @throws GeneratorException
     *     if any error during the generation process occurs, e.g., if any
     *     property could not be read or information is invalid/insufficient.
     */
    public WorkloadModel generateWorkloadModel (
            final Properties workloadIntensityProperties,
            final Properties behaviorModelsProperties,
            final String flowsDirectoryPath,
            final String graphOutputPath,
            final String sessionDatFile,
            final boolean sessionsCanBeExitedAnytime,
            final boolean useFullyQualifiedNames) throws GeneratorException {

        // to be returned;
        final WorkloadModel workloadModel =
                this.m4jdslFactory.createWorkloadModel();

        final ServiceRepository serviceRepository =
                new ServiceRepository(this.m4jdslFactory);

        final HashMap<String, Double> behaviorMixEntries =
                new HashMap<String, Double>();

        // might throw a GeneratorException;
        final LinkedList<BehaviorModelParameters> behaviorModelParametersList =
                readBehaviorModelParametersList(behaviorModelsProperties);

        final ArrayList<String> names         = new ArrayList<String>();
        final ArrayList<String> filenames     = new ArrayList<String>();
        final ArrayList<Double> frequencies   = new ArrayList<Double>();
        final ArrayList<File>   behaviorFiles = new ArrayList<File>();

        for (BehaviorModelParameters p : behaviorModelParametersList) {
            names.add(p.name);
            filenames.add(p.filename);
            frequencies.add(p.frequency);
            behaviorFiles.add(p.behaviorFile);
        }

        final Iterator<Double> frequencyIterator = frequencies.iterator();

        for (final String name : names) {
            behaviorMixEntries.put(name, frequencyIterator.next());
        }

        // set the individual components of the workload model;

        // might throw a GeneratorException;
        this.installWorkloadIntensity(
                workloadModel,
                workloadIntensityProperties);
        
        // might throw a GeneratorException;
        this.installApplicationLayer(
                workloadModel,
                serviceRepository,
                flowsDirectoryPath,
                graphOutputPath,
                sessionDatFile,
                sessionsCanBeExitedAnytime,
                useFullyQualifiedNames);
        
        // might throw a GeneratorException;
        this.installBehaviorModels(
                workloadModel,
                serviceRepository,
                names.toArray(new String[]{}),
                filenames.toArray(new String[]{}),
                behaviorFiles.toArray(new File[]{}));
        
        // might throw a GeneratorException;
        this.installBehaviorMix(
                workloadModel,
                workloadModel.getBehaviorModels(),
                behaviorMixEntries);         
 
        this.removeUnusedApplicationTransitions(workloadModel);
               
        this.installGuardsAndActions(workloadModel);

        return workloadModel;
    }


    /* **************************  private methods  ************************* */


    /* ---------  installation methods for M4J-DSL model components  -------- */


    /**
     * Installs the workload intensity in a given M4J-DSL model.
     *
     * @param workloadModel
     *     M4J-DSL model in which the workload intensity shall be installed.
     * @param workloadIntensityProperties
     *     properties file which includes the workload intensity definition.
     *
     * @return
     *     M4J-DSL model with the installed workload intensity.
     *
     * @throws GeneratorException
     *     if the workload intensity installation fails for any reason.
     */
    private WorkloadModel installWorkloadIntensity (
            final WorkloadModel workloadModel,
            final Properties workloadIntensityProperties) throws GeneratorException {

        final WorkloadIntensityGenerator workloadIntensityGenerator =
                new WorkloadIntensityGenerator(this.m4jdslFactory);

        // might throw a GeneratorException;
        final WorkloadIntensity workloadIntensity =
                workloadIntensityGenerator.generateWorkloadIntensity(
                        workloadIntensityProperties);

        workloadModel.setWorkloadIntensity(workloadIntensity);
        return workloadModel;
    }

    /**
     * Installs the Application Layer in a given M4J-DSL model.
     *
     * @param workloadModel
     *     M4J-DSL model in which the Application Layer shall be installed.
     * @param serviceRepository
     *     instance for handling all available services.
     * @param flowsDirectoryPath
     *     path to the directory which contains the Flow files.
     * @param graphOutputPath
     *     path to the graph output file.
     * @param sessionsCanBeExitedAnytime
     *     <code>true</code> if and only if sessions can be exited at any time,
     *     which is generally given in Web applications; if this flag is set
     *     <code>true</code>, transitions to the exit state will be installed
     *     for all states of the Application Layer.
     * @param useFullyQualifiedNames
     *     <code>true</code> if and only if fully qualified state names shall
     *     be used; if this flag is <code>false</code>, plain Node names will be
     *     used as state names, without any related Flow names being added as
     *     prefixes.
     *
     * @return
     *     M4J-DSL model with the installed Application Layer.
     *
     * @throws GeneratorException
     *     if the Application Layer installation fails for any reason.
     * @throws ExtractionException 
     * @throws ParseException 
     * @throws IOException 
     */
    private WorkloadModel installApplicationLayer (
            final WorkloadModel workloadModel,
            final ServiceRepository serviceRepository,
            final String flowsDirectoryPath,
            final String graphOutputPath,
            final String sessionDatFile,
            final boolean sessionsCanBeExitedAnytime,
            final boolean useFullyQualifiedNames) throws GeneratorException {
/*
        final AbstractProtocolLayerEFSMGenerator protocolLayerEFSMGenerator =
                new JavaProtocolLayerEFSMGenerator(
                        this.m4jdslFactory,
                        new IdGenerator("PS"),
                        new IdGenerator("R"));
*/   	
   	
        try {
        	
        	ArrayList<SessionData> sessions =  BehaviorModelExtractor.
			        parseSessionsIntoSessionsRepository(sessionDatFile);
        		
	        final AbstractProtocolLayerEFSMGenerator protocolLayerEFSMGenerator =
	                new HTTPProtocolLayerEFSMGenerator(
	                        this.m4jdslFactory,
	                        new IdGenerator("PS"),
	                        new IdGenerator("R"),
	                        sessions);
	
	        // might throw a GeneratorException;
	        final File[] flowFiles = this.readFilesFromDirectory(
	                flowsDirectoryPath,
	                M4jdslModelGenerator.FLOW_FILE_SUFFIX);
	
	        final AbstractSessionLayerEFSMGenerator sessionLayerEFSMGenerator =
	                new FlowSessionLayerEFSMGenerator(
	                        this.m4jdslFactory,
	                        serviceRepository,
	                        protocolLayerEFSMGenerator,
	                        new IdGenerator("ASId"),
	                        sessionsCanBeExitedAnytime,
	                        useFullyQualifiedNames,
	                        flowFiles,
	                        graphOutputPath);
	
	        final ApplicationModelGenerator applicationModelGenerator =
	                new ApplicationModelGenerator(this.m4jdslFactory, sessionLayerEFSMGenerator);
	
	        // might throw a GeneratorException;
	        final ApplicationModel applicationModel =
	                applicationModelGenerator.generateApplicationModel();
	
	        workloadModel.setApplicationModel(applicationModel);
        
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExtractionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return workloadModel;
    }
      
    /**
     * Installs the Behavior Models in a given M4J-DSL model.
     *
     * @param workloadModel
     *     M4J-DSL model in which the Behavior Models shall be installed.
     * @param serviceRepository
     *     instance for handling all available services.
     * @param names
     *     names of the Behavior Models.
     * @param filenames
     *     filenames of the Behavior Models.
     * @param behaviorFiles
     *     files which provide the behavior information (probabilities and
     *     think times) to be included to the Behavior Models.
     *
     * @return
     *     M4J-DSL model with the installed Behavior Models.
     *
     * @throws GeneratorException
     *     if the Behavior Models installation fails for any reason.
     */
    private WorkloadModel installBehaviorModels (
            final WorkloadModel workloadModel,
            final ServiceRepository serviceRepository,
            final String[] names,
            final String[] filenames,
            final File[] behaviorFiles) throws GeneratorException {

        final BehaviorModelsGenerator behaviorModelGenerator =
                new BehaviorModelsGenerator(
                        this.m4jdslFactory,
                        new IdGenerator("MSId"),
                        serviceRepository);

        // might throw a GeneratorException;
        final List<BehaviorModel> behaviorModels =
                behaviorModelGenerator.generateBehaviorModels(
                        names,
                        filenames,
                        behaviorFiles,
                        workloadModel.getApplicationModel().
                        getSessionLayerEFSM().getInitialState().getService());

        for (final BehaviorModel behaviorModel : behaviorModels) {
            workloadModel.getBehaviorModels().add(behaviorModel);
        }

        return workloadModel;
    }
    

    /**
     * Installs the Behavior Mix in a given M4J-DSL model.
     *
     * @param workloadModel
     *     M4J-DSL model in which the Behavior Mix shall be installed.
     * @param behaviorModels
     *     Behavior Models to be referred by the Behavior Mix.
     * @param behaviorMixEntries
     *     Behavior Mix entry data, including the relative frequencies to be
     *     registered.
     *
     * @return
     *     M4J-DSL model with the installed Behavior Mix.
     *
     * @throws GeneratorException
     *     if the Behavior Mix installation fails for any reason.
     */
    private WorkloadModel installBehaviorMix (
            final WorkloadModel workloadModel,
            final List<BehaviorModel> behaviorModels,
            final HashMap<String, Double> behaviorMixEntries)
                    throws GeneratorException {

        final BehaviorMixGenerator behaviorMixGenerator =
                new BehaviorMixGenerator(this.m4jdslFactory);

        // might throw a GeneratorException;
        final BehaviorMix behaviorMix =
                behaviorMixGenerator.generateBehaviorMix(
                        behaviorMixEntries,
                        behaviorModels);

        workloadModel.setBehaviorMix(behaviorMix);
        return workloadModel;
    }

    
    /**
     * Identify guards and actions.
     * 
     * @param workloadModel
     */
    private void installGuardsAndActions (final WorkloadModel workloadModel) {
    	GuardsAndActionsGenerator guardsAndActionsGenerator = new GuardsAndActionsGenerator(this.m4jdslFactory);
        guardsAndActionsGenerator.installGuardsAndActions(workloadModel);
    }    

    /* --------------------------  helping methods  ------------------------- */


    /**
     * Prints the usage information on the standard output stream.
     */
    private static void printUsage () {

        final String message = String.format(
                M4jdslModelGenerator.USAGE,
                M4jdslModelGenerator.class.getSimpleName() );

        System.out.println(message);
    }

    /**
     * Collects all files with a specified suffix from a given directory.
     *
     * @param directoryPath
     *     directory which contains the files to be collected.
     * @param suffix
     *     suffix of the files to be collected.
     *
     * @return
     *     the files whose suffix matches the specified one.
     *
     * @throws IOException
     *     if any I/O error occurs.
     * @throws GeneratorException
     *     if <code>null</code> has been passed as a directory path, or if the
     *     specified path does not denote a directory.
     */
    private File[] readFilesFromDirectory (
            final String directoryPath,
            final String suffix) throws GeneratorException {

        try {

            // might throw a NullPointerException;
            final File directory = new File(directoryPath);

            // might throw an IllegalArgument- or IOException;
            final File[] files = this.readFilesFromDirectory(
                    directory,
                    suffix);

            return files;

        } catch (final Exception ex) {

            throw new GeneratorException( ex.getMessage() );
        }
    }

    /**
     * Collects all files with a specified suffix from a given directory.
     *
     * @param directory
     *     directory which contains the files to be collected.
     * @param suffix
     *     suffix of the files to be collected.
     *
     * @return
     *     the files whose suffix matches the specified one.
     *
     * @throws IOException
     *     if any I/O error occurs.
     * @throws IllegalArgumentException
     *     if <code>null</code> has been passed as a <code>File</code> instance,
     *     or if the given file does not denote a directory.
     */
    private File[] readFilesFromDirectory (
            final File directory,
            final String suffix) throws IllegalArgumentException, IOException {

        if (directory == null) {

            throw new IllegalArgumentException(
                    M4jdslModelGenerator.ERROR_DIRECTORY_FILE_IS_NULL);
        }

        if ( !directory.isDirectory() ) {

            final String message = String.format(
                    M4jdslModelGenerator.
                    ERROR_DIRECTORY_FILE_DENOTES_NO_DIRECTORY,
                    directory.getAbsolutePath());

            throw new IllegalArgumentException(message);
        }

        return directory.listFiles(new FileFilter() {

            @Override
            public boolean accept (final File file) {

                return !file.isDirectory() && file.getName().endsWith(suffix);
            }
        });
    }

    /**
     * Reads the Behavior Models parameters from a given set of properties.
     *
     * @param properties
     *     properties which contain the Behavior Models parameters to be read.
     *
     * @return
     *     the list of Behavior Models parameters, ordered as they are found
     *     in the given properties set.
     *
     * @throws GeneratorException
     *     if no Behavior Models parameters are provided by the properties set,
     *     or if any parameter sequence has insufficient information, or if
     *     any parsing error occurs.
     */
    private LinkedList<BehaviorModelParameters>
    readBehaviorModelParametersList (final Properties properties)
            throws GeneratorException {

        final LinkedList<BehaviorModelParameters> behaviorModelParametersList =
                new LinkedList<BehaviorModelParameters>();

        final String behaviorModelsParameters = properties.getProperty(
                M4jdslModelGenerator.PKEY_BEHAVIOR_MODELS);

        if (behaviorModelsParameters == null) {

            throw new GeneratorException(
                    M4jdslModelGenerator.ERROR_PARAMETERS_UNDEFINED);
        }

        final String[] parameterSequences =
                behaviorModelsParameters.split("\\s*,\\s*");

        for (final String parameterSequence : parameterSequences) {

            final String[] parameters = parameterSequence.split("\\s*;\\s*");

            if (parameters.length < 3) {

                final String message = String.format(
                        M4jdslModelGenerator.
                        ERROR_PARAMETER_SEQUENCE_HAS_TOO_FEW_PARAMS,
                        parameterSequence);

                throw new GeneratorException(message);
            }

            // might throw a GeneratorException;
            final BehaviorModelParameters behaviorModelParameters =
                    this.extractBehaviorModelParameters(parameters);

            behaviorModelParametersList.add(behaviorModelParameters);
        }

        return behaviorModelParametersList;
    }

    /**
     * Parses a set of Behavior Model parameters from a given sequence of
     * <code>String</code>s.
     *
     * @param parameters
     *     sequence of <code>String</code>s to be parsed; the sequence must
     *     provide the information for a Behavior Model in the following order:
     *     <i>name</i>, <i>filename</i>, <i>frequency</i>,
     *      <i>behaviorFilePath</i>.
     *
     * @return
     *     the extracted parameter values.
     *
     * @throws GeneratorException
     *     if any parsing error occurs.
     */
    private BehaviorModelParameters extractBehaviorModelParameters (
            final String[] parameters) throws GeneratorException {

        final String name         = parameters[0];
        final String filename     = parameters[1];
        final String frequencyStr = parameters[2];

        final String behaviorFilePath =
                (parameters.length >= 4) ? parameters[3] : null;

        final double frequency;

        try {

            // might throw a NullPointer- or NumberFormatException;
            frequency = Double.parseDouble(frequencyStr);

        } catch (final Exception ex) {

            final String message = String.format(
                    M4jdslModelGenerator.ERROR_INVALID_FREQUENCY,
                    frequencyStr,
                    name);

            throw new GeneratorException(message);
        }

        // might throw NullPointerException (should never happen here);
        final File behaviorFile =
                (behaviorFilePath != null) ? new File(behaviorFilePath) : null;

        return new BehaviorModelParameters(
                name,
                filename,
                frequency,
                behaviorFile);
    }
        
    /**
     * As not all transitions are allowed in the behaviorModels and the application model is created before the behavior models,
     * we have to remove application transitions which are not possible. 
     * 
     * @param workloadModel
     */
    private void removeUnusedApplicationTransitions(final WorkloadModel workloadModel) {
    	SessionLayerEFSM sessionLayerEFSM = workloadModel.getApplicationModel().getSessionLayerEFSM();
    	List<ApplicationTransition> removeList = new ArrayList<ApplicationTransition>();
    	for (ApplicationState applicationState : sessionLayerEFSM.getApplicationStates()) {
    		for (ApplicationTransition applicationTransition : applicationState.getOutgoingTransitions()) {
    			Service fromApplicationStateService = applicationState.getService();
    			Service targetApplicationStateService = null;
    			if (applicationTransition.getTargetState() instanceof ApplicationState) {
    				targetApplicationStateService =  ((ApplicationState) applicationTransition.getTargetState()).getService();
    				if (!applicationTransitionInBehaviorModel(fromApplicationStateService, targetApplicationStateService, workloadModel)) {
        				removeList.add(applicationTransition);
        			}  
    			}    			  			
    		}
    		applicationState.getOutgoingTransitions().removeAll(removeList);
    		removeList.clear();
    	}    	
    }
    
    /**
     * Checks if an applicationTransition is in one of the behaviorModels.
     * 
     * @param fromApplicationStateService
     * @param targetApplicationStateService
     * @param workloadModel
     * @return boolean
     */
    private boolean applicationTransitionInBehaviorModel(final Service fromApplicationStateService, 
    		final Service targetApplicationStateService, 
    		final WorkloadModel workloadModel) {
    	boolean found = false;
		for (BehaviorModel behaviorModel : workloadModel.getBehaviorModels()) {
			for (MarkovState markovState : behaviorModel.getMarkovStates()) {
				for (m4jdsl.Transition transition : markovState.getOutgoingTransitions()) {
					Service fromMarkovStateService = markovState.getService();
					Service targetMarkovStateService = ((MarkovState) transition.getTargetState()).getService();
	    			if (fromApplicationStateService.equals(fromMarkovStateService) &&
	    					targetApplicationStateService.equals(targetMarkovStateService)) {
	    				found = true;
	    				break;
	    			}    		    		
				}
			}
		}
    	return found;
    }
    
    
    /* *************************  internal classes  ************************* */


    /**
     * POJO class for storing Behavior Model information.
     *
     * @author   Eike Schulz (esc@informatik.uni-kiel.de)
     * @version  1.0
     */
    private class BehaviorModelParameters {

        final String name;
        final String filename;
        final double frequency;
        final File   behaviorFile;

        public BehaviorModelParameters (
                final String name,
                final String filename,
                final double frequency,
                final File behaviorFile) {

            this.name         = name;
            this.filename     = filename;
            this.frequency    = frequency;
            this.behaviorFile = behaviorFile;
        }

        @Override
        public String toString() {

            return String.format("[name: \"%s\"; "
                    + "filename: \"%s\"; "
                    + "frequency: %f; "
                    + "behavior file: \"%s\"]",
                    name, filename, frequency, behaviorFile);
        }
    }


    /* **************************  main method(s)  ************************** */


    /**
     * Application main method.
     *
     * @param argv  sequence of command-line parameters.
     */
    public static void main (final String[] argv) {

        try {
            // initialize arguments handler for requesting the command line
            // values afterwards via get() methods; might throw a
            // NullPointer-, IllegalArgument- or ParseException;
            CommandLineArgumentsHandler.init(argv);

            // might throw FileNotFound-, Security-, IO- or GeneratorException;
            M4jdslModelGenerator.readArgumentsAndGenerate();

        } catch (final Exception ex) {

            System.err.println(ex.getMessage() + ".\n");      
            M4jdslModelGenerator.printUsage();
        }
    }

    /**
     * Starts the generation process with the arguments which have been passed
     * to command line.
     *
     * @throws IOException
     * @throws SecurityException
     * @throws FileNotFoundException
     * @throws GeneratorException
     *
     * @throws TransformationException
     *     if any critical error in the transformation process occurs.
     */
    private static void readArgumentsAndGenerate ()
            throws FileNotFoundException,
                   SecurityException,
                   IOException,
                   GeneratorException {

        final M4jdslModelGenerator m4jdslModelGenerator =
                new M4jdslModelGenerator();

        final String flowsDirectoryPath =
                CommandLineArgumentsHandler.getFlowsDirectoryPath();
        
        final String sessionDatFilePath =
                CommandLineArgumentsHandler.getSessionDatFilePath();

        final String workloadIntensityPropertiesFile =
                CommandLineArgumentsHandler.getWorkloadIntensityPropertiesFile();

        final String xmiOutputFilePath =
                CommandLineArgumentsHandler.getXmiOutputFilePath();

        final String behaviorModelsPropertiesFile =
                CommandLineArgumentsHandler.getBehaviorModelsPropertiesFile();

        final String graphOutputFilePath =
                CommandLineArgumentsHandler.getGraphOutputFilePath();

        final boolean sessionsCanBeExitedAnytime =
                CommandLineArgumentsHandler.getSessionsCanBeExitedAnytime();

        final boolean useFullyQualifiedNames =
                CommandLineArgumentsHandler.getUseFullyQualifiedNames();

        // might throw a FileNotFound- or IOException;
        final Properties workloadIntensityProperties =
                M4jdslModelGenerator.loadProperties(
                        workloadIntensityPropertiesFile);

        // might throw a FileNotFound- or IOException;
        final Properties behaviorModelsProperties =
                (behaviorModelsPropertiesFile != null) ?
                        M4jdslModelGenerator.loadProperties(
                                behaviorModelsPropertiesFile) : null;

        final WorkloadModel workloadModel =
                m4jdslModelGenerator.generateWorkloadModel(
                        workloadIntensityProperties,
                        behaviorModelsProperties,
                        flowsDirectoryPath,                        
                        graphOutputFilePath,
                        sessionDatFilePath,
                        sessionsCanBeExitedAnytime,
                        useFullyQualifiedNames);

        //final String outputFile = generatorProperties.
        //        getProperty(M4jdslModelGenerator.PKEY_XMI_OUTPUT_FILE);
        final String outputFile = xmiOutputFilePath;

        if (outputFile == null) {

            throw new IOException("XMI output file is undefined");
        }

        // might throw an IOException;
        XmiEcoreHandler.getInstance().ecoreToXMI(
                workloadModel,
                xmiOutputFilePath);

        System.out.println("Finished.");
    }

    /**
     * Loads the key/value pairs from a specified properties file.
     *
     * @param filename  name of the properties file to be loaded.
     *
     * @throws FileNotFoundException
     *     in case the denoted file does not exist.
     * @throws IOException
     *     if any error while reading occurs.
     * @throws NullPointerException
     *     if <code>null</code> is passed as filename.
     * @throws SecurityException
     *     if any file access is permitted.
     */
    private static Properties loadProperties (final String filename)
            throws FileNotFoundException, IOException, SecurityException {

        final Properties properties = new Properties();

        // might throw a FileNotFound- or SecurityException;
        final FileInputStream fileInputStream = new FileInputStream(filename);

        try {

            // might throw an IO- or IllegalArgumentException;
            properties.load(fileInputStream);

        } finally {

            if (fileInputStream != null) {

                try {

                    // might throw an IOException;
                    fileInputStream.close();

                } catch (final IOException ex) {

                    // ignore IOException, since this is the "finally" block;
                    // TODO: exception message should be written to log file;
                }
            }
        }

        return properties;
    }
    
   

}