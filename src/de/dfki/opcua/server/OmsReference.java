package de.dfki.opcua.server;

import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.common.ServiceResultException;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaReferenceType;
import com.prosysopc.ua.server.NodeManager;

/**
 * Describes References between nodes
 * which consist of a source, a target and a reference type.
 */
public class OmsReference extends UaReference {

	private final NodeId referenceTypeId;
	private final ExpandedNodeId sourceId;
	private final ExpandedNodeId targetId;
	private NodeManager nodeManager;

	/**
	 * @param sourceId
	 * @param targetId
	 * @param referenceType
	 */
	public OmsReference(ExpandedNodeId sourceId, ExpandedNodeId targetId, NodeId referenceType, NodeManager nodeManager) {
		super();
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.referenceTypeId = referenceType;
		this.nodeManager = nodeManager;
	}

	/**
	 * @param sourceId
	 * @param targetId
	 * @param referenceType
	 */
	public OmsReference(NodeId sourceId, NodeId targetId, NodeId referenceType, NodeManager nodeManager) {
		this(nodeManager.getNamespaceTable().toExpandedNodeId(sourceId),
				nodeManager.getNamespaceTable().toExpandedNodeId(targetId),
				referenceType, nodeManager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.prosysopc.ua.nodes.UaReference#delete()
	 */
	@Override
	public void delete() {
		throw new RuntimeException("StatusCodes.Bad_NotImplemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.prosysopc.ua.nodes.UaReference#getIsInverse(org.opcfoundation
	 * .ua.builtintypes.NodeId)
	 */
	@Override
	public boolean getIsInverse(NodeId nodeId) {
		try {
			if (nodeId.equals(nodeManager.getNamespaceTable().toNodeId(sourceId)))
				return false;
			if (nodeId.equals(nodeManager.getNamespaceTable().toNodeId(targetId)))
				return true;
		} catch (ServiceResultException e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException("not a source nor target");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.prosysopc.ua.nodes.UaReference#getIsInverse(com.prosysopc.ua.
	 * nodes.UaNode)
	 */
	@Override
	public boolean getIsInverse(UaNode node) {
		return getIsInverse(node.getNodeId());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.prosysopc.ua.nodes.UaReference#getReferenceType()
	 */
	@Override
	public UaReferenceType getReferenceType() {
		try {
			return (UaReferenceType) nodeManager.getNodeManagerTable().getNode(getReferenceTypeId());
		} catch (StatusException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.prosysopc.ua.nodes.UaReference#getReferenceTypeId()
	 */
	@Override
	public NodeId getReferenceTypeId() {
		return referenceTypeId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.prosysopc.ua.nodes.UaReference#getSourceId()
	 */
	@Override
	public ExpandedNodeId getSourceId() {
		return sourceId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.prosysopc.ua.nodes.UaReference#getSourceNode()
	 */
	@Override
	public UaNode getSourceNode() {
		return null; // new UaExternalNodeImpl(myNodeManager, sourceId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.prosysopc.ua.nodes.UaReference#getTargetId()
	 */
	@Override
	public ExpandedNodeId getTargetId() {
		return targetId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.prosysopc.ua.nodes.UaReference#getTargetNode()
	 */
	@Override
	public UaNode getTargetNode() {
		return null; // new UaExternalNodeImpl(myNodeManager, targetId);
	}

}
