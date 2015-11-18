package de.dfki.opcua.server.method;

import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.NodeManagerUaNode;
import de.dfki.omm.impl.OMMFactory;
import de.dfki.omm.impl.OMMHeaderImpl;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.types.URLType;
import de.dfki.oms.parser.RobotMLParser;
import org.opcfoundation.ua.builtintypes.NodeId;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * A method to create new OMMs on the OMS by parsing RobotML. <br>
 * <br>
 * Arguments to hand to this method: <br>
 * - RobotML text ({@link String}) <br>
 * 
 * @author xekl01
 *
 */
public class OmsMethodLoadRobotML extends OmsMethod {

	private String omsURL;

	/**
	 * Basic Constructor.
	 *
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodLoadRobotML(NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
	}

	/**
	 * Constructor with URL to the OMS.
	 *
	 * @param omsURL		URL to the OMS
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodLoadRobotML(String omsURL, NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
		this.omsURL = omsURL; 
	}
	
	@Override
	public boolean execute () {

		// check arguments 
		Class<?>[] inputFormats = new Class<?>[] { String.class };
		try {
			MethodManager.checkInputArguments(inputFormats, inputArguments, inputArgumentResults, inputArgumentDiagnosticInfos, false);
		} catch (Exception e) {
			System.err.println("Method could not be executed. Input arguments invalid (must be one RobotML String).");
			e.printStackTrace();
			return false;
		}
		
		// parse RobotML
		if (inputArguments[0] != null) {
			RobotMLParser.parseRobotMLString((String) inputArguments[0].getValue(), omsURL);
			return true;
		}
		else {
			System.err.println("No memory descriptions found.");
			throw new RuntimeException("RobotML could not be parsed.");
		}
	}

}
