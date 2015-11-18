package de.dfki.opcua.server;

import java.security.cert.CertificateException;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.server.UaServerException;

/**
 * Starts an OmsOpcUaServer that models a running OMS's contents and functionality as an OPC UA Server.
 * 
 * @author xekl01
 *
 */
public class ServerStarter {

	private static int opcuaPort = 52521; 
	private static int httpsPort = 52444; 
	private static String serverName = "OMS in OPC UA";
	
	/**
	 * Initializes and runs an OmsOpcUaServer. 
	 * 
	 * @param args 					Command line arguments for the application
	 * @throws StatusException 		if the server address space creation fails
	 * @throws UaServerException	if the server initialization parameters are invalid
	 * @throws CertificateException	if the application certificate or private key, cannot be
	 * 								loaded from the files due to certificate errors
	 */
	public static void main (String[] args) {
		
		// Handle logging (optionally)
		// Handle application arguments (optionally)
		
		OmsOpcUaServer omsOpcUaServer = new OmsOpcUaServer(opcuaPort, httpsPort, serverName);
		omsOpcUaServer.run();

		// after server termination
//		System.out.println("**** ******* OMS OPC UA Server closed.");
	}
	
}
