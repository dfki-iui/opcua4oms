package de.dfki.opcua.server.method;

import java.util.Locale;

import org.opcfoundation.ua.builtintypes.NodeId;

import com.prosysopc.ua.server.NodeManagerUaNode;

import de.dfki.omm.impl.rest.OMMRestImpl;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.tools.OMMActionResultType;
import de.dfki.omm.types.OMMEntity;

/**
 * A method to delete a given block from an OMM. 
 * 
 * @author xekl01
 *
 */
public class OmsMethodDeleteBlock extends OmsMethod {

	private OMMRestImpl omm;
	private OMMBlock block;

	/**
	 * Basic Constructor.
	 * 
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodDeleteBlock (NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
	}
	
	/**
	 * Constructor with OMM and Block to be deleted. 
	 * 
	 * @param omm			The OMM containing the block
	 * @param block			The block to be deleted
	 * @param parentNode	The node this method is a child of
	 * @param methodNodeId	This method's node id
	 * @param methodName	This method's display name for the OPC UA server
	 * @param locale		Locale of the display name
	 */
	public OmsMethodDeleteBlock (OMMRestImpl omm, OMMBlock block, NodeManagerUaNode parentNode, NodeId methodNodeId, String methodName, Locale locale) {
		super(parentNode, methodNodeId, methodName, locale);
		this.omm = omm; 
		this.block = block;
	}
	
	@Override
	public boolean execute () {

		OMMActionResultType result = omm.removeBlock(block, OMMEntity.getDummyEntity());
		if (result == OMMActionResultType.OK) return true;
		else {
			System.err.println("Block \""+block.getID()+"\" could not be deleted. "+result.toString());
			throw new RuntimeException(result.toString());
//			return false;
		}
	}
	
}
