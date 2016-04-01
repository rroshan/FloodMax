
/*
 * Team Memebers: Hardik Trivedi (hpt150030)
 * 				  Roshan Ravikumar (rxr151330)
 * 				  Sapan Gandhi (sdg150130) 
 */

public class Message
{
	/*
	 * R - Ready for next round
	 * N - Start of next round
	 * E - Explore
	 * A - Accept
	 * J - Reject
	 * C - Exploration Completed
	 * L - I'm leader Leader
	 */
	private char type;

	/*
	 * Integer.MIN_VALUE denotes hops field is insignificant
	 */
	private int ts;
	private int msgId;
	private Process to;
	private Process from;
	private int id;
	//TODO
	//change type, from, to, ts

	public Message(Process from, char type, int msgId, int ts, Process to, int id)
	{
		this.from = from;
		this.type = type;
		this.msgId = msgId;
		this.ts = ts;
		this.to = to;
		this.id = id;
	}

	public char getType() {
		return type;
	}

	public void setType(char type) {
		this.type = type;
	}

	public int getTs() {
		return ts;
	}

	public void setTs(int ts) {
		this.ts = ts;
	}

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}

	public Process getTo() {
		return to;
	}

	public void setTo(Process to) {
		this.to = to;
	}

	public Process getFrom() {
		return from;
	}

	public void setFrom(Process from) {
		this.from = from;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String toString()
	{
		return "Process ID:"+from.getId()+" Type:"+type+" Msg ID: "+msgId+" Time Stamp "+ts+" To:"+to.getId()+" ID:"+id;
	}
}