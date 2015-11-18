package de.dfki.opcua.server;

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
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.utils.NumericRange;

import com.prosysopc.ua.EventNotifierClass;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.ValueRanks;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.IoManager;
import com.prosysopc.ua.server.NodeManager;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.UaServer;

import de.dfki.omm.impl.rest.OMMRestImpl;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.types.GenericTypedValue;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMMultiLangText;
import de.dfki.omm.types.TypedValue;
//import de.dfki.oms.security.omm.OMMSecurityBlock;
import de.dfki.opcua.server.method.OmsMethodDeleteBlock;

/**
 * A NodeManager for the block level of an OMS. <br/>
 * (Multiple NodeManagers are necessary because of the need for different namespaces considering blocks and methods are 
 * called the same throughout the whole OMS representation.)
 * 
 * @author xekl01
 *
 */
public class NodeManagerBlock extends NodeManager {

	// OPC UA data
	private ExpandedNodeId memoryFolder;
	private ExpandedNodeId blockObject;
	private OmsMethodDeleteBlock deleteBlockMethod;
	private ExpandedNodeId deleteBlockMethodId;
	
	// OMS data
	private String blockId; 
	private OMMBlock block;
	
	/**
	 * Constructor.
	 * 
	 * @param server The server containing this NodeManager
	 * @param namespace The namespace for this NodeManager
	 * @param memoryFolder ID of the memory folder containing this block
	 * @param block The OMMBlock object modeled by this NodeManager
	 * @param omm The OMMRestImpl containing this block
	 */
	public NodeManagerBlock(UaServer server, String namespace, ExpandedNodeId memoryFolder, OMMBlock block, OMMRestImpl omm) {
		
		super(server, namespace);
		
		// initialize data
		this.memoryFolder = memoryFolder;
		this.block = block;
		blockId = block.getID();
//		if (!(block instanceof OMMSecurityBlock)) buildDeletionMethod(omm);
		buildDeletionMethod(omm);

		// add IOManager (to handle basic server requests)
		new IoManagerBlock(this);
	}

	/**
	 * Creates and adds a deletion method to this block.
	 *
	 * @param omm
	 * @throws StatusException 
	 */
	private void buildDeletionMethod (OMMRestImpl omm) {

		// new method
		NodeId deleteBlockId = new NodeId(getNamespaceIndex(), "Delete Block");
		deleteBlockMethod = new OmsMethodDeleteBlock(omm, block, getNodeManagerTable().getNodeManagerRoot(), deleteBlockId, "Delete Block", Locale.ENGLISH);

		// set method node
		deleteBlockMethodId = new ExpandedNodeId(deleteBlockId);
	}
	
	
	// reacts to method calls from clients
	@Override
	protected Variant[] callMethod (ServiceContext serviceContext, NodeId parentNode, NodeId callingNode, Variant[] variants, StatusCode[] statuses, DiagnosticInfo[] dInfos) {
		
		// TODO comments
		System.out.println("callMethod (block level)");
		System.out.println("-- serviceContext: "+serviceContext);
		System.out.println("-- parentNode: "+parentNode);
		System.out.println("-- callingNode: "+callingNode);
		System.out.print("-- variants: ");
		for (Variant v : variants) System.out.print(v.toStringWithType()); System.out.println("");
		System.out.print("-- statuses: ");
		for (StatusCode s : statuses) System.out.print(s.toString()); System.out.println("");
		System.out.print("-- dInfos: ");
		for (DiagnosticInfo d : dInfos) System.out.print(d.toString()); System.out.println("");
		
		try {
			if (callingNode.equals(getNamespaceTable().toNodeId(deleteBlockMethodId))) 
				deleteBlockMethod.execute();
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

		if (nodeId.getValue().toString().equals(blockId))
			return NodeClass.Object;
		
		if (isBlockVariable(nodeId.getValue().toString()))
			return NodeClass.Variable;
		
		if (nodeId.getValue().toString().equals("Delete Block"))
			return NodeClass.Method;

		return null;
	}


	@Override
	protected UaReference[] getReferences(NodeId nodeId, UaNode node) {

		// get references for the block object (basic references + all metadata items and methods)
		if (nodeId.getValue().toString().equals(blockId)) {

			UaReference[] references;
//			if (!(block instanceof OMMSecurityBlock)) references = new UaReference[14];
//			else references = new UaReference[13];
			references = new UaReference[14];

			// Inverse reference to the memory folder
			references[0] = new OmsReference(memoryFolder, blockObject, Identifiers.Organizes, this);
			// Type definition reference
			references[1] = new OmsReference(blockObject, new ExpandedNodeId(Identifiers.BaseObjectType), Identifiers.HasTypeDefinition, this);

			// Block variables
			// Contributors
			references[2] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Contributors"), Identifiers.HasComponent, this);
			// Creator
			references[3] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Creator"), Identifiers.HasComponent, this);
			// Description
			references[4] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Description"), Identifiers.HasComponent, this);
			// Format
			references[5] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Format"), Identifiers.HasComponent, this);
			// ID
			references[6] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "ID"), Identifiers.HasComponent, this);
			// Namespace
			references[7] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Namespace"), Identifiers.HasComponent, this);
			// Payload or Link
			if (block.isLinkBlock()) 
				references[8] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Link"), Identifiers.HasComponent, this);
			else 
				references[8] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Payload"), Identifiers.HasComponent, this);
			// PrimaryID
			references[9] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "PrimaryID"), Identifiers.HasComponent, this);
			// Subject
			references[10] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Subject"), Identifiers.HasComponent, this);
			// Title
			references[11] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Title"), Identifiers.HasComponent, this);
			// Type
			references[12] = new OmsReference(blockObject, new ExpandedNodeId(null, getNamespaceIndex(), "Type"), Identifiers.HasComponent, this);

			// Block deletion method
//			if (!(block instanceof OMMSecurityBlock))
//				references[13] = new OmsReference(blockObject, deleteBlockMethodId, Identifiers.HasComponent, this);
			references[13] = new OmsReference(blockObject, deleteBlockMethodId, Identifiers.HasComponent, this);

			return references;
		}
		
		// get references for all metadata items (only basic references)
		if (isBlockVariable(nodeId.getValue().toString())) {

			ExpandedNodeId variable = getNamespaceTable().toExpandedNodeId(nodeId);
			UaReference[] references = new UaReference[2];
			
			// Inverse reference to the block
			references[0] = new OmsReference(blockObject, variable, Identifiers.HasComponent, this);
			// Type definition reference
			references[1] = new OmsReference(variable, new ExpandedNodeId(Identifiers.VariableNode), Identifiers.HasTypeDefinition, this);

			return references;
		}
		
		// get references for deletion method
		if (nodeId.getValue().toString().equals("Delete Block")) {

			UaReference[] references = new UaReference[1];
			
			// Inverse reference to the OMS folder
			references[0] = new OmsReference(blockObject, deleteBlockMethodId, Identifiers.HasComponent, this);

			return references;
		}

		return null;
	}

	@Override
	protected ExpandedNodeId getTypeDefinition(ExpandedNodeId nodeId, UaNode node) {

		if (nodeId.getValue().toString().equals(blockId))
			return getNamespaceTable().toExpandedNodeId(Identifiers.ObjectNode);
		
		if (isBlockVariable(nodeId.getValue().toString()))
			return getNamespaceTable().toExpandedNodeId(Identifiers.VariableNode);
		
		if (nodeId.getValue().toString().equals("Delete Block"))
			return getNamespaceTable().toExpandedNodeId(Identifiers.MethodNode);
		
		return null;
	}

	@Override
	public NodeId getVariableDataType(NodeId nodeId, UaVariable arg1) throws StatusException {

		// return fitting datatypes for certain metadata items, String for remaining
		switch (nodeId.getValue().toString()) {
		case "Contributors":
			return Identifiers.Structure;
		case "Creator":
			return Identifiers.Structure;
		case "Description":
			return Identifiers.LocalizedText;
		case "Subject":
			return Identifiers.Structure;
		case "Title":
			return Identifiers.LocalizedText;
		default:
			return Identifiers.String;
		}
	}

	@Override
	public boolean hasNode(NodeId nodeId) {
		return (nodeId.getValue().toString().equals(blockId)) || isBlockVariable(nodeId.getValue().toString()) || (nodeId.getValue().toString().equals("Delete Block")) ;
	}

	/**
	 * Checks whether a node name refers to one of the block's metadata items (implemented as variable nodes).
	 * 
	 * @param nodeName The name of the node
	 * @return true if the node refers to one of the block's variables
	 */
	private boolean isBlockVariable(String nodeName) {

		return (nodeName.equals("Contributors")) ||
				(nodeName.equals("Creator")) ||
				(nodeName.equals("Description")) ||
				(nodeName.equals("Format")) ||
				(nodeName.equals("ID")) ||
				(nodeName.equals("Link")) ||
				(nodeName.equals("Namespace")) ||
				(nodeName.equals("Payload")) ||
				(nodeName.equals("PrimaryID")) ||
				(nodeName.equals("Subject")) ||
				(nodeName.equals("Title")) ||
				(nodeName.equals("Type")) ;
	}
	
	/**
	 * Sets the ExpandedNodeId of the block / block object in question. 
	 * (Cannot be done via constructor because this NodeManager's namespace is needed first.)
	 * 
	 * @param blockObject The block object that is handled by this NodeManager
	 */
	public void setBlockObject (ExpandedNodeId blockObject) {
		this.blockObject = blockObject;
	}
	

	/**
	 * An IO Manager which provides the values for the attributes of the nodes.
	 */
	public class IoManagerBlock extends IoManager {

		public IoManagerBlock(NodeManager nodeManager) {
			super(nodeManager);
		}
	
		
		/**
		 * Helper to get a node's description (descriptions based on http://www.w3.org/2005/Incubator/omm/XGR-omm-20111026/)
		 * 
		 * @param node The node's name
		 * @return A description for the node
		 */
		private String getNodeDescription(String node) {
			
			String description = "";
			
			switch (node) {
			case "Contributors":
				description = "Indicates the date of block contribution and the identity of the contributor.";
				break;
			case "Creator":
				description = "Indicates the date of block creation and the identity of the creator.";
				break;
			case "Description":
				description = "Provides a clear text, multi-language and human-readable description (long version of the title) for the block content.";
				break;
			case "Format":
				description = "Defines the format / encoding of the block payload, a MIME type is intended.";
				break;
			case "ID":
				description = "Memory unique identifier for this block.";
				break;
			case "Link":
				description = "If the block payload is not embedded directly into the XML structure, then a link can be provided to indicate a relation to an out-sourced block payload at any location.";
				break;
			case "Namespace":
				description = "The namespace declares the content and its encoding in a unique way. Well-defined OMM blocks are identified with their namespace.";
				break;
			case "Payload":
				description = "Content of the block.";
				break;
			case "PrimaryID":
				description = "Unique identifier for the object memory containing this block.";
				break;
			case "Subject":
				description = "Free text tags and ontology concepts (RDF or OWL) to describe the block payload.";
				break;
			case "Title":
				description = "A clear text, multi-language and human-readable short (< 255 characters) title for the block content.";
				break;
			case "Type":
				description = "Equals to Dublin Core \"Type\" (see http://purl.org/dc/elements/1.1/type).";
				break;
			default:
				description = "Memory Block with ID "+blockId;
			}
			
			return description;			
		}
		
		
		/**
		 * Tests whether a block's metadata item is writable or read-only.
		 * 
		 * @param nodeName Name of the node representing the metadata item
		 * @return true if item is read-only
		 */
		private boolean isReadOnly (String nodeName) {

			return nodeName.equals("Contributors") ||
					nodeName.equals("Creator") || 
					nodeName.equals("Format") || 
					nodeName.equals("ID") || 
					nodeName.equals("Namespace") || 
					nodeName.equals("PrimaryID") || 
					nodeName.equals("Subject")|| 
					nodeName.equals("Type");
		}
		
		// Read Node Attributes
		@Override
		protected void readNonValue(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node,
				UnsignedInteger attributeId, DataValue dataValue)
				throws StatusException {

			// prepare data
			Object value = null;
			UnsignedInteger status = StatusCodes.Bad_AttributeIdInvalid;
			ExpandedNodeId expandedNodeId = getNamespaceTable().toExpandedNodeId(nodeId);
			String nodeName = nodeId.getValue().toString();
			
			// set attribute values for any nodeId
			if (attributeId.equals(Attributes.NodeId))
				value = nodeId;
			else if (attributeId.equals(Attributes.BrowseName))
				value = getBrowseName(expandedNodeId, node);
			else if (attributeId.equals(Attributes.DisplayName))
				value = getDisplayName(expandedNodeId, node, null);
			else if (attributeId.equals(Attributes.Description)) 
				value = new LocalizedText(getNodeDescription(nodeName));	
			else if (attributeId.equals(Attributes.NodeClass))
				value = getNodeClass(expandedNodeId, node);
			else if (attributeId.equals(Attributes.WriteMask))
				value = UnsignedInteger.ZERO;
	
			// set attribute values for block objects
			else if (attributeId.equals(Attributes.EventNotifier))
				value = EventNotifierClass.getMask(EventNotifierClass.NONE);
			
			// set attribute values for variables
			else if (attributeId.equals(Attributes.DataType))
				value = Identifiers.String;
			else if (attributeId.equals(Attributes.ValueRank))
				value = ValueRanks.Scalar;
			else if (attributeId.equals(Attributes.ArrayDimensions))
				value = "null";
			else if (attributeId.equals(Attributes.AccessLevel))
				if (isReadOnly(nodeName)) {
					value = AccessLevel.getMask(AccessLevel.READONLY);
				}
				else value = AccessLevel.getMask(AccessLevel.READWRITE);
			else if (attributeId.equals(Attributes.UserAccessLevel))
				value = AccessLevel.getMask(AccessLevel.READWRITE);
			else if (attributeId.equals(Attributes.Historizing))
				value = false;
			else if (attributeId.equals(Attributes.MinimumSamplingInterval))
				value = "null";

			// deliver result
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
			
			if (nodeId.getValue().toString().equals("Contributors")) 
				value = OmsParser.parseContributorsAsString(block);
			else if (nodeId.getValue().toString().equals("Creator")) 
				value = OmsParser.parseCreator(block);
			else if (nodeId.getValue().toString().equals("Description")) 
				value = OmsParser.parseDescription(block);
			else if (nodeId.getValue().toString().equals("Format")) 
				value = OmsParser.parseFormat(block);
			else if (nodeId.getValue().toString().equals("ID")) 
				value = OmsParser.parseId(block);
			else if (nodeId.getValue().toString().equals("Link")) 
				value = OmsParser.parseLink(block);
			else if (nodeId.getValue().toString().equals("Namespace")) 
				value = OmsParser.parseNamespace(block);
			else if (nodeId.getValue().toString().equals("Payload")) 
				value = OmsParser.parsePayload(block);
			else if (nodeId.getValue().toString().equals("PrimaryID")) 
				value = OmsParser.parsePrimaryID(block);
			else if (nodeId.getValue().toString().equals("Subject")) 
				value = OmsParser.parseSubject(block);
			else if (nodeId.getValue().toString().equals("Title")) 
				value = OmsParser.parseTitle(block);
			else if (nodeId.getValue().toString().equals("Type")) 
				value = OmsParser.parseType(block);			

			if (value == null) dataValue.setStatusCode(status);
			else dataValue.setValue(new Variant(value));
			dataValue.setServerTimestamp(DateTime.currentTime());
			dataValue.setSourceTimestamp(OmsParser.getTimeOfLastChange(block)); // if last change is null this will be set to 01.01.1601 01:00:00.000
		}
		
		// Write Node Value (in certain cases)
		@Override
		protected boolean writeValue(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaVariable node, NumericRange range, DataValue dataValue) {

			// TODO use some sort of OPC UA entity (or user credentials)
			OMMEntity changer = OMMEntity.getDummyEntity();
			String nodeName = nodeId.getValue().toString();
			String newValue = dataValue.getValue().toString();
			
			switch (nodeName) {
				
			case "Description":
				OMMMultiLangText description = new OMMMultiLangText(); 
				description.put(Locale.getDefault(), newValue); // TODO how to choose locale? use UA method instead?
				block.setDescription(description, changer);
				// FIXME not fully implemented in OMMBlockRestImpl
				break;

			case "Link":
				TypedValue newLink = new GenericTypedValue("url", newValue);
				block.setLink(newLink, changer);
				break;
				
			case "Payload":
				TypedValue newPayload = new GenericTypedValue("text/plain", newValue);
				block.setPayload(newPayload, changer);
				break;

			case "Title":
				OMMMultiLangText title = new OMMMultiLangText(); 
				title.put(Locale.getDefault(), newValue); // TODO how to choose locale? use UA method instead?
				block.setTitle(title, changer);
				// FIXME not fully implemented in OMMBlockRestImpl
				break;
				
			default:
				return false;
			}
			
			return true;
		}
	}
	
}
