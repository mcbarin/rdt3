import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

// ***************** COMP/ELEC416 - Project 2  **********************************
//	Student Network Simulator code.
//  You need to implement the functions as described in the project document.
//	StudentNetworkSimulator class inherits from the base class NetworkSimulator.
// ******************************************************************************

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(int entity, String dataSent)
     *       Passes "dataSent" up to layer 5 from "entity" [A or B]
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          create a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the value of the Packet's checksum field
     *      int getPayload()
     *          returns the Packet's payload
     *
     */
	
	public int calculateCheckum(String messageData, int seqnum, int acknum){
		int checksum;
		checksum = seqnum + acknum;
		if (messageData != null) {
			for(int i=0;i<messageData.length();i++){
				checksum += messageData.charAt(i);
			}
		}
		return checksum;
	}
	
	public boolean isCorrupted(Packet p){
		int checksum = calculateCheckum(p.getPayload(), p.getSeqnum(), p.getAcknum());
		return checksum == p.getChecksum();
	}
	
	// For Sender A
	Packet lastPacketSent;
	int stateA;
	
	
	// For Receiver B
	Packet lastACKSent;
	int stateB;
	
	
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor. Don't touch!!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   long seed)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
    	// First check the state of the sender.
    	if(stateA == 0 || stateA == 2){
    		int packetNumber = stateA/2; // 0 -> 0 , 2 -> 1
	    	int checksum = calculateCheckum(message.getData(),packetNumber,packetNumber);
	    	Packet p = new Packet(packetNumber, packetNumber, checksum, message.getData());
	    	lastPacketSent = p;
	    	
	    	// Send packet to the network and start timer in case of a packet lost.
	    	toLayer3(A, p);
	    	startTimer(A, 100.0);
	    	stateA++;
	    	System.out.println("Packet #" + ""+packetNumber +  " Sent.\n Data: " + message.getData());
    	}else{
    		// Waiting for ACK.
    		System.out.println("Waiting for ACK from receiver");
    	}
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side. "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
    	// Packet received from receiver.
    	if(stateA == 1){ // Waiting for ACK state
    		if(!isCorrupted(packet))
    			System.out.println("Packet Corrupted.");
    		else if(packet.getAcknum() == 0){ // That's what we've been waiting for.
    			stopTimer(A);
    			stateA = 2;
    			System.out.println("Got ACK #0");
    		}
    		else if(packet.getAcknum() == 1)
    			System.out.println("Got ACK #1. Waiting for ACK #0");
    		
    	}else if(stateA == 3){
    		if(!isCorrupted(packet))
    			System.out.println("Packet Corrupted.");
    		else if(packet.getAcknum() == 0)
    			System.out.println("Got ACK #0. Waiting for ACK #1");
    		else if(packet.getAcknum() == 1){
    			stopTimer(A);
    			stateA = 0;
    			System.out.println("Got ACK #1");
    		}
    	}else{
    		// We are not waiting for ACK.
    		System.out.println("We are not waiting for ACK.");
    	}
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
    	// Send the last packet again and start timer.
    	toLayer3(A,lastPacketSent);
    	startTimer(A, 100.0);
    	System.out.println("Timer interrupt for A: Packet sent again.");
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
    	stateA = 0;
    	
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
    	stateB=0;
    }
}

