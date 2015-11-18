package de.dfki.opcua.server.method;

import java.util.Locale;

import org.opcfoundation.ua.builtintypes.NodeId;
import org.restlet.data.ChallengeScheme;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.NodeManagerUaNode;

import de.dfki.omm.impl.OMMFactory;


/**
 * A method to change the owner of an existing OMM. <br>
 * <br>
 * Arguments to hand to this method: <br>
 * - clear text name of the new owner ({@link String}) <br>
 * - user name of the new owner ({@link String}) <br>
 * - password of the new owner ({@link String}) 
 * 
 * @author xekl01
 *
 */
public class OmsMethodChangeOwner extends OmsMethod {

	private String ownerURL;
	
	/**
	 * Basic Constructor.
	 * 
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodChangeOwner (NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
		userIdentity = null;
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
	public OmsMethodChangeOwner (String ownerURL, NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		this(parentNode, methodNodeId, methodName, locale);
		this.ownerURL = ownerURL; 
	}
	
	
	@Override
	public boolean execute () {

		// check arguments 
		Class<?>[] inputFormats = new Class<?>[] { String.class, String.class, String.class };
		try {
			MethodManager.checkInputArguments(inputFormats, inputArguments, inputArgumentResults, inputArgumentDiagnosticInfos, false);
		} catch (Exception e) {
			System.err.println("Method could not be executed. Input arguments invalid (must be three strings).");
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

		// parse arguments
		String newCleartextname = (String) inputArguments[0].getValue();
		String newUsername = (String) inputArguments[1].getValue();
		String newPassword = (String) inputArguments[2].getValue();
		
		// change owner
		ClientResource ownerCr = new ClientResource(ownerURL); 
		if (authUser != null && authPw != null) 
			ownerCr.setChallengeResponse(ChallengeScheme.HTTP_BASIC, authUser, authPw);
		try {
			ownerCr.put(OMMFactory.createOMMOwnerStringFromUsernamePassword(newCleartextname, newUsername, newPassword));
		} catch (ResourceException e) {
			if (e.getMessage().equals("Unauthorized")) System.err.println("Owner could not be changed. Check user rights.");
			else System.err.println("Owner could not be changed. Check input format.");
			throw new RuntimeException(e);
//			return false;
		}

		return true;
	}

}
