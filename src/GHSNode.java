import jbotsim.Topology;
import jbotsim.Clock;
import jbotsim.Node;
import jbotsim.Message;
import jbotsim.Link;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

//Basic class for Nodes in the Graph
public class GHSNode extends Node{
	private
		int phase;											//Phase number
		int frag;											//Roots of the fragment ID
		GHSNode father; 									//father's node
		ArrayList<GHSNode> Sons = new ArrayList<GHSNode>(); //List of Sons, empty if leaves
		
		Link mcoe;											//MCOE edge, null if it doesn't exist
		Node mcoeSon;										//Keep track of the son that send the MCOE edge
		int mcoeReceived;
		
	public void onStart() {
		phase = 0;
		frag = this.getID();
		father = this;
		mcoe = null;
		mcoeReceived = 0;
	}
	
	public void onClock() {
		//Recover list of messages in the mailbox
		List<Message> messages= this.getMailbox();
		List<Node> neighbors = this.getNeighbors();
		List<Node> outsideFrag = this.notInFrag(neighbors);
		//If no messages are received
		if(messages.size()==0) {
			
		}
		//Several messages coming from sons
		else {
				for(int i=0; i<messages.size(); i++) {
					Message currentMessage = messages.get(i);
					List<String> messageString = this.readMessage(currentMessage); 
					Message frag = new Message("FRAG");
					//Messages are separated in a list of string
					//First string tells what type of messages is received
					if(messageString.get(0).equals("PULSE")) {
						this.phase++;
						//Instead of sending "FRAG" to every neighbor outside the fragment
						//Just send it to the one sharing the MCOE
						if(outsideFrag.size()>0) { 						//One neighbor in another fragment is found
							Node mcoeNode = this.getMCOE(outsideFrag);
							mcoe = this.getCommonLinkWith(mcoeNode);
							this.send(mcoeNode, frag);
						}
						else {											//No neighbors outside the fragment
							mcoe = null;
						}
					}
					// FRAG received, send Frag(u) to sender
					else if(messageString.get(0).equals("PULSE")) {
						Message fragID = new Message(this.frag);
						this.send(currentMessage.getSender(), fragID);
					}
					// MCOE received from a son
					else if(messageString.get(0).equals("MCOE")) {
						this.mcoeUpdate(messageString, currentMessage.getSender());
					}
				}
		}
	}
	
	public List<Node> notInFrag(List<Node> neighbors){
		List<Node> outsideFrag = new ArrayList<Node>();
		for(int i=0; i<neighbors.size(); i++) {
			if(this.isSon(neighbors.get(i))) {
				outsideFrag.add(neighbors.get(i));
			}
		}
		return outsideFrag;
	}
	
	
	
	
	public boolean isSon(Node neighbor) {
		for(int i=0; i>this.Sons.size(); i++) {
			if(this.Sons.get(i).getID()==neighbor.getID()) {
				return true;
			}
		}
		return false;
	}
	
	public Node getMCOE(List<Node> outsideFrag) {
		Node linkedNode = outsideFrag.get(0);
		Node minNode = outsideFrag.get(0);
		double minCost = this.getCommonLinkWith(linkedNode).getLength();
		for(int i=1; i<outsideFrag.size(); i++) {
			linkedNode = outsideFrag.get(i);
			if(this.getCommonLinkWith(linkedNode).getLength()<minCost) {
				minCost = this.getCommonLinkWith(linkedNode).getLength();
				minNode = linkedNode;
			}
			if(this.getCommonLinkWith(linkedNode).getLength()==minCost && linkedNode.getID()<minNode.getID()){
				minNode = linkedNode;
			}
		}
		return minNode;
	}
	
	//MCOE edge is memorized as a link, but send as two node's IDs and ID of the son sending the mcoe
	public Message mcoeMessage() {
		List<Node> mcoeNodes = this.mcoe.endpoints();
		String node1 = String.valueOf(mcoeNodes.get(0).getID());
		String node2 = String.valueOf(mcoeNodes.get(2).getID());
		String message = String.join(",", "MCOE", node1, node2);
		return new Message(message);
	}
	
	//Method to update the MCOE edge if MCOE edge send by a son cost less than the one in memory
	//MCOE message is NodeInFrag, NodeOutOfFrag, Son'sID
	public void mcoeUpdate(List<String> messageList, Node sender) {
		int nodeID1 = Integer.parseInt(messageList.get(1));
		int nodeID2 = Integer.parseInt(messageList.get(2));
		if(nodeID1!=nodeID2) {
			Topology tp = this.getTopology();
			Node firstNode = tp.findNodeById(nodeID1);
			Node secondNode = tp.findNodeById(nodeID2);
			if(mcoe==null) { //If no mcoe are recorded
				mcoe = tp.getLink(firstNode, secondNode);
				mcoeSon = sender;
			}
			else if(tp.getLink(firstNode, secondNode).getLength()<mcoe.getLength()) {
				mcoe = tp.getLink(firstNode, secondNode);
				mcoeSon = sender;
			}
			else if(tp.getLink(firstNode, secondNode).getLength()<mcoe.getLength() && nodeID1<mcoe.endpoint(0).getID()) {
				mcoe = tp.getLink(firstNode, secondNode);
				mcoeSon = sender;
			}
			else if(tp.getLink(firstNode, secondNode).getLength()<mcoe.getLength() && nodeID1==mcoe.endpoint(0).getID() && nodeID2==mcoe.endpoint(1).getID()) {
				mcoe = tp.getLink(firstNode, secondNode);
				mcoeSon = sender;
			}
		}
	}
	
	public Message newRoot() {
		List<Node> mcoeNodes = this.mcoe.endpoints();
		String node = String.valueOf(mcoeNodes.get(0).getID());
		String message = String.join(",", "ACK", node);
		return new Message(message);
	}

	public List<String> readMessage(Message message){
		String messageString = String.valueOf(message);
		List<String> messageList = new ArrayList<String>(Arrays.asList(messageString.split(",")));
		return messageList;
	}
}
