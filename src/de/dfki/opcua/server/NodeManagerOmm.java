package de.dfki.opcua.server;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.Argument;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.core.ViewDescription;
import org.opcfoundation.ua.utils.NumericRange;

import com.prosysopc.ua.EventNotifierClass;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.ValueRanks;
import com.prosysopc.ua.nodes.MethodArgumentException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.BrowseContinuationPoint;
import com.prosysopc.ua.server.IoManager;
import com.prosysopc.ua.server.NodeManager;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.ServerUserIdentity;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.UaServer;

import de.dfki.omm.impl.rest.OMMRestImpl;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.types.OMMRestAccessMode;
//import de.dfki.oms.security.acl.OMMUsernamePasswordCredentials;
import de.dfki.opcua.server.method.OmsMethodChangeACL;
import de.dfki.opcua.server.method.OmsMethodChangeOwner;
import de.dfki.opcua.server.method.OmsMethodCreateBlock;
import de.dfki.opcua.server.method.OmsMethodDeleteOMM;
import de.dfki.opcua.server.method.OmsMethodSearchBlock;


/**
 * A NodeManager for the OMM level of an OMS. <br/>
 * (Multiple NodeManagers are necessary because of the need for different namespaces considering blocks and methods are 
 * called the same throughout the whole OMS representation.)
 * 
 * @author xekl01
 *
 */
public class NodeManagerOmm extends NodeManager {

	// OPC UA data
	private UaServer server;
	private ExpandedNodeId omsFolder;
	private ExpandedNodeId memoryFolder;
	private NodeManagerUaNode petManager;
	
	// methods
	private ExpandedNodeId changeACLMethodId;
	private ExpandedNodeId changeACLMethodInputs;
	private OmsMethodChangeACL changeACLMethod;
	private ExpandedNodeId changeOwnerMethodId;
	private ExpandedNodeId changeOwnerMethodInputs;
	private OmsMethodChangeOwner changeOwnerMethod;
	private OmsMethodCreateBlock createBlockMethod;
	private ExpandedNodeId createBlockMethodId;
	private ExpandedNodeId createBlockMethodInputs;
	private ExpandedNodeId deleteOmmMethodId;
	private OmsMethodDeleteOMM deleteOmmMethod;
	private OmsMethodSearchBlock searchBlockMethod;
	private ExpandedNodeId searchBlockMethodId;
	private ExpandedNodeId searchBlockMethodInputs;

	// OMS data
	private String memoryName;
	private OMMRestImpl omm;
	private String memoryURL;
	
	/**
	 * Constructor.
	 * 
	 * @param server The server containing this NodeManager
	 * @param namespace The namespace for this NodeManager
	 * @param omsFolder ID of the OMS folder, containing this block
	 * @param memoryURL Full URL of the OMM modeled in this NodeManager 
	 * @param memoryName Name of the OMM modeled in this NodeManager 
	 */
	public NodeManagerOmm(UaServer server, String namespace, ExpandedNodeId omsFolder, String memoryURL, String memoryName) {

		super(server, namespace);

		// setup OMS information
		this.memoryName = memoryName;
		this.memoryURL = memoryURL;
		omm = new OMMRestImpl(memoryURL, OMMRestAccessMode.CompleteDownloadUnlimited, null);
		petManager = new NodeManagerUaNode(server, namespace+"/pet");

		// setup OPC UA information
		this.server = server;
		this.omsFolder = omsFolder;
		buildOmmDeletionMethod();
		buildChangeAclMethod();
		buildChangeOwnerMethod();
		buildCreateBlockMethod();
		buildSearchBlockMethod();
		memoryFolder = null;

		// add IOManager (to handle basic server requests)
		new IoManagerOmm(this);
	}
	
	// This method leeches the ServiceContext from a current request in order to set the OMM's credentials to the current user's
	@SuppressWarnings({ "rawtypes", "unchecked" }) // needed for signature to be equal withoug warnings
	@Override
	protected BrowseContinuationPoint browseNode (ServiceContext serviceContext, List arg1, NodeId arg2, int arg3, 
			BrowseDirection arg4, NodeId arg5, Boolean arg6, QualifiedName arg7, EnumSet arg8, EnumSet arg9, 
			ViewDescription arg10, int arg11) 
					throws ServiceException, StatusException {

		// handle authentication
//		String authUser = null;
//		String authPw = null;
//		ServerUserIdentity userIdentity = serviceContext.getSession().getUserIdentity();
//		if (userIdentity != null) {
//			authUser = userIdentity.getName();
//			authPw = userIdentity.getPassword();
//		}
//		if (authUser != null && authPw != null) {
//			omm.setCredentials(new OMMUsernamePasswordCredentials(authUser, authUser, authPw));
//		}

		// continue normal browsing
		return super.browseNode(serviceContext, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
	}
	  
	
	/**
	 * Creates a method for the OMM folder that lets you change its Access Control List.
	 */
	private void buildChangeAclMethod () {

		// new method
//		NodeId changeACLId = new NodeId(getNamespaceIndex(), "Change ACL");
//		changeACLMethod = new OmsMethodChangeACL(memoryURL+"/mgmt/acl", getNodeManagerTable().getNodeManagerRoot(), changeACLId, "Change ACL", Locale.ENGLISH);
		NodeId changeACLId = new NodeId(petManager.getNamespaceIndex(), "Change ACL");
		changeACLMethod = new OmsMethodChangeACL(memoryURL+"/mgmt/acl", petManager, changeACLId, "Change ACL", Locale.ENGLISH);

		// method arguments
		Argument[] inputs = new Argument[1];
		inputs[0] = new Argument();
		inputs[0].setName("New ACL Entry");
		inputs[0].setDataType(Identifiers.String);
		inputs[0].setValueRank(ValueRanks.Scalar);
		inputs[0].setArrayDimensions(null);
		inputs[0].setDescription(new LocalizedText("A new ACL entry as an XML String to add to the list. Overwrites old user rights if existent.",Locale.ENGLISH));
		changeACLMethod.setInputArguments(inputs);

		// set method node
		changeACLMethodId = new ExpandedNodeId(changeACLId);

		// add method to pet manager
		try {
//			FolderTypeNode dummy = petManager.createInstance(FolderTypeNode.class, "changeACLDummy", new NodeId(petManager.getNamespaceIndex(), "changeACLDummy"));
//			petManager.addNodeAndReference(dummy, changeACLMethod, Identifiers.HasComponent);
			petManager.addNode(changeACLMethod);
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a method for the OMM folder that lets you change its owner.
	 * @throws StatusException 
	 */
	private void buildChangeOwnerMethod () {
		
		// new method
//		NodeId changeOwnerId = new NodeId(getNamespaceIndex(), "Change Owner");
//		changeOwnerMethod = new OmsMethodChangeOwner(memoryURL+"/mgmt/owner", getNodeManagerTable().getNodeManagerRoot(), changeOwnerId, "Change Owner", Locale.ENGLISH);
		NodeId changeOwnerId = new NodeId(petManager.getNamespaceIndex(), "Change Owner");
		changeOwnerMethod = new OmsMethodChangeOwner(memoryURL+"/mgmt/owner", petManager, changeOwnerId, "Change Owner", Locale.ENGLISH);
		
		// method arguments
		Argument[] inputs = new Argument[3];
		inputs[0] = new Argument();
		inputs[0].setName("Owner's Name");
		inputs[0].setDataType(Identifiers.String);
		inputs[0].setValueRank(ValueRanks.Scalar);
		inputs[0].setArrayDimensions(null);
		inputs[0].setDescription(new LocalizedText("Name of the new owner as plain text (to be displayed in the owner node).",Locale.ENGLISH));
		inputs[1] = new Argument();
		inputs[1].setName("User Name");
		inputs[1].setDataType(Identifiers.String);
		inputs[1].setValueRank(ValueRanks.Scalar);
		inputs[1].setDescription(new LocalizedText("Memory owner's user name",Locale.ENGLISH));
		inputs[2] = new Argument();
		inputs[2].setName("Password");
		inputs[2].setDataType(Identifiers.String);
		inputs[2].setValueRank(ValueRanks.Scalar);
		inputs[2].setDescription(new LocalizedText("Memory owner's password",Locale.ENGLISH));
		changeOwnerMethod.setInputArguments(inputs);
		
		// set method node
		changeOwnerMethodId = new ExpandedNodeId(changeOwnerId);
		
		// add method to pet manager
		try {
//			FolderTypeNode dummy = petManager.createInstance(FolderTypeNode.class, "changeOwnerDummy", new NodeId(petManager.getNamespaceIndex(), "changeOwnerDummy"));
//			petManager.addNodeAndReference(dummy, changeOwnerMethod, Identifiers.HasComponent);
			petManager.addNode(changeOwnerMethod);
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a method for the OMM folder that lets you create blocks. 
	 */
	private void buildCreateBlockMethod () {

		// new method
//		NodeId createBlockId = new NodeId(getNamespaceIndex(), "Create new Block");
//		createBlockMethod = new OmsMethodCreateBlock(memoryURL, getNodeManagerTable().getNodeManagerRoot(), createBlockId, "Create new Block", Locale.ENGLISH);
		NodeId createBlockId = new NodeId(petManager.getNamespaceIndex(), "Create new Block");
		createBlockMethod = new OmsMethodCreateBlock(memoryURL, petManager, createBlockId, "Create new Block", Locale.ENGLISH);
		
		// method arguments
		UnsignedInteger[] oneDimArraySizeTwo = { new UnsignedInteger(2) }; // one array entry per array dimension
		UnsignedInteger[] oneDimArraySizeThree = { new UnsignedInteger(3) }; // one array entry per array dimension
		Argument[] inputs = new Argument[9];
		
		inputs[0] = new Argument();
		inputs[0].setName("Namespace");
		inputs[0].setDataType(Identifiers.String);
		inputs[0].setValueRank(ValueRanks.Scalar);
		inputs[0].setDescription(new LocalizedText("The namespace declares the content and its encoding in a unique way",Locale.ENGLISH));
		
		inputs[1] = new Argument();
		inputs[1].setName("Format");
		inputs[1].setDataType(Identifiers.String);
		inputs[1].setValueRank(ValueRanks.OneDimension);
		inputs[1].setArrayDimensions(oneDimArraySizeThree);
		inputs[1].setDescription(new LocalizedText("Defines the format / encoding of the block payload, a MIME type is intended",Locale.ENGLISH));
		
		inputs[2] = new Argument();
		inputs[2].setName("Creator");
		inputs[2].setDataType(Identifiers.String);
		inputs[2].setValueRank(ValueRanks.OneDimension);
		inputs[2].setArrayDimensions(oneDimArraySizeTwo);
		inputs[2].setDescription(new LocalizedText("Identity type and identity of the creator",Locale.ENGLISH));
		
		inputs[3] = new Argument();
		inputs[3].setName("Title");
		inputs[3].setDataType(Identifiers.String);
		inputs[3].setValueRank(ValueRanks.OneDimension);
		inputs[3].setArrayDimensions(null);
		inputs[3].setDescription(new LocalizedText("Pairs of the block's title and title language",Locale.ENGLISH));
		
		inputs[4] = new Argument();
		inputs[4].setName("Description");
		inputs[4].setDataType(Identifiers.String);
		inputs[4].setValueRank(ValueRanks.OneDimension);
		inputs[4].setArrayDimensions(null);
		inputs[4].setDescription(new LocalizedText("Pairs of the block's description and description language",Locale.ENGLISH));
		
		inputs[5] = new Argument();
		inputs[5].setName("Type");
		inputs[5].setDataType(Identifiers.String);
		inputs[5].setValueRank(ValueRanks.Scalar);
		inputs[5].setDescription(new LocalizedText("Additional information for Dublin Core aware systems. Equals to Dublin Core \"Type\"",Locale.ENGLISH));
		
		inputs[6] = new Argument();
		inputs[6].setName("Subject");
		inputs[6].setDataType(Identifiers.String);
		inputs[6].setValueRank(ValueRanks.Scalar);
		inputs[6].setDescription(new LocalizedText("Free text tags and ontology concepts (RDF or OWL) to describe the block payload.",Locale.ENGLISH));
		
		inputs[7] = new Argument();
		inputs[7].setName("Payload");
		inputs[7].setDataType(Identifiers.String);
		inputs[7].setValueRank(ValueRanks.Scalar);
		inputs[7].setDescription(new LocalizedText("Inline payload as CDATA (plain text or XML structures).",Locale.ENGLISH));
		
		inputs[8] = new Argument();
		inputs[8].setName("Link");
		inputs[8].setDataType(Identifiers.String);
		inputs[8].setValueRank(ValueRanks.OneDimension);
		inputs[8].setArrayDimensions(oneDimArraySizeThree);
		inputs[8].setDescription(new LocalizedText("If the block payload is not given, a link must be provided to an out-sourced block payload. Consists of link type, the link itself and a hash value.",Locale.ENGLISH));

		createBlockMethod.setInputArguments(inputs);

		// set method node
		createBlockMethodId = new ExpandedNodeId(createBlockId);
		
		// add method to pet manager
		try {
//			FolderTypeNode dummy = petManager.createInstance(FolderTypeNode.class, "createBlocDummy", new NodeId(petManager.getNamespaceIndex(), "createBlocDummy"));
//			petManager.addNodeAndReference(dummy, createBlockMethod, Identifiers.HasComponent);
			petManager.addNode(createBlockMethod);

		} catch (StatusException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a method for the OMM folder that lets you delete this OMM. 
	 */
	private void buildOmmDeletionMethod() {

		// new method (no arguments)
//		NodeId deleteOmmId = new NodeId(getNamespaceIndex(), "Delete OMM");		
//		deleteOmmMethod = new OmsMethodDeleteOMM(memoryURL, getNodeManagerTable().getNodeManagerRoot(), deleteOmmId, "Delete OMM", Locale.ENGLISH);
		NodeId deleteOmmId = new NodeId(petManager.getNamespaceIndex(), "Delete OMM");
		deleteOmmMethod = new OmsMethodDeleteOMM(memoryURL, petManager, deleteOmmId, "Delete OMM", Locale.ENGLISH);
		
		// set method node
		deleteOmmMethodId = new ExpandedNodeId(deleteOmmId);
		
		// add method to pet manager
		try {
//			FolderTypeNode dummy = petManager.createInstance(FolderTypeNode.class, "deleteOmmMethodDummy", new NodeId(petManager.getNamespaceIndex(), "deleteOmmMethodDummy"));
//			petManager.addNodeAndReference(dummy, deleteOmmMethod, Identifiers.HasComponent);
			petManager.addNode(deleteOmmMethod);
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a method for the OMM folder that lets you search for blocks with certain contents. 
	 */
	private void buildSearchBlockMethod () {

		// new method
//		NodeId searchBlockId = new NodeId(getNamespaceIndex(), "Search for Block");
//		searchBlockMethod = new OmsMethodSearchBlock(memoryURL, getNodeManagerTable().getNodeManagerRoot(), searchBlockId, "Search for Block", Locale.ENGLISH);
		NodeId searchBlockId = new NodeId(petManager.getNamespaceIndex(), "Search for Block");
		searchBlockMethod = new OmsMethodSearchBlock(memoryURL, petManager, searchBlockId, "Search for Block", Locale.ENGLISH);

		// method arguments
		Argument[] inputs = new Argument[9];
		
		inputs[0] = new Argument();
		inputs[0].setName("Namespace");
		inputs[0].setDataType(Identifiers.String);
		inputs[0].setValueRank(ValueRanks.Scalar);
		inputs[0].setDescription(new LocalizedText("Exact namespace to search for or parts of it", Locale.ENGLISH));
		
		inputs[1] = new Argument();
		inputs[1].setName("Format");
		inputs[1].setDataType(Identifiers.String);
		inputs[1].setValueRank(ValueRanks.Scalar);
		inputs[1].setDescription(new LocalizedText("Exact format/encoding of the block payload or parts of it",Locale.ENGLISH));
		
		inputs[2] = new Argument();
		inputs[2].setName("Creator");
		inputs[2].setDataType(Identifiers.String);
		inputs[2].setValueRank(ValueRanks.Scalar);
		inputs[2].setDescription(new LocalizedText("Exact name of the creator or parts of it",Locale.ENGLISH));
		
		inputs[3] = new Argument();
		inputs[3].setName("Title");
		inputs[3].setDataType(Identifiers.String);
		inputs[3].setValueRank(ValueRanks.Scalar);
		inputs[3].setArrayDimensions(null);
		inputs[3].setDescription(new LocalizedText("Exact title or parts of it (all languages are checked)",Locale.ENGLISH));
		
		inputs[4] = new Argument();
		inputs[4].setName("Description");
		inputs[4].setDataType(Identifiers.String);
		inputs[4].setValueRank(ValueRanks.Scalar);
		inputs[4].setArrayDimensions(null);
		inputs[4].setDescription(new LocalizedText("Exact description or parts of it (all languages are checked)",Locale.ENGLISH));
		
		inputs[5] = new Argument();
		inputs[5].setName("Type");
		inputs[5].setDataType(Identifiers.String);
		inputs[5].setValueRank(ValueRanks.Scalar);
		inputs[5].setDescription(new LocalizedText("Exact type to search for or parts of it",Locale.ENGLISH));
		
		inputs[6] = new Argument();
		inputs[6].setName("Subject");
		inputs[6].setDataType(Identifiers.String);
		inputs[6].setValueRank(ValueRanks.Scalar);
		inputs[6].setDescription(new LocalizedText("Exact subject or parts of subject tags",Locale.ENGLISH));
		
		inputs[7] = new Argument();
		inputs[7].setName("Payload");
		inputs[7].setDataType(Identifiers.String);
		inputs[7].setValueRank(ValueRanks.Scalar);
		inputs[7].setDescription(new LocalizedText("Exact payload or parts of it (only plain text)",Locale.ENGLISH));
		
		inputs[8] = new Argument();
		inputs[8].setName("Link");
		inputs[8].setDataType(Identifiers.String);
		inputs[8].setValueRank(ValueRanks.Scalar);
		inputs[8].setDescription(new LocalizedText("Exact link or parts of it",Locale.ENGLISH));

		searchBlockMethod.setInputArguments(inputs);

		Argument[] outputs = new Argument[1];
		outputs[0] = new Argument();
		outputs[0].setName("Result");
		outputs[0].setDataType(Identifiers.String);
		outputs[0].setValueRank(ValueRanks.ScalarOrOneDimension); 
		outputs[0].setArrayDimensions(null);
		outputs[0].setDescription(new LocalizedText("Block ID for block(s) containing queried contents", Locale.ENGLISH));
		searchBlockMethod.setOutputArguments(outputs);
		
		// set method node
		searchBlockMethodId = new ExpandedNodeId(searchBlockId);
		
		// add method to pet manager
		try {
			petManager.addNode(searchBlockMethod);
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}
	
	// reacts to method calls from clients
	@Override
	protected Variant[] callMethod (ServiceContext serviceContext, NodeId parentNode, NodeId callingNode, Variant[] inputs, StatusCode[] inputArgumentResults, DiagnosticInfo[] dInfos) {
		
//		try {
//			System.out.println("callMethod");
//			System.out.println("called by: \n");
//			StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
//			for (StackTraceElement ste : stElements) System.out.println("\t"+ste.toString());
//			System.out.println("-- serviceContext: "+serviceContext);
//			System.out.println("---- session: "+serviceContext.getSession());
//			System.out.println("---- session name: "+serviceContext.getSession().getSessionName());
//			System.out.println("---- session authentication token: "+serviceContext.getSession().getAuthenticationToken());
//			System.out.println("---- session client identity: "+serviceContext.getSession().getClientIdentity());
//			System.out.println("---- session user identity: "+serviceContext.getSession().getUserIdentity());
//			System.out.println("---- session user identity name: "+serviceContext.getSession().getUserIdentity().getName());
//			System.out.println("---- session user identity password: "+serviceContext.getSession().getUserIdentity().getPassword());
//			System.out.println("-- parentNode: "+parentNode);
//			System.out.println("-- callingNode: "+callingNode);
//			System.out.print("-- variants: ");
//			for (Variant v : inputs) System.out.print(v.toStringWithType()); System.out.println("");
////			System.out.print("-- statuses: ");
////			if (statuses != null) for (StatusCode s : statuses) System.out.print(s.toString()); System.out.println("");
////			System.out.print("-- dInfos: ");
////			for (DiagnosticInfo d : dInfos) System.out.print(d.toString()); System.out.println("");
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
		
		try {
			if (callingNode.equals(getNamespaceTable().toNodeId(deleteOmmMethodId))) {
				deleteOmmMethod.initialize(inputs, inputArgumentResults, dInfos, serviceContext.getSession().getUserIdentity());
				deleteOmmMethod.execute();
				// TODO a client will have to rebrowse the OMS folder after calling this, making sure the changes show
			}
			else if (callingNode.equals(getNamespaceTable().toNodeId(changeACLMethodId))) {
				changeACLMethod.initialize(inputs, inputArgumentResults, dInfos, serviceContext.getSession().getUserIdentity());
				changeACLMethod.execute();
			}
			else if (callingNode.equals(getNamespaceTable().toNodeId(changeOwnerMethodId))) {
				changeOwnerMethod.initialize(inputs, inputArgumentResults, dInfos, serviceContext.getSession().getUserIdentity());
				changeOwnerMethod.execute();
			}
			else if (callingNode.equals(getNamespaceTable().toNodeId(createBlockMethodId))) {
				createBlockMethod.initialize(inputs, inputArgumentResults, dInfos, serviceContext.getSession().getUserIdentity());
				createBlockMethod.execute();
				// TODO a client will have to rebrowse the memory folder after calling this, making sure the changes show
			}
			else if (callingNode.equals(getNamespaceTable().toNodeId(searchBlockMethodId))) {
				searchBlockMethod.initialize(inputs, inputArgumentResults, dInfos, serviceContext.getSession().getUserIdentity());
				return searchBlockMethod.executeWithOutput();
			}
		} catch (ServiceResultException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected QualifiedName getBrowseName(ExpandedNodeId nodeId, UaNode node) {
		return new QualifiedName(getNamespaceIndex(), nodeId.getValue().toString());
	}

	@Override
	protected LocalizedText getDisplayName(ExpandedNodeId nodeId, UaNode targetNode, Locale locale) {
		return new LocalizedText(nodeId.getValue().toString(), LocalizedText.NO_LOCALE);
	}

	@Override
	protected NodeClass getNodeClass(NodeId nodeId, UaNode node) {
		
		// if node is a method
		if (getNamespaceTable().nodeIdEquals(nodeId, deleteOmmMethodId) || 
				getNamespaceTable().nodeIdEquals(nodeId, changeACLMethodId) ||
				getNamespaceTable().nodeIdEquals(nodeId, changeOwnerMethodId) ||
				getNamespaceTable().nodeIdEquals(nodeId, createBlockMethodId) ||
				getNamespaceTable().nodeIdEquals(nodeId, searchBlockMethodId)) 
			return NodeClass.Method;
		
		// if node is a property
		if (nodeId.getValue().toString().equals("Owner") || nodeId.getValue().toString().endsWith("InputArguments"))
			return NodeClass.Variable;
		
		// all remaining nodes are objects
		return NodeClass.Object;
	}

	@Override
	protected UaReference[] getReferences(NodeId nodeId, UaNode node) {

		try {
			
			if (nodeId.getValue().toString().equals(memoryName)) {

				if (memoryFolder == null) memoryFolder = getNamespaceTable().toExpandedNodeId(nodeId);
				
				// number of references: number of blocks + 8 (see below)
				Collection<OMMBlock> memoryBlocks = omm.getAllBlocks();
				int i = 8;
				UaReference[] references;
				if (memoryBlocks != null) references = new UaReference[memoryBlocks.size() + i];
				else references = new UaReference[i];
				
				// Inverse reference to the OMS folder
				references[0] = new OmsReference(omsFolder, memoryFolder, Identifiers.Organizes, this);
				// Type definition reference
				references[1] = new OmsReference(memoryFolder, new ExpandedNodeId(Identifiers.FolderType), Identifiers.HasTypeDefinition, this);
				// Owner
				references[2] = new OmsReference(memoryFolder, new ExpandedNodeId(null, getNamespaceIndex(), "Owner"), Identifiers.HasProperty, this);
				// Change ACL method
				references[3] = new OmsReference(memoryFolder, changeACLMethodId, Identifiers.HasComponent, this);
				// Change Owner method
				references[4] = new OmsReference(memoryFolder, changeOwnerMethodId, Identifiers.HasComponent, this);		
				// Create Block method
				references[5] = new OmsReference(memoryFolder, createBlockMethodId, Identifiers.HasComponent, this);		
				// Search Block method
				references[6] = new OmsReference(memoryFolder, searchBlockMethodId, Identifiers.HasComponent, this);
				// OMM deletion method
				references[7] = new OmsReference(memoryFolder, deleteOmmMethodId, Identifiers.HasComponent, this);
				
				//  Block references
				if (memoryBlocks != null) {
					for (OMMBlock block : memoryBlocks) {
						String blockId = OmsParser.parseId(block);
						NodeManagerBlock blockNodeManager = new NodeManagerBlock(server, getNamespaceUri()+"/"+blockId, memoryFolder, block, omm);
						ExpandedNodeId blockObject = new ExpandedNodeId(null, blockNodeManager.getNamespaceIndex(), blockId);
						blockNodeManager.setBlockObject(blockObject);
						references[i] = new OmsReference(memoryFolder, blockObject, Identifiers.HasComponent, this);
						i++;
					}
				}

				return references;
			}
			
			if (nodeId.getValue().toString().equals("Owner")) {
				
				UaReference[] references = new UaReference[2]; 
				ExpandedNodeId thisNode = getNamespaceTable().toExpandedNodeId(nodeId);

				// Inverse reference to the creation method
				references[0] = new OmsReference(memoryFolder, thisNode, Identifiers.HasProperty, this);
				// Type definition reference
				references[1] = new OmsReference(thisNode, new ExpandedNodeId(Identifiers.PropertyType), Identifiers.HasTypeDefinition, this);

				return references;
			}
			
//			if (nodeId.equals(getNamespaceTable().toNodeId(deleteOmmMethodId))) {
//
//				UaReference[] references = new UaReference[1];
//				
//				// Inverse reference to the OMS folder
//				references[0] = new OmsReference(memoryFolder, deleteOmmMethodId, Identifiers.HasComponent, this);
//
//				return references;
//			}
			
//			if (nodeId.equals(getNamespaceTable().toNodeId(changeACLMethodId))) {
//
//				UaReference[] references = new UaReference[1];
//				
//				// Inverse reference to the OMS folder
//				references[0] = new OmsReference(memoryFolder, changeACLMethodId, Identifiers.HasComponent, this);
//				// Input reference
////				changeACLMethodInputs = new ExpandedNodeId(null, getNamespaceIndex(), "ChangeACL_InputArguments");
////				references[1] = new OmsReference(changeACLMethodId, changeACLMethodInputs, Identifiers.HasProperty, this);
//
//				return references;
//			}
//			if (nodeId.equals(getNamespaceTable().toNodeId(changeACLMethodInputs))) {
//				
//				UaReference[] references = new UaReference[2]; 
//				ExpandedNodeId thisNode = getNamespaceTable().toExpandedNodeId(nodeId);
//
//				// Inverse reference to the creation method
//				references[0] = new OmsReference(changeACLMethodId, thisNode, Identifiers.HasProperty, this);
//				// Type definition reference
//				references[1] = new OmsReference(thisNode, new ExpandedNodeId(Identifiers.PropertyType), Identifiers.HasTypeDefinition, this);
//
//				return references;
//			}
			
//			if (nodeId.equals(getNamespaceTable().toNodeId(changeOwnerMethodId))) {
//
//				UaReference[] references = new UaReference[2];
//				
//				// Inverse reference to the OMS folder
//				references[0] = new OmsReference(memoryFolder, changeOwnerMethodId, Identifiers.HasComponent, this);
//				// Input reference
//				changeOwnerMethodInputs = new ExpandedNodeId(null, getNamespaceIndex(), "ChangeOwner_InputArguments");
//				references[1] = new OmsReference(changeOwnerMethodId, changeOwnerMethodInputs, Identifiers.HasProperty, this);
//
//				return references;
//			}
//			if (nodeId.equals(getNamespaceTable().toNodeId(changeOwnerMethodInputs))) {
//				
//				UaReference[] references = new UaReference[2]; 
//				ExpandedNodeId thisNode = getNamespaceTable().toExpandedNodeId(nodeId);
//
//				// Inverse reference to the creation method
//				references[0] = new OmsReference(changeOwnerMethodId, thisNode, Identifiers.HasProperty, this);
//				// Type definition reference
//				references[1] = new OmsReference(thisNode, new ExpandedNodeId(Identifiers.PropertyType), Identifiers.HasTypeDefinition, this);
//
//				return references;
//			}
			
//			if (nodeId.equals(getNamespaceTable().toNodeId(createBlockMethodId))) {
//
//				UaReference[] references = new UaReference[2];
//				
//				// Inverse reference to the OMS folder
//				references[0] = new OmsReference(memoryFolder, createBlockMethodId, Identifiers.HasComponent, this);
//				// Input reference
//				createBlockMethodInputs = new ExpandedNodeId(null, getNamespaceIndex(), "CreateBlock_InputArguments");
//				references[1] = new OmsReference(createBlockMethodId, createBlockMethodInputs, Identifiers.HasProperty, this);
//
//				return references;
//			}
//			if (nodeId.equals(getNamespaceTable().toNodeId(createBlockMethodInputs))) {
//				
//				UaReference[] references = new UaReference[2]; 
//				ExpandedNodeId thisNode = getNamespaceTable().toExpandedNodeId(nodeId);
//
//				// Inverse reference to the creation method
//				references[0] = new OmsReference(createBlockMethodInputs, thisNode, Identifiers.HasProperty, this);
//				// Type definition reference
//				references[1] = new OmsReference(thisNode, new ExpandedNodeId(Identifiers.PropertyType), Identifiers.HasTypeDefinition, this);
//
//				return references;
//			}
			
//			if (nodeId.equals(getNamespaceTable().toNodeId(searchBlockMethodId))) {
//
//				UaReference[] references = new UaReference[2];
//				
//				// Inverse reference to the OMS folder
//				references[0] = new OmsReference(memoryFolder, searchBlockMethodId, Identifiers.HasComponent, this);
//				// Input reference
//				searchBlockMethodInputs = new ExpandedNodeId(null, getNamespaceIndex(), "SearchBlock_InputArguments");
//				references[1] = new OmsReference(searchBlockMethodId, searchBlockMethodInputs, Identifiers.HasProperty, this);
//
//				return references;
//			}
//			if (nodeId.equals(getNamespaceTable().toNodeId(searchBlockMethodInputs))) {
//				
//				UaReference[] references = new UaReference[2]; 
//				ExpandedNodeId thisNode = getNamespaceTable().toExpandedNodeId(nodeId);
//
//				// Inverse reference to the creation method
//				references[0] = new OmsReference(searchBlockMethodInputs, thisNode, Identifiers.HasProperty, this);
//				// Type definition reference
//				references[1] = new OmsReference(thisNode, new ExpandedNodeId(Identifiers.PropertyType), Identifiers.HasTypeDefinition, this);
//
//				return references;
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected ExpandedNodeId getTypeDefinition(ExpandedNodeId nodeId, UaNode node) {

		// if node is memory folder
		if (nodeId.getValue().toString().equals(memoryName)) 
			return getNamespaceTable().toExpandedNodeId(Identifiers.FolderType);
		
		// if node is a method
		if (getNamespaceTable().nodeIdEquals(nodeId, deleteOmmMethodId) || 
				getNamespaceTable().nodeIdEquals(nodeId, changeACLMethodId) ||
				getNamespaceTable().nodeIdEquals(nodeId, changeOwnerMethodId) ||
				getNamespaceTable().nodeIdEquals(nodeId, createBlockMethodId) ||
				getNamespaceTable().nodeIdEquals(nodeId, searchBlockMethodId))
			return getNamespaceTable().toExpandedNodeId(Identifiers.MethodNode);

		// if node is a property
		if (nodeId.getValue().toString().equals("Owner") || nodeId.getValue().toString().endsWith("InputArguments"))
			return getNamespaceTable().toExpandedNodeId(Identifiers.PropertyType);
		
		return null;
	}

	@Override
	public NodeId getVariableDataType(NodeId nodeId, UaVariable arg1) throws StatusException {
		return null; // no variables on this browse level
	}

	@Override
	public boolean hasNode(NodeId nodeId) {
		
		if (nodeId.getValue().equals("ChangeACL_InputArguments")) return false;
		
		return (nodeId.getValue().equals(memoryName) || 
				nodeId.getValue().equals("Delete OMM") || 
				nodeId.getValue().equals("Owner") || 
				nodeId.getValue().equals("Change ACL") ||
				nodeId.getValue().equals("Change Owner") || 
				nodeId.getValue().equals("Create new Block") || 
				nodeId.getValue().equals("Search for Block") || 
				nodeId.getValue().toString().endsWith("InputArguments"));
	}


	
	
	/**
	 * An IO Manager which provides the values for the attributes of the nodes.
	 */
	public class IoManagerOmm extends IoManager {

		public IoManagerOmm(NodeManager nodeManager) {
			super(nodeManager);
		}
	
		// Read Attributes
		@Override
		protected void readNonValue(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node,
				UnsignedInteger attributeId, DataValue dataValue)
				throws StatusException {

			Object value = null;
			UnsignedInteger status = StatusCodes.Bad_AttributeIdInvalid;
			ExpandedNodeId expandedNodeId = getNamespaceTable().toExpandedNodeId(nodeId);
			
			// Deliver attribute values for a nodeId (only folders on OMM level)
			if (attributeId.equals(Attributes.NodeId))
				value = nodeId;
			else if (attributeId.equals(Attributes.BrowseName))
				value = getBrowseName(expandedNodeId, node);
			else if (attributeId.equals(Attributes.DisplayName))
				value = getDisplayName(expandedNodeId, node, null);
			else if (attributeId.equals(Attributes.Description))
				try {
					if (nodeId.getValue().equals(memoryName)) value = new LocalizedText("Folder for OMM "+memoryName);
					else if (nodeId.getValue().equals("Delete OMM")) value = new LocalizedText("A Method to delete this OMM");
					else if (nodeId.getValue().equals("Owner")) value = new LocalizedText("The owner of this OMM");
					else if (nodeId.getValue().equals("Change ACL")) value = new LocalizedText("A Method to change the ACL of this OMM");
					else if (nodeId.getValue().equals("Change Owner")) value = new LocalizedText("A Method to change the Owner of this OMM");
					else if (nodeId.getValue().equals("Create new Block")) value = new LocalizedText("A Method to create a new block in this OMM");
					else if (nodeId.getValue().equals("Search for Block")) value = new LocalizedText("A Method to search for a block in this OMM by contents");
					else if (nodeId.equals(getNamespaceTable().toNodeId(changeACLMethodInputs))) value = new LocalizedText("Inputs for the 'Change ACL' Method");
					else if (nodeId.equals(getNamespaceTable().toNodeId(changeOwnerMethodInputs))) value = new LocalizedText("Inputs for the 'Change Owner' Method");
					else if (nodeId.equals(getNamespaceTable().toNodeId(createBlockMethodInputs))) value = new LocalizedText("Inputs for the 'Create Block' Method");
					else if (nodeId.equals(getNamespaceTable().toNodeId(searchBlockMethodInputs))) value = new LocalizedText("Inputs for the 'Search Block' Method");
					else value = new LocalizedText("No description available"); 
				} catch (ServiceResultException e) {
					e.printStackTrace();
				}
			else if (attributeId.equals(Attributes.NodeClass))
				value = getNodeClass(expandedNodeId, node);
			else if (attributeId.equals(Attributes.WriteMask))
				value = UnsignedInteger.ZERO;
			else if (attributeId.equals(Attributes.EventNotifier))
				value = EventNotifierClass.getMask(EventNotifierClass.NONE);
			
			// Deliver attribute values for memory properties
			else if (attributeId.equals(Attributes.DataType))
				if (nodeId.getValue().toString().endsWith("InputArguments")) value = Identifiers.Argument;
				else value = Identifiers.String;
			else if (attributeId.equals(Attributes.ValueRank))
				value = ValueRanks.OneDimension;
			else if (attributeId.equals(Attributes.ArrayDimensions))
				if (nodeId.getValue().toString().endsWith("InputArguments")) value = new UnsignedInteger[1];
				else value = "null";
			else if (attributeId.equals(Attributes.AccessLevel))
				value = AccessLevel.getMask(AccessLevel.READONLY);
			else if (attributeId.equals(Attributes.UserAccessLevel))
				value = AccessLevel.getMask(AccessLevel.READONLY);
			else if (attributeId.equals(Attributes.Historizing))
				value = false;
			else if (attributeId.equals(Attributes.MinimumSamplingInterval))
				value = "null";

			if (value == null) dataValue.setStatusCode(status);
			else dataValue.setValue(new Variant(value));
			dataValue.setServerTimestamp(DateTime.currentTime());
		}
		
		// Read Node Value (if it has one)
		@Override
		protected void readValue(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaVariable node,
				NumericRange indexRange, TimestampsToReturn timestampsToReturn, DateTime minTimestamp, DataValue dataValue)
				throws StatusException {
			
			Object value = null;
			UnsignedInteger status = StatusCodes.Bad_AttributeIdInvalid;
			
			if (nodeId.getValue().toString().equals("Owner")) 
				value = OmsParser.getOwner(memoryURL);
			else 
				try {
					if (nodeId.equals(getNamespaceTable().toNodeId(changeACLMethodInputs)))
						value = changeACLMethod.getInputArguments();
					else if (nodeId.equals(getNamespaceTable().toNodeId(changeOwnerMethodInputs)))
						value = changeOwnerMethod.getInputArguments();
					else if (nodeId.equals(getNamespaceTable().toNodeId(createBlockMethodInputs)))
						value = createBlockMethod.getInputArguments();
					else if (nodeId.equals(getNamespaceTable().toNodeId(searchBlockMethodInputs)))
						value = searchBlockMethod.getInputArguments();
				} catch (MethodArgumentException | ServiceResultException e) {
					e.printStackTrace();
				}

			if (value == null) dataValue.setStatusCode(status);
			else dataValue.setValue(new Variant(value));
			dataValue.setServerTimestamp(DateTime.currentTime());
		}
	}

}
