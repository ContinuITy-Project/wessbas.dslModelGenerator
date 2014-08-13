package net.sf.markov4jmeter.m4jdslmodelgenerator;


import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class defines the command-line options accepted by the M4J-DSL Model
 * Generator. Each option is initiated by a leading hyphen; an overview is
 * given below, followed by several examples.
 *
 * <p>Available command-line options:
 * <table border="1">
 *   <tr><th>Long         </th>
 *       <th> Short       </th>
 *       <th> Description </th>
 *
 *   <tr><td><code> flows </code></td>
 *       <td><code> f     </code></td>
 *       <td> Path to the directory of input Flows that indicate the Session
 *       Layer EFSM structure, e.g., "./flows/". </td>
 *
 *   <tr><td><code> workloadIntensity </code></td>
 *       <td><code> w                 </code></td>
 *       <td> Properties file which provides the workload intensity information,
 *       e.g., "workloadintensity.properties". </td>
 *
 *   <tr><td><code> output </code></td>
 *       <td><code> o      </code></td>
 *       <td> Output file for the M4J-DSL Model, e.g., "workloadmodel.xmi".
 *       </td>
 *
 *   <tr><td colspan="3" align="center">
 *       <i>Optional Arguments</i>
 *       </td></tr>
 *
 *   <tr><td><code> behavior </code></td>
 *       <td><code> b        </code></td>
 *       <td> (Optional) properties file which specifies the user behavior
 *       information to be included optionally, e.g.,
 *       "behaviorModels.properties". </td>
 *
 *   <tr><td><code> graph </code></td>
 *       <td><code> g     </code></td>
 *       <td> (Optional) output file for the DOT graph that represents the
 *       Session Layer EFSM, e.g., "graph.dot". </td>
 *
 *   <tr><td><code> exitAnytime </code></td>
 *       <td><code> e           </code></td>
 *       <td> (Optional) flag that indicates whether sessions can by exited at
 *       any time, e.g., by closing a browser window in session-based Web
 *       applications; the default value is <code>true</code> (sessions can be
 *       exited at any time). </td>
 *
 *   <tr><td><code> qualifiedNames </code></td>
 *       <td><code> q              </code></td>
 *       <td> (Optional) flag that indicates whether fully qualified names
 *       shall be used for services/states. If this flag is <code>true</code>,
 *       each service/state name is formatted as "<i>Fname</i>.<i>Nname</i>",
 *       whereas <i>Fname</i> and <i>Nname</i> denote the names of the
 *       corresponding Flows and Nodes respectively; the default value is
 *       <code>true</code>. </td>
 * </table>
 *
 * <p>Examples: TODO: revise remaining comments!
 * <ul>
 *   <li>The options sequence
 *   <blockquote>
 *     <code>-i WorkloadModel.xmi -o testplan.jmx -t testplan.properties</code>
 *   </blockquote>
 *   denotes a minimum start configuration for the Test Plan Generator, since
 *   it defines the files "WorkloadModel.xmi" and "testplan.jmx" to be used as
 *   input file and output file respectively, and it directs the generator to
 *   use the default values provided by file "testplan.properties" for Test Plan
 *   elements.
 *   </li>
 *
 *   <li>The options sequence
 *   <blockquote>
 *     <code>-i WorkloadModel.xmi -o testplan.jmx -t testplan.properties
 *     -l 2</code>
 *   </blockquote>
 *   has the same effect as the first one, but it additionally defines a
 *   MacOS-specific line-break type to be used for the CSV files of the
 *   Behavior Models.
 *
 *   <li>The options sequence
 *   <blockquote>
 *     <code>-i WorkloadModel.xmi -o testplan.jmx -t testplan.properties
 *     -l 2 -g generator.properties -r</code>
 *   </blockquote>
 *   has the same effect as the second one, but it additionally passes a custom
 *   configuration file for the generator and directs the generator to start
 *   the JMeter engine finally, for running a test with the resulting Test Plan.
 *   </li>
 * </ul>
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class CommandLineArgumentsHandler {

    /** Path to the directory of input Flows that indicate the Session Layer
     *  EFSM structure. */
    private final static Option FLOWS_DIRECTORY_PATH =
            CmdlOptionFactory.createOption(
                    "f",                                   // opt;
                    "flows",                               // longOpt;
                    "Path to the directory of input "      // description;
                    + "Flows that indicate the Session Layer EFSM structure.",
                    true,                                  // isRequired;
                    "./flows/",                            // argName;
                    false);                                // !hasOptionalArg;

    /** Properties file which provides the workload intensity information. */
    private final static Option WORKLOAD_INTENSITY_PROPERTIES_FILE =
            CmdlOptionFactory.createOption(
                    "w",                                   // opt;
                    "workloadIntensity",                   // longOpt;
                    "Properties file which provides the "  // description;
                    + "workload intensity information.",
                    true,                                  // isRequired;
                    "workloadintensity.properties",        // argName;
                    false);                                // !hasOptionalArg;

    /** Output file for the M4J-DSL Model. */
    private final static Option XMI_OUTPUT_FILE_PATH =
            CmdlOptionFactory.createOption(
                    "o",                                   // opt;
                    "output",                              // longOpt;
                    "Output file for the M4J-DSL Model.",  // description;
                    true,                                  // isRequired;
                    "workloadmodel.xmi",                   // argName;
                    false);                                // !hasOptionalArg;

    /** (Optional) properties file which specifies the user behavior information
     *  to be included optionally. */
    private final static Option BEHAVIOR_MODELS_PROPERTIES_FILE =
            CmdlOptionFactory.createOption(
                    "b",                                   // opt;
                    "behavior",                            // longOpt;
                    "(Optional) properties file which "    // description;
                    + "specifies the user behavior information to be included optionally.",
                    false,                                 // !isRequired;
                    "behaviorModels.properties",                                    // argName;
                    false);                                // !isRequired;

    /** (Optional) output file for the DOT graph that represents the Session
     *  Layer EFSM. */
    private final static Option GRAPH_OUTPUT_FILE_PATH =
            CmdlOptionFactory.createOption(
                    "g",                                   // opt;
                    "graph",                               // longOpt;
                    "(Optional) output file for the DOT "  // description;
                    + "graph that represents the Session Layer EFSM.",
                    false,                                 // !isRequired;
                    "graph.dot",                           // argName;
                    false);                                // !hasOptionalArg;

    /** (Optional) flag that indicates whether sessions can by exited at any
     *  time, e.g., by closing a browser window in session-based Web
     *  applications. */
    private final static Option SESSIONS_CAN_BE_EXITED_ANYTIME =
            CmdlOptionFactory.createOption(
                    "e",                                   // opt;
                    "exitAnytime",                         // longOpt;
                    "(Optional) flag that indicates "      // description;
                    + "whether sessions can by exited at any time, e.g., by closing a browser window in session-based Web applications.",
                    false,                                 // !isRequired;
                    "true",                                // argName;
                    false);                                // !hasOptionalArg;

    /** (Optional) flag that indicates whether fully qualified names shall be
     *  used for services/states. */
    private final static Option USE_FULLY_QUALIFIED_NAMES =
            CmdlOptionFactory.createOption(
                    "q",                                    // opt;
                    "qualifiedNames",                       // longOpt;
                    "(Optional) flag that indicates "       // description;
                    + "whether fully qualified names shall be used for services/states.",
                    false,                                  // !isRequired;
                    "true",                                 // argName;
                    false);                                 // !hasOptionalArg;

    /** Formatter for printing the usage instructions. */
    private final static HelpFormatter HELP_FORMATTER = new HelpFormatter();

    /** Basic parser for extracting values from command-line input. */
    private final static CommandLineParser PARSER = new BasicParser();


    /* *********************  global (non-final) fields  ******************** */


    /** Input file path which has been read from command-line. */
    private static String flowsDirectoryPath;

    /** Test Plan properties file path which has been read from command-line. */
    private static String workloadIntensityPropertiesFile;

    /** Output file path which has been read from command-line. */
    private static String xmiOutputFilePath;

    /** (Optional) line-break type which has been read from command-line. */
    private static String behaviorModelsPropertiesFile;

    /** (Optional) output path which has been read from command-line. */
    private static String graphOutputFilePath;

    /** (Optional) generator properties file path which has been read from
     *  command-line. */
    private static boolean sessionsCanBeExitedAnytime;

    /** Flag indicating whether a test run shall be started immediately. */
    private static boolean useFullyQualifiedNames;

    /** Command-line options to be parsed. */
    private static Options options;


    /* ***************************  static blocks  ************************** */


    static {

        // fill the options container;
        CommandLineArgumentsHandler.options = new Options();

        CommandLineArgumentsHandler.options.addOption(
                CommandLineArgumentsHandler.FLOWS_DIRECTORY_PATH);

        CommandLineArgumentsHandler.options.addOption(
                CommandLineArgumentsHandler.WORKLOAD_INTENSITY_PROPERTIES_FILE);

        CommandLineArgumentsHandler.options.addOption(
                CommandLineArgumentsHandler.XMI_OUTPUT_FILE_PATH);

        CommandLineArgumentsHandler.options.addOption(
                CommandLineArgumentsHandler.BEHAVIOR_MODELS_PROPERTIES_FILE);

        CommandLineArgumentsHandler.options.addOption(
                CommandLineArgumentsHandler.GRAPH_OUTPUT_FILE_PATH);

        CommandLineArgumentsHandler.options.addOption(
                CommandLineArgumentsHandler.SESSIONS_CAN_BE_EXITED_ANYTIME);

        CommandLineArgumentsHandler.options.addOption(
                CommandLineArgumentsHandler.USE_FULLY_QUALIFIED_NAMES);
    }


    /* **************************  public methods  ************************** */

    // TODO: revise remaining comments!

    /**
     * Returns the input file path which has been read from command-line.
     *
     * @return  a valid <code>String</code> which represents a file path.
     */
    public static String getFlowsDirectoryPath () {

        return CommandLineArgumentsHandler.flowsDirectoryPath;
    }

    /**
     * Returns the output file path which has been read from command-line.
     *
     * @return  a valid <code>String</code> which represents a file path.
     */
    public static String getWorkloadIntensityPropertiesFile () {

        return CommandLineArgumentsHandler.workloadIntensityPropertiesFile;
    }

    /**
     * Returns the Test Plan properties file path which has been read from
     * command-line.
     *
     * @return
     *     a valid <code>String</code> which represents a file path, or
     *     <code>null</code> if no file path has been read.
     */
    public static String getXmiOutputFilePath () {

        return CommandLineArgumentsHandler.xmiOutputFilePath;
    }

    /**
     * Returns the (optional) line-break value which has been read from
     * command-line.
     *
     * @return  an integer value which represents the line-break type.
     */
    public static String getBehaviorModelsPropertiesFile () {

        return CommandLineArgumentsHandler.behaviorModelsPropertiesFile;
    }

    /**
     * Returns the (optional) output path which has been read from command-line.
     *
     * @return
     *     a <code>String</code> which denotes the location of an (existing)
     *     directory.
     */
    public static String getGraphOutputFilePath () {

        return CommandLineArgumentsHandler.graphOutputFilePath;
    }

    /**
     * Returns the (optional) generator properties file path which has been read
     * from command-line.
     *
     * @return
     *     a valid <code>String</code> which represents a file path, or
     *     <code>null</code> if no file path has been read.
     */
    public static boolean getSessionsCanBeExitedAnytime () {

        return CommandLineArgumentsHandler.sessionsCanBeExitedAnytime;
    }

    /**
     * Returns the filter flags which have been read from command-line.
     *
     * @return
     *     a valid <code>String</code>, or <code>null</code> if no filter flags
     *     have been read.
     */
    public static boolean getUseFullyQualifiedNames () {

        return CommandLineArgumentsHandler.useFullyQualifiedNames;
    }

    /**
     * Prints the usage instructions to standard output.
     */
    public static void printUsage () {

        CommandLineArgumentsHandler.HELP_FORMATTER.printHelp(
            M4jdslModelGenerator.class.getSimpleName(),
            CommandLineArgumentsHandler.options);
    }

    /**
     * Initializes the handler by parsing the given array of arguments; the
     * parsed values might be requested through the <code>get()</code> methods
     * of this class.
     *
     * @param args
     *     sequence of <code>String</code>s to be parsed; might comply with
     *     the arguments which have been passed to the <code>main()</code>
     *     method of the application.
     *
     * @throws ParseException
     *     if the given arguments do not match the set of options which
     *     is predefined by this class.
     * @throws NullPointerException
     *     if <code>null</code> has been passed as a parameter, or if the value
     *     of any required option is undefined (<code>null</code>).
     * @throws IllegalArgumentException
     *     if an option flag denotes an empty <code>String</code>
     *     (<code>""</code>).
     */
    public static void init (final String[] args)
        throws ParseException, NullPointerException, IllegalArgumentException {

        // might throw a ParseException;
        final CommandLine commandLine =
                CommandLineArgumentsHandler.parseCommands(args);

        CommandLineArgumentsHandler.flowsDirectoryPath =
                CommandLineArgumentsHandler.readOptionValueAsString(
                        commandLine,
                        CommandLineArgumentsHandler.FLOWS_DIRECTORY_PATH);

        CommandLineArgumentsHandler.workloadIntensityPropertiesFile =
                CommandLineArgumentsHandler.readOptionValueAsString(
                        commandLine,
                        CommandLineArgumentsHandler.
                        WORKLOAD_INTENSITY_PROPERTIES_FILE);

        CommandLineArgumentsHandler.xmiOutputFilePath =
                CommandLineArgumentsHandler.readOptionValueAsString(
                        commandLine,
                        CommandLineArgumentsHandler.XMI_OUTPUT_FILE_PATH);

        CommandLineArgumentsHandler.behaviorModelsPropertiesFile =
                CommandLineArgumentsHandler.readOptionValueAsString(
                        commandLine,
                        CommandLineArgumentsHandler.
                        BEHAVIOR_MODELS_PROPERTIES_FILE);

        CommandLineArgumentsHandler.graphOutputFilePath =
                CommandLineArgumentsHandler.readOptionValueAsString(
                        commandLine,
                        CommandLineArgumentsHandler.GRAPH_OUTPUT_FILE_PATH);

        CommandLineArgumentsHandler.sessionsCanBeExitedAnytime =
                CommandLineArgumentsHandler.readOptionValueAsBoolean(
                        commandLine,
                        CommandLineArgumentsHandler.
                        SESSIONS_CAN_BE_EXITED_ANYTIME,
                        true);

        CommandLineArgumentsHandler.useFullyQualifiedNames =
                CommandLineArgumentsHandler.readOptionValueAsBoolean(
                        commandLine,
                        CommandLineArgumentsHandler.USE_FULLY_QUALIFIED_NAMES,
                        true);
    }


    /* **************************  private methods  ************************* */


    /**
     * Reads the value for a given option from the specified command-line as
     * <code>String</code>.
     *
     * @param commandLine  command-line which provides the values.
     * @param option       option whose value shall be read from command-line.
     *
     * @return
     *     a valid <code>String</code>, or <code>null</code> if the option's
     *     value is optional and undefined.
     *
     * @throws NullPointerException
     *     in case the value is required, but could not be read as
     *     <code>String</code>.
     * @throws IllegalArgumentException
     *     if an option flag denotes an empty <code>String</code>
     *     (<code>""</code>).
     */
    private static String readOptionValueAsString (
            final CommandLine commandLine,
            final Option option)
                    throws NullPointerException, IllegalArgumentException {

        String value;  // to be returned;

        final String opt = option.getOpt();

        // build an instance for reading "typed" options from command-line;
        final CmdlOptionsReader cmdlOptionsReader =
                new CmdlOptionsReader(commandLine);

        try {

            // might throw a NullPointer- or IllegalArgumentException;
            value = cmdlOptionsReader.readOptionValueAsString(opt);

        } catch (final Exception ex) {

            if ( option.isRequired() ) {

                throw ex;

            } else {

                value = null;  // accept undefined value for optional option;
            }
        }

        return value;
    }

    /**
     * Reads the value for a given option from the specified command-line as
     * <code>int</code>.
     *
     * @param commandLine  command-line which provides the values.
     * @param option       option whose value shall be read from command-line.
     *
     * @return
     *     an <code>int</code> value which is 0, if the option's value is
     *     optional and undefined.
     *
     * @throws NullPointerException
     *     in case the value is required, but could not be read as
     *     <code>int</code>.
     * @throws NumberFormatException
     *     if the parsed value does not denote an <code>int</code> value.
     */
    private static boolean readOptionValueAsBoolean (
            final CommandLine commandLine,
            final Option option,
            final boolean defaultValue) throws NullPointerException {

        boolean value;  // to be returned;

        final String opt = option.getOpt();

        // build an instance for reading "typed" options from command-line;
        final CmdlOptionsReader cmdlOptionsReader =
                new CmdlOptionsReader(commandLine);

        try {

            // might throw a NullPointerException;
            value = cmdlOptionsReader.readOptionValueAsBoolean(opt);

        } catch (final Exception ex) {

            if ( option.isRequired() ) {

                throw ex;

            } else {

                value = defaultValue;
            }
        }

        return value;
    }

    /**
     * Parses the given user input and builds an instance of
     * {@link CommandLine}.
     *
     * @param args
     *     user input as it might have been passed to the <code>main()</code>
     *     method of the application before.
     *
     * @return
     *     an instance of {@link CommandLine} to be used for requesting any
     *     input values.
     *
     * @throws ParseException
     *     in case the given arguments do not match the predefined set of
     *     options.
     */
    private static CommandLine parseCommands (final String[] args)
            throws ParseException {

        // might throw a ParseException; returns a CommandLine, if successful;
        return CommandLineArgumentsHandler.PARSER.parse(
                CommandLineArgumentsHandler.options, args);
    }
}