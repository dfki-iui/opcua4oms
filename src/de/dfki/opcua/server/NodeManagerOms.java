
package de.dfki.opcua.server;

import java.util.ArrayList;
import java.util.Locale;

import de.dfki.opcua.server.method.OmsMethodLoadRobotML;
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
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.utils.NumericRange;

import com.prosysopc.ua.EventNotifierClass;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.ValueRanks;
import com.prosysopc.ua.nodes.MethodArgumentException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.IoManager;
import com.prosysopc.ua.server.NodeManager;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.types.opcua.FolderType;

import de.dfki.opcua.server.method.OmsMethodCreateOMM;


/**
 * A NodeManager which does not use UaNode objects,
 * but connects to a running OMS for the data.
 * 
 * @author xekl01
 */
public class NodeManagerOms extends NodeManager {

	// OPC UA data
	private UaServer server;
	private ExpandedNodeId omsFolder;
	private NodeManagerUaNode petManager;
	private ExpandedNodeId createOmmMethodId;
	private OmsMethodCreateOMM createOmmMethod;
	private ExpandedNodeId loadRobotMLMethodId;
	private OmsMethodLoadRobotML loadRobotMLMethod;
	
	// OMS data
	private String omsURL;
	private ArrayList<String> memoryNames;
	
	/**
	 * Basic constructor. 
	 *
	 * @param server The server on which this node manager is running
	 * @param namespaceUri This node manager's namespace
	 */
	public NodeManagerOms (UaServer server, String namespaceUri, String omsURL) {
		
		super(server, namespaceUri);
		
		// setup OMS information
		this.omsURL = omsURL;
		
		// setup OPC UA information
		this.server = server; 
		petManager = new NodeManagerUaNode(server, namespaceUri+"/pet");
		omsFolder = new ExpandedNodeId(null, getNamespaceIndex(), "OMS");		
		buildOmmCreationMethod();
		buildRobotMLLoadMethod();

		// create OMS folder inside Objects
		try {
			FolderType objectsFolder = getNodeManagerTable().getNodeManagerRoot().getObjectsFolder(); 
			objectsFolder.addReference(getNamespaceTable().toNodeId(omsFolder), Identifiers.Organizes, false);
		} catch (ServiceResultException e) {
			System.err.println("Creation of OMS root failed.");
			e.printStackTrace();
		}
		
		// add IOManager (to handle basic server requests)
		new IoManagerOms(this);
	}

	/**
	 * Adds a method to the OMS folder that lets you create OMMs. 
	 */
	private void buildOmmCreationMethod () {
		
		// new method
		NodeId createOmmId = new NodeId(petManager.getNamespaceIndex(), "Create new OMM");
		createOmmMethod = new OmsMethodCreateOMM(omsURL, petManager, createOmmId, "Create new OMM", Locale.ENGLISH);

		// method arguments
		Argument[] inputs = new Argument[4];
		inputs[0] = new Argument();
		inputs[0].setName("Memory Name");
		inputs[0].setDataType(Identifiers.String);
		inputs[0].setValueRank(ValueRanks.Scalar);
		inputs[0].setArrayDimensions(null);
		inputs[0].setDescription(new LocalizedText("Name of the new OMM",Locale.ENGLISH));
		inputs[1] = new Argument();
		inputs[1].setName("Owner Name");
		inputs[1].setDataType(Identifiers.String);
		inputs[1].setValueRank(ValueRanks.Scalar);
		inputs[1].setDescription(new LocalizedText("Memory owner's cleartext name",Locale.ENGLISH));
		inputs[2] = new Argument();
		inputs[2].setName("User Name");
		inputs[2].setDataType(Identifiers.String);
		inputs[2].setValueRank(ValueRanks.Scalar);
		inputs[2].setDescription(new LocalizedText("Memory owner's user name",Locale.ENGLISH));
		inputs[3] = new Argument();
		inputs[3].setName("Password");
		inputs[3].setDataType(Identifiers.String);
		inputs[3].setValueRank(ValueRanks.Scalar);
		inputs[3].setDescription(new LocalizedText("Memory owner's password",Locale.ENGLISH));
		createOmmMethod.setInputArguments(inputs);

		// TODO also possible/useful: should header and or owner be secure?

		// set method node
		createOmmMethodId = new ExpandedNodeId(createOmmId);
		
		// add method to pet manager
		try {
			petManager.addNode(createOmmMethod);
//			petManager.addReference(getNamespaceTable().toNodeId(createOmmMethodId), omsFolder, Identifiers.HasComponent, true);
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds a method to the OMS folder that lets you create OMMs by entering RobotML text.
	 */
	private void buildRobotMLLoadMethod() {

		// new method
		NodeId loadRobotMLId = new NodeId(petManager.getNamespaceIndex(), "Load from RobotML");
		loadRobotMLMethod = new OmsMethodLoadRobotML(omsURL, petManager, loadRobotMLId, "Load from RobotML", Locale.ENGLISH);

		// method arguments
		Argument[] inputs = new Argument[1];
		inputs[0] = new Argument();
		inputs[0].setName("RobotML text");
		inputs[0].setDataType(Identifiers.String);
		inputs[0].setValueRank(ValueRanks.Scalar);
		inputs[0].setArrayDimensions(null);
		inputs[0].setDescription(new LocalizedText("Complete RobotML String or snippet", Locale.ENGLISH));
		loadRobotMLMethod.setInputArguments(inputs);

		// TODO also possible/useful: should header and or owner be secure?

		// set method node
		loadRobotMLMethodId = new ExpandedNodeId(loadRobotMLId);

		// add method to pet manager
		try {
			petManager.addNode(loadRobotMLMethod);
//			petManager.addReference(getNamespaceTable().toNodeId(createOmmMethodId), omsFolder, Identifiers.HasComponent, true);
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}


	// reacts to method calls from clients
	@Override
	protected Variant[] callMethod (ServiceContext serviceContext, NodeId node1, NodeId node2, Variant[] variants, StatusCode[] statuses, DiagnosticInfo[] dInfos) {

		try {
			if (node2.equals(getNamespaceTable().toNodeId(createOmmMethodId))) {
				createOmmMethod.initialize(variants, statuses, dInfos, serviceContext.getSession().getUserIdentity());
				createOmmMethod.execute();
				// TODO a client will have to rebrowse the OMS folder after calling this, making sure the changes show
			}
			else if (node2.equals(getNamespaceTable().toNodeId(loadRobotMLMethodId))) {
				loadRobotMLMethod.initialize(variants, statuses, dInfos, serviceContext.getSession().getUserIdentity());
				loadRobotMLMethod.execute();
			}
		} catch (ServiceResultException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	// returns browse name for clients to browse the address space
	@Override
	protected QualifiedName getBrowseName (ExpandedNodeId nodeId, UaNode node) {
		return new QualifiedName(getNamespaceIndex(), nodeId.getValue().toString());
	}

	// returns display name for clients to show to users
	@Override
	protected LocalizedText getDisplayName (ExpandedNodeId nodeId, UaNode targetNode, Locale locale) {
		return new LocalizedText(nodeId.getValue().toString(), LocalizedText.NO_LOCALE);
	}

	// returns node class for clients to request correct attributes
	@Override
	protected NodeClass getNodeClass (NodeId nodeId, UaNode node) {
		return NodeClass.Object; // there is only the "OMS" node on this level 
	}

	// returns an array of references for a requested node, thus creating the address space
	@Override
	protected UaReference[] getReferences (NodeId nodeId, UaNode node) {

		try {
			
			// References for OMS root
			if (nodeId.equals(getNamespaceTable().toNodeId(omsFolder))) {

				// number of references: number of known OMMs + 3 
				memoryNames = OmsParser.getOMSMemoryNamesList(omsURL);
				int i = 4;
				UaReference[] references = new UaReference[memoryNames.size() + i];
				
				// Inverse reference to the ObjectsFolder
				references[0] = new OmsReference(new ExpandedNodeId(Identifiers.ObjectsFolder), omsFolder, Identifiers.Organizes, this);
				// Type definition reference
				references[1] = new OmsReference(omsFolder, new ExpandedNodeId(Identifiers.FolderType), Identifiers.HasTypeDefinition, this);
				// OMM creation method
				references[2] = new OmsReference(omsFolder, createOmmMethodId, Identifiers.HasComponent, this);
				// RobotML load method
				references[3] = new OmsReference(omsFolder, loadRobotMLMethodId, Identifiers.HasComponent, this);

				// OMM references
				for (String memoryName : memoryNames) {
					NodeManagerOmm memoryNodeManager = new NodeManagerOmm(server, getNamespaceUri()+"/"+memoryName, omsFolder, omsURL+"/rest/"+memoryName, memoryName);
					references[i] = new OmsReference(omsFolder, new ExpandedNodeId(null, memoryNodeManager.getNamespaceIndex(), memoryName), Identifiers.Organizes, this);
					i++;
				}

				return references;
			}
			
		} catch (ServiceResultException e) {
			throw new RuntimeException(e);
		} 

		return null;
	}

	@Override
	protected ExpandedNodeId getTypeDefinition (ExpandedNodeId nodeId, UaNode node) {

		// if node is OMS folder
		if (getNamespaceTable().nodeIdEquals(nodeId, omsFolder))
			return getNamespaceTable().toExpandedNodeId(Identifiers.FolderType);

		return null;
	}

	// returns the data type of a variable node
	@Override
	public NodeId getVariableDataType(NodeId nodeId, UaVariable variable) throws StatusException {
		return null; // there are no variables on this level
	}
	
	// If the NodeManager does not know a node, a client can't request it
	@Override
	public boolean hasNode(NodeId nodeId) {
		return (nodeId.getValue().equals("OMS"));
	}
	
	

	
	/**
	 * An IO Manager which provides the values for the attributes of the nodes.
	 */
	public class IoManagerOms extends IoManager {

		public IoManagerOms (NodeManager nodeManager) {
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
			
			// Deliver attribute values for a nodeId
			if (attributeId.equals(Attributes.NodeId))
				value = nodeId;
			else if (attributeId.equals(Attributes.BrowseName))
				value = getBrowseName(expandedNodeId, node);
			else if (attributeId.equals(Attributes.DisplayName))
				value = getDisplayName(expandedNodeId, node, null);
			else if (attributeId.equals(Attributes.Description))
//				if (nodeId.getValue().equals("OMS")) value = new String("OPC UA folder in which the OMS contents reside");
//				else if (nodeId.getValue().equals("Create new OMM")) value = new String("A Method to create a new OMM");
//				else value = new String("Input arguments for creation method");
				if (nodeId.getValue().equals("OMS")) value = new LocalizedText("OPC UA folder in which the OMS contents reside");
				else if (nodeId.getValue().equals("Create new OMM")) value = new LocalizedText("A Method to create a new OMM");
				else value = new LocalizedText("Input arguments for creation method");
			else if (attributeId.equals(Attributes.NodeClass))
				value = getNodeClass(expandedNodeId, node);
			else if (attributeId.equals(Attributes.WriteMask))
				value = UnsignedInteger.ZERO;
			else if (attributeId.equals(Attributes.EventNotifier))
				value = EventNotifierClass.getMask(EventNotifierClass.NONE);
			
			// Deliver attribute values for method arguments
			else if (attributeId.equals(Attributes.DataType))
				value = Identifiers.Argument;
			else if (attributeId.equals(Attributes.ValueRank))
				value = ValueRanks.OneDimension;
			else if (attributeId.equals(Attributes.ArrayDimensions))
				value = new UnsignedInteger[1];
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
		
		// Read Value
		@Override
		protected void readValue(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaVariable node,
				NumericRange indexRange, TimestampsToReturn timestampsToReturn, DateTime minTimestamp, DataValue dataValue)
				throws StatusException {

			Object value = null;
			UnsignedInteger status = StatusCodes.Bad_AttributeIdInvalid;

//			if (nodeId.getValue().toString().equals("InputArguments")) {
//				try {
//					value = createOmmMethod.getInputArguments();
//				} catch (MethodArgumentException e) {
//					e.printStackTrace();
//				}
//			}

			if (value == null) dataValue.setStatusCode(status);
			else dataValue.setValue(new Variant(value));
			dataValue.setServerTimestamp(DateTime.currentTime());
			
		}
		
		
	}

}
