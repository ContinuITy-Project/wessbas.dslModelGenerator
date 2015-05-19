package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolExitState;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.ProtocolState;
import m4jdsl.ProtocolTransition;
import m4jdsl.Request;
import net.sf.markov4jmeter.behaviormodelextractor.extraction.parser.SessionData;
import net.sf.markov4jmeter.behaviormodelextractor.extraction.parser.UseCase;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

public class HTTPProtocolLayerEFSMGenerator
extends AbstractProtocolLayerEFSMGenerator {
	
	/* ***************************  Global Variables  *************************** */
	
    HashMap<String, HashSet<String>> parameterMap = new HashMap<String, HashSet<String>>();
	
    /* ***************************  constructors  *************************** */

    /**
     * Constructor for a Protocol Layer EFSM with Http requests.
     *
     * @param m4jdslFactory
     *     instance for creating M4J-DSL model elements.
     * @param idGenerator
     *     instance for creating unique Protocol State IDs.
     * @param requestIdGenerator
     *     instance for creating unique request IDs.
     */
    public HTTPProtocolLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final IdGenerator idGenerator,
            final IdGenerator requestIdGenerator,
            final ArrayList<SessionData> sessions) {
        super(m4jdslFactory, idGenerator, requestIdGenerator, sessions);        
    }

    /* **************************  public methods  ************************** */

    /**
     * Creates a Protocol Layer EFSM.
     *
     * @return
     *     the newly created Protocol Layer EFSM.
     *
     * @throws GeneratorException
     *     if any error during the generation process occurs.
     */
    @Override
    public ProtocolLayerEFSM generateProtocolLayerEFSM (
            final String serviceName) throws GeneratorException {
    	
    	ArrayList<UseCase> relatedUseCases = new ArrayList<UseCase>();    	
    	String ip = "";    
    	int port = 0;    
    	String uri = "";   
    	String method = "";   
    	String encoding = "";   
    	String protocol = "";  

    	// get useCases for this serviceName
    	for (SessionData sessionData : this.sessions)  {
    		for (UseCase useCase : sessionData.getUseCases()) {
    			if (useCase.getName().equals(serviceName)) {
    				relatedUseCases.add(useCase);
    			}
    		}
    	}    	
    		
    	if (relatedUseCases.size() > 0 ) {
    		// take the value form the first useCase
    		ip = relatedUseCases.get(0).getIp();
    		port = relatedUseCases.get(0).getPort();
    		uri = relatedUseCases.get(0).getUri();
    		method = relatedUseCases.get(0).getMethode();
    		encoding = relatedUseCases.get(0).getEncoding();
    		protocol = relatedUseCases.get(0).getProtocol();
    		initializeParameterMap(relatedUseCases);		
    	}    	    	
   
        final ProtocolLayerEFSM protocolLayerEFSM =
                this.createEmptyProtocolLayerEFSM();

        final ProtocolExitState protocolExitState =
                protocolLayerEFSM.getExitState();

        String[][] requestParameter = new String[parameterMap.keySet().size()][2];
        int i = 0;
        for (String key : parameterMap.keySet()) {
        	HashSet<String> parameterValues = parameterMap.get(key);
        	requestParameter[i][0] =  key;
        	requestParameter[i][1] =  getValuesAsString(parameterValues, ";");
        	i++;
        }

        // z.B. http://localhost:8080/action-servlet/ActionServlet?action=sellInventory
        final Request request = this.createRequest(
                AbstractProtocolLayerEFSMGenerator.REQUEST_TYPE_HTTP,
                new String[][] {  // properties;
                        {"HTTPSampler.domain", ip},
                        {"HTTPSampler.port",  Integer.toString(port)},
                        {"HTTPSampler.path",  uri},
                        {"HTTPSampler.method", method},
                        {"HTTPSampler.encoding", encoding},
                        {"HTTPSampler.protocol", protocol}
                }, requestParameter);

        String eId = request.getEId();
        eId = eId + " (" + serviceName + ")";
        request.setEId(eId);

        final ProtocolState protocolState = this.createProtocolState(request);

        final String guard;  // no SUT-specific guard available yet ...
        final String action;  // no SUT-specific action available yet ...

        final ProtocolTransition protocolTransition =
                this.createProtocolTransition(
                        protocolExitState,
                        "",
                        "");

        protocolState.getOutgoingTransitions().add(protocolTransition);
        protocolLayerEFSM.getProtocolStates().add(protocolState);
        protocolLayerEFSM.setInitialState(protocolState);

        return protocolLayerEFSM;
    }
    
    /**
     * @param parameterValues
     * @param delimiter
     * @return String
     */
    private String getValuesAsString(final HashSet<String> parameterValues, final String delimiter) {
    	String returnString = "";
    	for (String value : parameterValues) {
    		returnString += value + delimiter;
    	}
    	return returnString;
    }
    
    /**
     * 
     * http://stackoverflow.com/questions/13592236/parse-the-uri-string-into-name-value-collection-in-java.
     * 
     * @param queryString
     * @return Map<String, List<String>>
     * @throws UnsupportedEncodingException
     */
    private static Map<String, List<String>> splitQuery(String queryString) throws UnsupportedEncodingException {
    	  final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();    	  
    	  final String[] pairs = queryString.split("&");    	  
    	  for (String pair : pairs) {
    	    final int idx = pair.indexOf("=");
    	    final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
    	    if (!query_pairs.containsKey(key)) {
    	      query_pairs.put(key, new LinkedList<String>());
    	    }
    	    final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
    	    query_pairs.get(key).add(value);
    	  }
    	  
     return query_pairs;
   }    
    
    /**
     * Init parameterMap.
     * 
     * @param relatedSessions
     */
    private void initializeParameterMap(final ArrayList<UseCase> relatedUseCases) {
		for (UseCase useCase : relatedUseCases) {
			try {
				Map<String, List<String>> parameterRequest = splitQuery(useCase.getQueryString());
				for (String parameterName : parameterRequest.keySet()) {					
					if (!parameterName.equals("<no-query-string>")) {						
						List<String> parameterValues = parameterRequest.get(parameterName);
						for (String parameterValue : parameterValues) {
							addToParameters(parameterName, parameterValue);
						}
					}		
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}   	
    }
    
    /**
     * Add key value pairs to parameterMap.
     * 
	 * @param key
	 * @param value
	 */
	private void addToParameters(String key, String value) {
	   if (parameterMap.get(key) != null ) {
		   HashSet<String> valueString = parameterMap.get(key);
		   valueString.add(value);
		   parameterMap.put(key, valueString);
	   } else {
		   HashSet<String> valueString = new HashSet<String>();
		   valueString.add(value);
		   parameterMap.put(key, valueString);
	   }
	}
    
}
