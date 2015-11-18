package de.dfki.opcua.server.method;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.opcfoundation.ua.builtintypes.NodeId;
import org.w3c.dom.Element;

import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.NodeManagerUaNode;

import de.dfki.omm.acl.OMSCredentials;
import de.dfki.omm.impl.OMMBlockImpl;
import de.dfki.omm.impl.rest.OMMRestImpl;
import de.dfki.omm.tools.OMMActionResultType;
import de.dfki.omm.types.GenericTypedValue;
import de.dfki.omm.types.ISO8601;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMEntityCollection;
import de.dfki.omm.types.OMMFormat;
import de.dfki.omm.types.OMMMultiLangText;
import de.dfki.omm.types.OMMRestAccessMode;
import de.dfki.omm.types.OMMSubjectCollection;
import de.dfki.omm.types.TypedValue;
import de.dfki.omm.types.URLType;
//import de.dfki.oms.security.acl.OMMUsernamePasswordCredentials;

/**
 * A method to create a new block in an OMM. <br>
 * <br>
 * Arguments to hand to this method: <br>
 * - namespace ({@link String}) <br>
 * - format, consisting of format, schema and encoding ({@link String}[]) <br>
 * - creator, consisting of type (e.g email) and creator entity ({@link String}[]) <br>
 * - title, consisting of pairs of locale and title ({@link String}[]) <br>
 * - description, consisting of pairs of locale and title ({@link String}[]) <br>
 * - type ({@link String}) <br>
 * - subject, as RDF or OWL structure ({@link String}) <br>
 * - payload ({@link String}) <br>
 * - link, consisting of the type of the link (e.g. url) and the link itself ({@link String}[]) <br>
 * 
 * @author xekl01
 *
 */
public class OmsMethodCreateBlock extends OmsMethod {

	private String memoryURL;

	/**
	 * Basic Constructor.
	 * 
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodCreateBlock (NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
	}
	
	/**
	 * Constructor with URL to the OMS. 
	 * 
	 * @param memoryURL		URL to the OMS
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodCreateBlock (String memoryURL, NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
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
		
		System.out.println("creating new block");
		
		// create omm
		OMSCredentials creds = null;
//		if (authUser != null && authPw != null) creds = new OMMUsernamePasswordCredentials(authUser, authUser, authPw);
		OMMRestImpl omm = new OMMRestImpl(memoryURL, OMMRestAccessMode.CompleteDownloadUnlimited, creds);

		// gather IDs from OMM
		String blockId;
		List<String> blocks = omm.getAllBlockIDs();
		if (blocks != null) blockId = String.valueOf(omm.getAllBlockIDs().size() + 1);
		else blockId = "1";
		TypedValue memoryId = null;
		try {
			memoryId = new URLType(new URL(memoryURL));
		} catch (MalformedURLException e) {
			System.err.println("Not a valid URL: "+memoryURL);
			e.printStackTrace();
		}

		// namespace (example: "urn:sample")
		String namespaceInput = (String) inputArguments[0].getValue();
		URI namespace = URI.create(namespaceInput);
		
		// format: the format itself, its schema and encoding
		String[] formatInput = (String[]) inputArguments[1].getValue();
		OMMFormat format = null;
		if (formatInput[1] == null) 
			format = new OMMFormat(formatInput[0], null, formatInput[2]);
		else {
			try {
				format = new OMMFormat(formatInput[0], new URL(formatInput[1]), formatInput[2]); // format, schema, encoding
			} catch (MalformedURLException e) {
				System.err.println("Not a valid URL: "+formatInput[1]);
				e.printStackTrace();
			}
		}

		// creator: type ("email"), content ("a@b.c"), date ("1963-11-23T17:16:20+02:00")
		OMMEntity creator = null; 
		String[] creatorInput = (String[]) inputArguments[2].getValue();
		String creatorType = creatorInput[0];
		String creatorValue = creatorInput[1];
		String creationTime =  ISO8601.getISO8601String(Calendar.getInstance().getTime());
		if (creatorType != null && creatorValue != null && !creatorValue.equals("")) creator = new OMMEntity(creatorType, creatorValue, creationTime);
//		else if (authUser != null) creator = new OMMEntity("OpcUaUser", authUser, creationTime);
		
		// contributors
		OMMEntityCollection contributors = new OMMEntityCollection();
		if (creator != null) contributors.add(creator);
		
		// title: localized title strings
		String[] titleInput = (String[]) inputArguments[3].getValue();
		OMMMultiLangText title = null;
		if (titleInput.length % 2 == 0) {
			title = new OMMMultiLangText(); 
			int index = 0;
			while (index < titleInput.length) {
				// TODO parse locale, too, if any of this makes any sense anyway
				title.put(Locale.ENGLISH, titleInput[index+1]);
				index += 2;
			}
		}
		else {
			System.err.println("Invalid argument \"Title\". Needs to consist of language/title pairs.");
			return false;
		}

		// description: localized description strings
		String[] descriptionInput = (String[]) inputArguments[4].getValue();
		OMMMultiLangText description = null;
		if (descriptionInput.length % 2 == 0) {
			description = new OMMMultiLangText(); 
			int index = 0;
			while (index < descriptionInput.length) {
				// TODO parse locale, too, if any of this makes any sense anyway
				description.put(Locale.ENGLISH, descriptionInput[index+1]);
				index += 2;
			}
		}
		else {
			System.err.println("Invalid argument \"Description\". Needs to consist of language/description pairs.");
			return false;
		}

		// type (example: "http://purl.org/dc/dcmitype/Text")
		String typeInput = (String) inputArguments[5].getValue();
		URL type = null;
		if (typeInput != null && !typeInput.equals(""))
			try {
				type = new URL(typeInput);
			} catch (MalformedURLException e) {
				System.err.println("Not a valid URL: "+typeInput);
				e.printStackTrace();
			}

		// subject: RDF or OWL structure
		// TODO create OMMSubjectCollection from input string
		String subjectInput = (String) inputArguments[6].getValue(); 
		OMMSubjectCollection subject = null;

		// payload: pair of encoding and content
		// TODO only text payload possible right now
		String payloadInput = (String) inputArguments[7].getValue();
		TypedValue payload = new GenericTypedValue("text/plain", payloadInput);
		
		// payloadElement
		// TODO huh?
		String payloadElementInput = "";
		Element payloadElement = null;

		// link: type of the link ("url") and link itself ("http://www.w3.org/2005/Incubator/omm/samples/p1/ext.xml")
		// TODO what to do with the link's type?
		String[] linkInput = (String[]) inputArguments[8].getValue();
		TypedValue link = null;
		if (linkInput[1] != null && !linkInput[1].equals(""))
			try {
				link = new URLType(new URL(linkInput[1]));
			} catch (MalformedURLException e) {
				System.err.println("Not a valid URL: "+linkInput[1]);
				e.printStackTrace();
			}
		String linkHash = linkInput[2]; // optional hash value of the linked data

		// create and add block
		OMMBlockImpl block = (OMMBlockImpl) OMMBlockImpl.create(blockId, memoryId, namespace, type, title, description, contributors, creator, format, subject, payload, payloadElement, link, linkHash);
		OMMActionResultType result = omm.addBlock(block, null);
		if (result.equals(OMMActionResultType.OK)) return true;
		else {
			System.err.println("Block could not be created. "+result.toString());
			throw new RuntimeException(result.toString());
//			return false;
		}
	}

}
