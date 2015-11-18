package de.dfki.opcua.server.method;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.opcfoundation.ua.builtintypes.NodeId;

import com.prosysopc.ua.server.NodeManagerUaNode;

import de.dfki.omm.acl.OMSCredentials;
import de.dfki.omm.impl.OMMFactory;
//import de.dfki.oms.security.acl.OMMUsernamePasswordCredentials;


/**
 * A method to delete a given OMM on the OMS. 
 * 
 * @author xekl01
 *
 */
public class OmsMethodDeleteOMM extends OmsMethod {

	private String ommURL;

	/**
	 * Basic Constructor.
	 * 
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodDeleteOMM (NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
	}
	
	/**
	 * Constructor with URL to the OMM to be deleted. 
	 * 
	 * @param ommURL		URL to the OMM
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodDeleteOMM (String ommURL, NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
		this.ommURL = ommURL; 
	}

	@Override
	public boolean execute () {

		// handle authentication
//		String authUser = null;
//		String authPw = null;
//		if (userIdentity != null) {
//			authUser = userIdentity.getName();
//			authPw = userIdentity.getPassword();
//		}

		// delete memory
		try {
			OMSCredentials creds = null;
//			if (authUser != null && authPw != null) creds = new OMMUsernamePasswordCredentials(authUser, authUser, authPw);
			boolean success = OMMFactory.deleteOMMViaOMSRestInterface(new URL(ommURL), creds);
			if (success) return true;
			else {
				System.err.println("OMM \""+ommURL+"\" could not be deleted. Check for secure blocks or user rights.");
				throw new RuntimeException(ommURL);
			}
		} catch (MalformedURLException e) {
			System.err.println("Not a valid URL: "+ommURL);
			throw new RuntimeException(ommURL);
		}

//		return false;
	}

	
	
}
