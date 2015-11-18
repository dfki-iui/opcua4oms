package de.dfki.opcua.client;


/**
 * Starts an OmsOpcUaClient to access a suitable OPC UA server which models a running OMS.
 * 
 * @author xekl01
 *
 */
public class ClientStarter {

	// client arguments (null for default / anonymous)
	private static String serverAddress = null;
	private static String userName = null;
	private static String userPw = null;
	
	/**
	 * Initializes and runs an OmsOpcUaClient.
	 * 
	 * @param args Command line arguments for the application
	 */
	public static void main (String[] args) {
		
		// Handle logging (optionally)
		// Handle application arguments (optionally)
		
		// create and start client
		OmsOpcUaClient omsOpcUaClient = null;
		if (serverAddress != null)
			if (userName != null && userPw != null) omsOpcUaClient = new OmsOpcUaClient(serverAddress, userName, userPw);
			else omsOpcUaClient = new OmsOpcUaClient(serverAddress);
		else 
			if (userName != null && userPw != null) omsOpcUaClient = new OmsOpcUaClient(userName, userPw);
			else omsOpcUaClient = new OmsOpcUaClient(); 
		omsOpcUaClient.run();

		// after client termination
		System.out.println("**** ******* OMS OPC UA Client closed.");
	}
}
