package de.dfki.opcua.server.method;

import java.io.IOException;
import java.util.Locale;

import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.Variant;
import org.restlet.data.ChallengeScheme;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.NodeManagerUaNode;


/**
 * A method to change the ACL of an existing OMM. <br>
 * <br>
 * Arguments to hand to this method: <br>
 * - an XML representation of an ACL entry (as {@link String})  <br>
 * 
 * @author xekl01
 *
 */
public class OmsMethodChangeACL extends OmsMethod {

	private String aclURL;
	
	/**
	 * Basic Constructor.
	 * 
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodChangeACL (NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
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
	public OmsMethodChangeACL (String aclURL, NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
		this.aclURL = aclURL; 
	}

	@Override
	public void initialize(Variant[] inputArguments, StatusCode[] inputArgumentResults, DiagnosticInfo[] inputArgumentDiagnosticInfos) {
		super.initialize(inputArguments, inputArgumentResults, inputArgumentDiagnosticInfos);
	}

	@Override
	public boolean execute () {

		// check arguments 
		Class<?>[] inputFormats = new Class<?>[] { String.class };
		try {
			MethodManager.checkInputArguments(inputFormats, inputArguments, inputArgumentResults, inputArgumentDiagnosticInfos, false);
		} catch (Exception e) {
			System.err.println("Method could not be executed. Input arguments invalid (expecting one String).");
			e.printStackTrace();
			return false;
		}

		// handle authentication
		String authUser = null;
		String authPw = null;
		if (userIdentity != null) {
			authUser = userIdentity.getName();
			authPw = userIdentity.getPassword();
		}
		
		// parse argument
		String newACL = (String) inputArguments[0].getValue();

		// change ACL
		ClientResource aclCr = new ClientResource(aclURL); // access ACL
		if (authUser != null && authPw != null) 
			aclCr.setChallengeResponse(ChallengeScheme.HTTP_BASIC, authUser, authPw);
		try {
			aclCr.put(newACL).getText();
		} catch (ResourceException | IOException e) {
			if (e.getMessage().equals("Unauthorized")) System.err.println("ACL could not be changed. Check user rights.");
			else System.err.println("ACL could not be changed. Check input format (must be an ACL entry in XML format).");
			throw new RuntimeException(e);
//			return false;
		}

		return true;
	}

}
