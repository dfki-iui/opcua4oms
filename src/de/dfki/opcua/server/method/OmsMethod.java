package de.dfki.opcua.server.method;

import java.util.Locale;

import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.Variant;

import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.ServerUserIdentity;
import com.prosysopc.ua.server.nodes.PlainMethod;

/**
 * An abstract superclass for all methods to handle OMS functions. 
 * 
 * @author xekl01
 *
 */
public abstract class OmsMethod extends PlainMethod {

	protected Variant[] inputArguments; 
	protected StatusCode[] inputArgumentResults; 
	protected DiagnosticInfo[] inputArgumentDiagnosticInfos;
	
	protected ServerUserIdentity userIdentity;
	
	/**
	 * Basic Constructor.
	 * 
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethod (NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
	}

	/**
	 * Initialize the method with its current arguments. 
	 * 
	 * @param inputArguments
	 * @param inputArgumentResults
	 * @param inputArgumentDiagnosticInfos
	 */
	public void initialize(Variant[] inputArguments, StatusCode[] inputArgumentResults, DiagnosticInfo[] inputArgumentDiagnosticInfos) {
		this.inputArguments = inputArguments;
		this.inputArgumentResults = inputArgumentResults;
		this.inputArgumentDiagnosticInfos = inputArgumentDiagnosticInfos;
	}
	
	/**
	 * Initialize the method with its current arguments and the identity of the user calling the method.
	 * 
	 * @param inputArguments
	 * @param inputArgumentResults
	 * @param inputArgumentDiagnosticInfos
	 * @param userIdentity 
	 */
	public void initialize(Variant[] inputArguments, StatusCode[] inputArgumentResults, DiagnosticInfo[] inputArgumentDiagnosticInfos, ServerUserIdentity userIdentity) {
		initialize(inputArguments, inputArgumentResults, inputArgumentDiagnosticInfos);
		this.userIdentity = userIdentity;
	}
	
	/**
	 * Execute the method's functions. 
	 * 
	 * @return true if execution was successful
	 */
	public abstract boolean execute();
	
}
