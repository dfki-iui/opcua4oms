package de.dfki.opcua.server.method;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.Variant;

import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.NodeManagerUaNode;

import de.dfki.omm.acl.OMSCredentials;
import de.dfki.omm.impl.rest.OMMRestImpl;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.types.OMMRestAccessMode;
// import de.dfki.oms.security.acl.OMMUsernamePasswordCredentials;
import de.dfki.opcua.server.OmsParser;

/**
 * A method to search for a block in an OMM by its contents. <br>
 * <br>
 * Arguments to hand to this method: <br>
 * - query items for namespace ({@link String}) <br>
 * - query items for format ({@link String}) <br>
 * - query items for creator ({@link String}) <br>
 * - query items for title ({@link String}) <br>
 * - query items for description ({@link String}) <br>
 * - query items for type ({@link String}) <br>
 * - query items for subject ({@link String}) <br>
 * - query items for payload ({@link String}) <br>
 * - query items for link({@link String}) <br>
 * 
 * @author xekl01
 *
 */
public class OmsMethodSearchBlock extends OmsMethod {

	private String memoryURL;

	private Variant[] output;
	
	/**
	 * Basic Constructor.
	 * 
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodSearchBlock (NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
	}
	
	/**
	 * Constructor with URL to the memory. 
	 * 
	 * @param memoryURL		URL to the OMS
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodSearchBlock (String memoryURL, NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
		this.memoryURL = memoryURL; 
	}
	
	@Override
	public boolean execute () {

		// check arguments 
		Class<?>[] inputFormats = new Class<?>[] { String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class };
		try {
			MethodManager.checkInputArguments(inputFormats, inputArguments, inputArgumentResults, inputArgumentDiagnosticInfos, false);
		} catch (Exception e) {
			System.err.println("Method could not be executed. Input arguments invalid.");
			e.printStackTrace();
			return false;
		}
		
		// handle authentication
//		String authUser = null;
//		String authPw = null;
//		if (userIdentity != null) {
//			authUser = userIdentity.getName();
//			authPw = userIdentity.getPassword();
//		}

		// create omm and get its blocks
		OMSCredentials creds = null;
//		if (authUser != null && authPw != null) creds = new OMMUsernamePasswordCredentials(authUser, authUser, authPw);
		OMMRestImpl omm;
		omm = new OMMRestImpl(memoryURL, OMMRestAccessMode.CompleteDownloadUnlimited, creds);
		Collection<OMMBlock> blocks = null;
		try {
			blocks = omm.getAllBlocks();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		// search blocks for queried contents
		if (blocks != null && blocks.size() > 0) {
			
			String query;
			HashSet<String> results = new HashSet<String>();
			
			// search for namespace (if queried)
			query = (String) inputArguments[0].getValue();
			if (query != null && query.length() > 0) {
				for (OMMBlock block : blocks) 
					if (OmsParser.parseNamespace(block).contains(query))
						results.add(block.getID());
			}
			
			// search for format (if queried)
			query = (String) inputArguments[1].getValue();
			if (query != null && query.length() > 0) {
				for (OMMBlock block : blocks) 
					if (OmsParser.parseFormat(block).contains(query))
						results.add(block.getID());
			}
			
			// search for creator (if queried)
			query = (String) inputArguments[2].getValue();
			if (query != null && query.length() > 0) {
				for (OMMBlock block : blocks) 
					if (OmsParser.parseCreator(block).contains(query))
						results.add(block.getID());
			}
			
			// search for title (if queried)
			query = (String) inputArguments[3].getValue();
			if (query != null && query.length() > 0) {
				for (OMMBlock block : blocks) 
					if (OmsParser.parseTitle(block).contains(query))
						results.add(block.getID());
			}
			
			// search for description (if queried)
			query = (String) inputArguments[4].getValue();
			if (query != null && query.length() > 0) {
				for (OMMBlock block : blocks) 
					if (OmsParser.parseDescription(block).contains(query))
						results.add(block.getID());
			}
			
			// search for type (if queried)
			query = (String) inputArguments[5].getValue();
			if (query != null && query.length() > 0) {
				for (OMMBlock block : blocks) 
					if (OmsParser.parseType(block).contains(query))
						results.add(block.getID());
			}
			
			// search for subject (if queried)
			query = (String) inputArguments[6].getValue();
			if (query != null && query.length() > 0) {
				for (OMMBlock block : blocks) 
					if (OmsParser.parseSubject(block).contains(query))
						results.add(block.getID());
			}
			
			// search for payload (if queried)
			query = (String) inputArguments[7].getValue();
			if (query != null && query.length() > 0) {
				for (OMMBlock block : blocks) 
					if (OmsParser.parsePayload(block).contains(query))
						results.add(block.getID());
			}
			
			// search for link (if queried)
			query = (String) inputArguments[8].getValue();
			if (query != null && query.length() > 0) {
				for (OMMBlock block : blocks) 
					if (OmsParser.parseLink(block).contains(query))
						results.add(block.getID());
			}
			
			// add search results to output
			if (!results.isEmpty()) {
				output = new Variant[1];
				output[0] = new Variant(results.toArray(new String[0]));
				return true;
			}
			return false;
		}
		
		// there are no blocks
		else 
			return false;
	}

	/**
	 * Calls this method's execute() method and returns its result (if there is any).
	 * 
	 * @return Variant[] with the method's result in the first slot, or null
	 */
	public Variant[] executeWithOutput() {
		if (execute()) return output;
		else return null;
	}

}
