import java.nio.charset.StandardCharsets;

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
	
	public int checkSum(Message message){
		int checksum=0;
		byte[] bitsOfMessage = message.getData().getBytes(StandardCharsets.UTF_8);
		for(int i=0; i<bitsOfMessage.length;i++){
			if (bitsOfMessage[i] == 1){
				checksum++;
			}
		}
		return checksum;
	}
	
	public boolean checkCorruption(Packet p){
		byte[] bitsOfMessage = p.getPayload().getBytes(StandardCharsets.UTF_8);
		int calculatedChecksum = 0;
		for(int i=0;i<bitsOfMessage.length;i++){
			if(bitsOfMessage[i]==1){
				calculatedChecksum++;
			}
		}
		return calculatedChecksum == p.getChecksum();
	}
	
	// For Sender A
	int sequenceNumberA;
	int lastReceivedACK;
	int defaultACKForSender = 0;
	Packet lastPacketSent;
	
	// For Receiver B
	int lastSentACK;
	int sequenceNumberB;
	
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
    	// First create a packet for this message.
    	int checksum = checkSum(message);
    	Packet p = new Packet(sequenceNumberA, defaultACKForSender, checksum, message.getData());
    	lastPacketSent = p;
    	
    	// Send packet to the network and start timer in case of a packet lost.
    	toLayer3(A, p);
    	startTimer(A, TIMERINTERRUPT);
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side. "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
    	// Send the last packet again and start timer.
    	toLayer3(A,lastPacketSent);
    	startTimer(A, TIMERINTERRUPT);
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
    	sequenceNumberA = 0; // next packet number to send
    	lastReceivedACK = -1; // last ACK received from receiver
    	
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
    	sequenceNumberB = 0; // waiting for this packet
    	lastSentACK = -1; // Last successfully received packet's number.
    }
}

