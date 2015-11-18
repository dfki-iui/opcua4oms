package de.dfki.opcua.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;

import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.UserTokenPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.UaAddress;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.server.Session;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.UaServerException;


/**
 * Models a a running OMS's contents and functionality as a OPC UA Server.
 * 
 * @author xekl01
 *
 */
public class OmsOpcUaServer {

	// OPC UA resources
	private UaServer uaServer;
	private String discoveryServerAddress = "opc.tcp://localhost:4840";
	
	// OMS resources
//	private String omsURL = "http://oms:10082";   // needed for implementation in linked docker containers
	private String omsURL = "http://localhost:10082";
	
	
	/**
	 * Constructor. Uses default OMS address (http://localhost:10082).
	 * 
	 * @param opcuaPort		Port number for this server (protocol opc.tcp://)
	 * @param httpsPort		Port number for this server (protocol https://)
	 * @param serverName	Name of this server
	 */
	public OmsOpcUaServer(int opcuaPort, int httpsPort, String serverName) {
		try {
			initialize(opcuaPort, httpsPort, serverName);
			new NodeManagerOms(uaServer, "DE/DFKI/OPCUA/OMS", omsURL);
		} catch (UaServerException | SecureIdentityException | IOException e) {
			e.printStackTrace();
			System.err.println("Server could not be created, please check settings.");
		}
	}
	
	/**
	 * Constructor. Uses custom OMS address.
	 * 
	 * @param opcuaPort		Port number for this server (protocol opc.tcp://)
	 * @param httpsPort		Port number for this server (protocol https://)
	 * @param serverName	Name of this server
	 * @param omsUrl		URL of the OMS to be modeled
	 */
	public OmsOpcUaServer(int opcuaPort, int httpsPort, String serverName, String omsUrl) {
		this(opcuaPort, httpsPort, serverName);
		this.omsURL = omsUrl;
	}

	
	/**
	 * Configures Application Identity, Server Endpoints and basic Security.
	 * 
	 * @param port 					(port for OPC TCP)
	 * @param httpsPort				(port for HTTPS)
	 * @param applicationName		(name of the server)
	 * @throws UaServerException 		if the server initialization parameters are invalid
	 * @throws SecureIdentityException 	if issuer certificate cannot be loaded
	 * @throws IOException  			if application identity cannot be set
	 */
	private void initialize (int port, int httpsPort, String applicationName) throws UaServerException, SecureIdentityException, IOException {

		// Create server
		uaServer = new UaServer();

		// 1) Application Identity
		// - create ApplicationDescription
		ApplicationDescription appDescription = new ApplicationDescription();
		appDescription.setApplicationName(new LocalizedText(applicationName,Locale.ENGLISH));
		appDescription.setApplicationUri("urn:localhost:OPCUA:"+applicationName); // ApplicationUri is a unique identifier for each running instance, ‘localhost’ will be replaced with the actual host name
		appDescription.setApplicationType(ApplicationType.Server);
		// - handle certification
		final PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator(); // PKI files keep track of trusted and rejected client certificates
		uaServer.setCertificateValidator(validator);
		File certificateFolder = new File(validator.getBaseDir(), "private");
		// - create and set identity
		ApplicationIdentity identity = ApplicationIdentity.loadOrCreateCertificate(appDescription, "DFKI", "privatepassword", certificateFolder, null, null, true);
		uaServer.setApplicationIdentity(identity);
		
		// 2) Server Endpoint
		uaServer.setPort(Protocol.OpcTcp, port);
		String hostName = InetAddress.getLocalHost().getHostName();
		KeyPair serverHTTPScertificate = ApplicationIdentity.loadOrCreateIssuerCertificate("OpcUaServerHttps", certificateFolder, "HTTPScertificatePassword", 3650, false);
		identity.setHttpsCertificate(ApplicationIdentity.loadOrCreateHttpsCertificate(appDescription, hostName, "HTTPScertificatePassword", serverHTTPScertificate, certificateFolder, true));
		
		// 3) Security settings
		// supported user Token policies
		uaServer.addUserTokenPolicy(UserTokenPolicy.ANONYMOUS); 
		uaServer.addUserTokenPolicy(UserTokenPolicy.SECURE_USERNAME_PASSWORD);
//		userValidator = new OmsUserValidator();
//		uaServer.setUserValidator(userValidator);
	
		// 4) Register on the local discovery server (if present)
		try {
			uaServer.setDiscoveryServerAddress(new UaAddress(discoveryServerAddress));
			// registration happens automatically in uaServer.init();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			System.err.println("DiscoveryURL is not valid");
		}
		
		// Finally init server, according to above settings
		uaServer.init();
	}
	

	/**
	 * Starts the server into an I/O loop until the exit command is entered, then shuts it down. 
	 */
	public void run() {

		// start 
		try {
			uaServer.start();
		} catch (UaServerException e) {
			e.printStackTrace();
			System.err.println("Server could not be started, please check settings.");
		}

//		// work until shutdown command
//		serverLoop();
//
//		// shutdown after shutdown command
//		uaServer.shutdown(0, new LocalizedText("OPC UA server closed by user", Locale.ENGLISH));
	}

	/**
	 * Starts an I/O loop for the server to repeat until it is shut down.
	 */
	private void serverLoop() {
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String s = null;
		System.out.println("**** ******* OMS OPC UA Server running");
		do {
			try {
				lookForClients();
				System.out.println("**** x to exit");
				s = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		while ((s == null) || (s.length() == 0) || (!s.equals("x")));
	
	}
	
	
	private void lookForClients() {
		
		Collection<Session> sessions = uaServer.getSessionManager().getSessions(); 
		if (sessions.size() > 0) {
			System.out.print("connected clients: ");
			for (Session s : sessions) System.out.println("- "+s.getSessionName()+" | user: "+s.getUserIdentity().getName()+", pw: "+s.getUserIdentity().getPassword()); 
			System.out.println("");
		}
		
	}
	
	
	
	
}
