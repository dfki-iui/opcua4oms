package de.dfki.opcua.server.method;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.opcfoundation.ua.builtintypes.NodeId;

import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.NodeManagerUaNode;

import de.dfki.omm.impl.OMMFactory;
import de.dfki.omm.impl.OMMHeaderImpl;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.types.URLType;

/**
 * A method to create a new OMM on the OMS. <br>
 * <br>
 * Arguments to hand to this method: <br>
 * - name of the new memory ({@link String}) <br>
 * - clear text name of the owner ({@link String}) <br>
 * - user name of the owner ({@link String}) <br>
 * - password of the owner ({@link String}) 
 * 
 * @author xekl01
 *
 */
public class OmsMethodCreateOMM extends OmsMethod {

	private String omsURL;

	/**
	 * Basic Constructor.
	 * 
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodCreateOMM (NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
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
	public OmsMethodCreateOMM (String omsURL, NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
		this.omsURL = omsURL; 
	}
	
	@Override
	public boolean execute () {

		// check arguments 
		Class<?>[] inputFormats = new Class<?>[] { String.class, String.class, String.class, String.class };
		try {
			MethodManager.checkInputArguments(inputFormats, inputArguments, inputArgumentResults, inputArgumentDiagnosticInfos, false);
		} catch (Exception e) {
			System.err.println("Method could not be executed. Input arguments invalid (must be four strings).");
			e.printStackTrace();
			return false;
		}
		
		// create header
		String memoryName = (String) inputArguments[0].getValue();
		OMMHeaderImpl header = null;
		try {
			header = (OMMHeaderImpl) OMMHeaderImpl.create(new URLType(new URL(omsURL+"/rest/"+memoryName)), null);
		} catch (MalformedURLException e) {
			System.err.println("Not a valid URL: "+omsURL+"/rest/"+memoryName);
			e.printStackTrace();
		}

		// create ownerBlock
		String ownerClear = (String) inputArguments[1].getValue();
		String ownerUser = (String) inputArguments[2].getValue();
		String ownerPassword = (String) inputArguments[3].getValue();
		if (ownerClear == null || ownerClear.equals("")) 
			if (userIdentity != null) ownerClear = userIdentity.getName();
		if (ownerUser == null || ownerUser.equals("")) 
			if (userIdentity != null) ownerUser = userIdentity.getName();
		if (ownerPassword == null || ownerPassword.equals("")) 
			if (userIdentity != null) ownerPassword = userIdentity.getPassword();
		String ownerString = OMMFactory.createOMMOwnerStringFromUsernamePassword(ownerClear, ownerUser, ownerPassword);
		OMMBlock ownerBlock = OMMFactory.createOMMOwnerBlock(header, ownerString);

		// create new memory
		if (OMMFactory.createOMMViaOMSRestInterface(omsURL + "/mgmt/createMemory", header, ownerBlock))
			return true;
		else 
			System.err.println("Memory \""+memoryName+"\" could not be created.");
			throw new RuntimeException(memoryName);
	}

}
