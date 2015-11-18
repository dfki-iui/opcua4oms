package de.dfki.opcua.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.UserTokenType;
import org.opcfoundation.ua.transport.security.SecurityMode;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.MethodCallStatusException;
import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.SessionActivationException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.UserIdentity;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.nodes.UaNode;


/**
 * A basic client for other applications to use. <br>
 * <br>
 * Grants access to a OPC UA server that models a running OMS, 
 * allowing user authentication, browsing of the memory nodes, 
 * changing payloads and metadata, and calling methods.
 * 
 * @author xekl01
 *
 */
public class OmsOpcUaClient {
	
	// OPC UA resources
	private static UaClient uaClient;	
	private static String applicationName = "OMS OPC UA Client";

	private String userName;
	private String userPw;
	
	private NodeId omsNode = null;
	private NodeId currentNode = null;
	private NodeId previousNode = null;
	
	BufferedReader inputReader = null;
	
	// browse actions
	protected static final int RETURN_TO_OMS = -1;
	protected static final int CALL_METHOD = -2;
	protected static final int READ_VALUE = -3;
	protected static final int WRITE_VALUE = -4;
	protected static final int STOP_BROWSING = -5;
	
	/**
	 * Constructor. Creates and configures a new client to connect to a server modelling an OMS in OPC UA,
	 * standard server address is used (opc.tcp://localhost:52521).
	 */
	public OmsOpcUaClient () {
		this("opc.tcp://localhost:52521");
	}
	
	/**
	 * Constructor. Creates and configures a new client to connect to a server modelling an OMS in OPC UA, 
	 * without using user credentials.
	 * 
	 * @param serverAddress Address of the server with which to connect
	 */
	public OmsOpcUaClient (String serverAddress) {
		this(serverAddress, null, null);
	}
	
	/**
	 * Constructor. Creates and configures a new client to connect to a server modelling an OMS in OPC UA,
	 * using the given user credentials and standard server address (opc.tcp://localhost:52521).
	 * 
	 * @param userName Name of the user requesting OMS information
	 * @param userPw Password of the user requesting OMS information
	 */
	public OmsOpcUaClient (String userName, String userPw) {
		this("opc.tcp://localhost:52521", userName, userPw);
	}
	
	/**
	 * Constructor. Creates and configures a new client to connect to a server modelling an OMS in OPC UA,
	 * using the given user credentials.
	 * 
	 * @param serverAddress Address of the server with which to connect
	 * @param userName Name of the user requesting OMS information
	 * @param userPw Password of the user requesting OMS information
	 */
	public OmsOpcUaClient (String serverAddress, String userName, String userPw) {
		this.userName = userName;
		this.userPw = userPw;
		try {
			// create new client with server address
			uaClient = new UaClient(serverAddress);
			// configure security settings
			initialize();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Moves the client to the OMS folder (located under Objects) and sets currentNode to the OMS root.
	 * 
	 * @throws ServiceException
	 * @throws StatusException
	 * @throws ServiceResultException 
	 */
	private void browseToOmsNode() throws ServiceException, StatusException, ServiceResultException {
		
		if (omsNode == null) {
			// browse root node
			List<ReferenceDescription> rootNodeReferences = uaClient.getAddressSpace().browse(Identifiers.RootFolder);
			for (ReferenceDescription rd : rootNodeReferences) {
				// browse Objects node
				if (rd.getDisplayName().getText().equals("Objects")) {
					List<ReferenceDescription> objectNodeReferences = uaClient.getAddressSpace().browse(uaClient.getNamespaceTable().toNodeId(rd.getNodeId()));
					for (ReferenceDescription rdo : objectNodeReferences) {
						// browse OMS node
						if (rdo.getDisplayName().getText().equals("OMS")) 
							omsNode = uaClient.getNamespaceTable().toNodeId(rdo.getNodeId());
					}
				}
			}
		}

		currentNode = omsNode;
	}

	
	/**
	 * Calls a method using the given arguments.
	 * 
	 * @param objectId The NodeId of the source node of the method (by HasComponent reference) 
	 * @param methodId The method's NodeId
	 * @param inputs The arguments to hand to the method
	 * @return The method's output (if there is any) or null
	 */
	public Variant[] callMethod(NodeId objectId, NodeId methodId, Variant[] inputs) {

		Variant[] outputs = null;

		try {
			outputs = uaClient.call(objectId, methodId, inputs);
		} catch (MethodCallStatusException | ServiceException e) {
			e.printStackTrace();
		}

		return outputs;
	}
	
	/**
	 * Configures client settings. 
	 */
	private void initialize () {
		
		if (uaClient != null) {

			// Application Description 
			ApplicationDescription appDescription = new ApplicationDescription();
			appDescription.setApplicationName(new LocalizedText(applicationName,Locale.ENGLISH));
			appDescription.setApplicationUri("urn:localhost:OPCUA:"+applicationName); // ApplicationUri is a unique identifier for each running instance, ‘localhost’ will be replaced with the actual host name
			appDescription.setProductUri("urn:localhost:OPCUA:"+applicationName); // ProductUri is used to identify the product and should be globally unique for all instances
			appDescription.setApplicationType(ApplicationType.Client);
	
			// Application Identity
			PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator();
			uaClient.setCertificateValidator(validator);
			File privatePath = new File(validator.getBaseDir(), "private");
			ApplicationIdentity identity = null;
			try {
				identity = ApplicationIdentity.loadOrCreateCertificate(appDescription, 
						/* Organization */ "DFKI",
						/* Private Key Password */ "opcua",
						/* Key File Path */ privatePath,
						/* CA certificate & private key */ null,
						/* Key Sizes for instance certificates to create */ null,
						/* Enable renewing the certificate */ true);
			} catch (SecureIdentityException | IOException e) {
				e.printStackTrace();
			} 
			uaClient.setApplicationIdentity(identity);
			
			// Security Mode
//			uaClient.setSecurityMode(SecurityMode.BASIC128RSA15_SIGN_ENCRYPT); // default
//			uaClient.setSecurityMode(SecurityMode.BASIC128RSA15_SIGN_ENCRYPT); // sign all communication messages, but leave them unencrypted
			uaClient.setSecurityMode(SecurityMode.NONE);	// disable security
			
			// User Identity
			try {
				if (userName != null && userPw != null) uaClient.setUserIdentity(new UserIdentity(userName, userPw));
				else uaClient.setUserIdentity(new UserIdentity()); // anonymous user identity
			} catch (SessionActivationException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Starts an I/O loop for the "browse" submenu in which nodes can be bowsed and accessed.
	 */
	private void menuBrowse() {

		if (inputReader == null) {
			System.err.println("Input Reader not found");
			return;
		}
		
		System.out.println("---- ------- ---- ------- ---- ------- ---- ------- ");

		String s = null;
		do {
			try {
				
				// show current node and subnodes
				UaNode node = uaClient.getAddressSpace().getNode(currentNode);
				System.out.println("Browsing node: "+node.getDisplayName().getText()+" ("+node.getDescription().getText()+")");
				List<ReferenceDescription> references, upReferences;
				try {
					references = uaClient.getAddressSpace().browse(currentNode);
					upReferences = uaClient.getAddressSpace().browseUp(currentNode);
				} catch (ServiceException e1) {
					references = new ArrayList<ReferenceDescription>();
					upReferences = new ArrayList<ReferenceDescription>();
				}
				for (int i = 1; i <= references.size(); i++)
					System.out.println(" "+i+ "] " + references.get(i-1).getDisplayName().getText());
				
				// show menu
				System.out.println("\n**** node number to browse (0 to go up)");
				System.out.println("**** c to call method (if node is a method node)");
				System.out.println("**** o to return to OMS root");
				System.out.println("**** r to read value of current node");
				System.out.println("**** w to write value of current node");
				System.out.println("**** x to stop browsing");
				
				// read input
				s = inputReader.readLine();
				
				// call submenu
				int command = parseBrowseCommand(s);
				switch(command) {
				
				case CALL_METHOD:
					Variant[] inputs = parseMethodInputs();
//					Variant[] outputList = callMethod(previousNode, currentNode, inputs);
					Variant[] outputList = callMethod(omsNode, currentNode, inputs);
					if (outputList != null) {
						System.out.println("-> Method output: ");
						for (Variant v : outputList) System.out.println(v.toString());
						System.out.println("");
					}
					else System.out.println("-> Method called (refresh to see changes)\n");
					break;
				
				case RETURN_TO_OMS:
					browseToOmsNode();
					break;
					
				case READ_VALUE:
					Variant output = readNodeValue(currentNode);
					if (output == null) System.out.println("-> Value of node cannot be read (node might not be a variable)\n");
					else System.out.println("-> Value of node "+currentNode.getValue()+": "+output+"\n");
					break;
					
				case WRITE_VALUE:
					System.out.println("-> Enter the value to write");
					String value = inputReader.readLine();
					if (writeNodeValue(currentNode, value)) System.out.println("-> New value of node "+currentNode.getValue()+": "+value+"\n");
					else System.out.println("-> Value of node "+currentNode.getValue()+" could not be changed to '"+value+"' (node might not be a variable)\n");
					break;
					
				case STOP_BROWSING:
					// nothing to do, loop quits because of "x"
					break;
					
				case 0: // input was 0 (browse up)
					if (!upReferences.isEmpty()) // if there is an up reference, use it
						currentNode = uaClient.getNamespaceTable().toNodeId(upReferences.get(0).getNodeId());
					else // else go back to previous (higher) node
						if (previousNode != null) currentNode = previousNode;
					break;
					
				default: // input was (node) number
					if (command > 0 && command <= references.size()) {
						if (!upReferences.isEmpty()) previousNode = currentNode;
						currentNode = uaClient.getNamespaceTable().toNodeId(references.get(command-1).getNodeId());
					}
					break;
				}

			} catch (IOException | StatusException | AddressSpaceException | ServiceException | ServiceResultException e) {
				e.printStackTrace();
			}
		}
		while ((s == null) || (s.length() == 0) || (!s.equals("x")));
		
		System.out.println("---- ------- ---- ------- ---- ------- ---- ------- ");
		
	}
	
	/**
	 * Starts an I/O loop for the "login/logout" submenu in which user credentials can be entered.
	 */
	private void menuLoginLogout() {
		
		if (inputReader == null) {
			System.err.println("Input Reader not found");
			return;
		}
		
		System.out.println("---- ------- ---- ------- ---- ------- ---- ------- ");

		// login if there is no current user
		if (uaClient.getUserIdentity() == null || uaClient.getUserIdentity().getType() == UserTokenType.Anonymous) {
			try {
				
				// gather login information
				System.out.println("Enter username:");
				String user = inputReader.readLine();
				System.out.println("Enter password:");
				String password = inputReader.readLine();
				
				// login user
				if (user != null && password != null)
					uaClient.setUserIdentity(new UserIdentity(user, password));
				
				System.out.println("Login succesful");
				
			} catch (IOException | SessionActivationException e) {
				e.printStackTrace();
			}
		}
		// logout if there is a current user
		else {
			try {
				uaClient.setUserIdentity(new UserIdentity());
				System.out.println("Logout succesful!");
			} catch (SessionActivationException e) {
				e.printStackTrace();
			}
		}

		System.out.println("---- ------- ---- ------- ---- ------- ---- ------- ");
	}
	

	/**
	 * Starts an I/O loop for the client to repeat until it is shut down. In this menu and its 
	 * submenus, printed to the console, commands for browsing and changing the OMS contents 
	 * can be entered, as well as methods called.<br>
	 * <br> 
	 * If this loop is not started the client can equally be used to browse and manipulate the 
	 * OMS address space, but it will not print a menu to the console. 
	 */
	private void menuMain() {

		if (inputReader == null) {
			System.err.println("Input Reader not found");
			return;
		}
		
		String s = null;
		do {
			try {
				// show menu
				System.out.println("**** b to browse the OMS");
				System.out.println("**** l to login/logout");
//				System.out.println("**** h for help");
				System.out.println("**** x to exit");
				// read input
				s = inputReader.readLine();
				// call submenu
				if (s != null && s.equals("b")) 
					menuBrowse();
				if (s != null && s.equals("l")) 
					menuLoginLogout();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		while ((s == null) || (s.length() == 0) || (!s.equals("x")));
	}
	


	/**
	 * Parses a command to use in the browse menu.
	 * 
	 * @param s The input String from which to parse a command
	 * @return Integer representation of the command
	 */
	private int parseBrowseCommand(String s) {

		int input = -100;
		
		try {
			// parse 0 or node number (if input is a number)
			input = Integer.parseInt(s);
		}
		catch (NumberFormatException e) {
			// parse other command
			if (s != null && s.equals("c")) 
				input = CALL_METHOD;
			else if (s != null && s.equals("o"))
				input = RETURN_TO_OMS;
			else if (s != null && s.equals("r")) 
				input = READ_VALUE;
			else if (s != null && s.equals("w")) 
				input = WRITE_VALUE;
			else if (s != null && s.equals("x")) 
				input = STOP_BROWSING;
		}

		return input;
	}
	
	
	/**
	 * Asks for and gathers all necessary inputs for a method.
	 * 
	 * @return Variant[] of method inputs (or null)
	 */
	private Variant[] parseMethodInputs() {

		Variant[] inputs = null;
		String method = currentNode.getValue().toString();
		
		try {
			switch (method) {
			
			case ("Create new OMM"):
				inputs = new Variant[4];
				System.out.println("-> Enter name of the new memory:");
				inputs[0] = new Variant(inputReader.readLine());
				System.out.println("-> Enter clear text name of the memory's owner:");
				inputs[1] = new Variant(inputReader.readLine());
				System.out.println("-> Enter user name of the memory's owner:");
				inputs[2] = new Variant(inputReader.readLine());
				System.out.println("-> Enter password of the memory's owner:");
				inputs[3] = new Variant(inputReader.readLine());
				break;
				
			case ("Change ACL"):
				inputs = new Variant[1];
				System.out.println("-> Enter new ACL entry as XML string:");
				inputs[0] = new Variant(inputReader.readLine());
				break;
			
			case ("Change Owner"):
				inputs = new Variant[3];
				System.out.println("-> Enter clear text name of the memory's new owner:");
				inputs[0] = new Variant(inputReader.readLine());
				System.out.println("-> Enter user name of the memory's new owner:");
				inputs[1] = new Variant(inputReader.readLine());
				System.out.println("-> Enter password of the memory's new owner:");
				inputs[2] = new Variant(inputReader.readLine());
				break;
			
			case ("Create new Block"):
				inputs = new Variant[9];
				inputs[0] = inputs[5] = inputs[6] = new Variant("");
				inputs[1] = inputs[2] = inputs[3] = inputs[4] = inputs[8] = new Variant(new String[4]);
				System.out.println("-> Enter payload of the new block:");
				inputs[7] = new Variant(inputReader.readLine());
				// nothing more to setup via console, too tedious
				break;
			
			case ("Search for Block"):
				inputs = new Variant[9];
				for (int i = 0; i < inputs.length; i++) inputs[i] = new Variant(""); 
				int field = 8; 
				System.out.println("-> Search where? (Default: payload) \n\t 1] namespace \n\t 2] format \n\t 3] creator \n\t 4] title \n\t 5] description \n\t 6] type \n\t 7] subject \n\t 8] payload \n\t 9] link");
				field = Integer.parseInt(inputReader.readLine());
				System.out.println("-> Search for what?");
				inputs[field-1] = new Variant(inputReader.readLine());
				break;
						
			// no inputs needed for other methods
			default:
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return inputs;
	}
	
	
	/**
	 * Reads the value of a given node (if node is a variable).
	 * 
	 * @param nodeId The NodeId of the node to read 
	 */
	public Variant readNodeValue (NodeId nodeId) {

		Variant result = null;

		try {
			DataValue value = uaClient.readValue(nodeId);
			result = value.getValue();
		} catch (ServiceException | StatusException e) {
//			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * Starts the client into an I/O loop until the exit command is entered, then shuts it down. 
	 */
	public void run() {
		
		// start and browse to OMS folder
		try {
			uaClient.connect();
			browseToOmsNode();
		} catch (ServiceException | StatusException | ServiceResultException e) {
			e.printStackTrace();
			System.err.println("Client could not be started, please check settings and make sure the server models a running OMS.");
			return;
		}
		
		// work until exit command is entered
		inputReader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("**** ******* OMS OPC UA Client running");
		menuMain();
		
		// disconnect after exit command
		try {
			inputReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		uaClient.disconnect();
	}
	
	
	/**
	 * Writes the value of a given node (if node is a variable).
	 * 
	 * @param nodeId The NodeId of the node to write
	 * @param value The new value
	 */
	public boolean writeNodeValue (NodeId nodeId, String value) {

		try {
			return uaClient.writeValue(nodeId, value);
		} catch (ServiceException | StatusException e) {
			e.printStackTrace();
		}
		
		return false;
	}

}
