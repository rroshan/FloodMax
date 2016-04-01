import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Process implements Runnable
{
	private int id;
	ArrayList<Link> neighborLinks;
	private BlockingQueue<Message> qMaster, qRound;

	private PriorityQueue<Message> qIn;

	private final char READY = 'R';
	private final char NEW_ROUND = 'N';
	private final char EXPLORE = 'E';
	private final char ACCEPT = 'A';
	private final char REJECT = 'J';
	private final char EXP_COMPLETED = 'C';
	private final char LEADER = 'L';
	private int msgId = 0;
	private int roundNo = 0;
	private CountDownLatch latch;
	private int largestSeenSoFar;
	private Process parent;
	private boolean exploreFlag;
	private int exploreCount;
	private int rejectCount;
	private int exploreCompletedCount;
	private boolean leaderFound;
	private boolean exploreCompleted;

	private ArrayList<Message> outList = new ArrayList<Message>();
	private ArrayList<Process> rejectList = new ArrayList<Process>();
	private ArrayList<Process> children = new ArrayList<Process>();

	public Process(int id)
	{
		this.id = id;
		neighborLinks = new ArrayList<Link>();

		qIn = new PriorityQueue<Message>(11, new Comparator<Message>() {
			@Override
			public int compare(Message msg1, Message msg2)
			{
				if(msg1.getTs() == msg2.getTs())
				{
					return msg1.getFrom().getId() - msg2.getFrom().getId();
				}

				return msg1.getTs() - msg2.getTs();
			}
		}); 

		latch = new CountDownLatch(neighborLinks.size());

		largestSeenSoFar = id;

		exploreFlag = false;

		leaderFound = false;

		exploreCompleted = false;
	}

	public void initialize()
	{
		Message msg;

		Iterator<Link> it = neighborLinks.iterator();
		while(it.hasNext())
		{
			Link l = it.next();
			msg = new Message(this, EXPLORE, msgId++, l.getTs(this, roundNo), l.getNeighbor(this), id);
			//System.out.println(id+" Added "+msg+ " to outlist");
			outList.add(msg);
			exploreCount++;
		}
		//System.out.println(id+ " Exp count after initialize: "+exploreCount);
	}

	public int getId()
	{
		return id;
	}

	public void setQMaster(BlockingQueue<Message> qMaster)
	{
		this.qMaster = qMaster;
	}

	public void addLink(Link link)
	{
		neighborLinks.add(link);
	}

	public void writeToQueueIn(Message msg)
	{
		qIn.add(msg);
	}

	public void countDownLatch()
	{
		latch.countDown();
	}

	public void setQRound(BlockingQueue<Message> qRound)
	{
		this.qRound = qRound;
	}

	@Override
	public void run()
	{
		while(true)
		{
			//waiting new round start message
			Message msg = null;
			try {
				msg = qRound.take();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				//System.out.println("Process "+this.getId()+" terminated!");
				break;
			}

			//System.out.println(id+" mes from roundQ "+ msg.getType()+ " at round "+roundNo);

			if(msg.getType() == 'N')
			{
				//roundNo++;
				exploreFlag = false;
				//System.out.println(id+" "+outList.toString());
				Iterator<Message> it = outList.iterator();
				Message sendMsg;
				while(it.hasNext())
				{
					sendMsg = it.next();
					//System.out.println(id+" "+sendMsg);

					for(Link link : neighborLinks)
					{
						if(sendMsg.getTo() == link.getNeighbor(this))
						{
							System.out.println(id+ " Writing "+sendMsg+ " to "+link.getNeighbor(this).getId()+ " at round "+roundNo);
							link.getNeighbor(this).writeToQueueIn(sendMsg);
						}
					}
				}

				for(Link link : neighborLinks)
				{
					//System.out.println(id+" couting down latch for "+link.getNeighbor(this).getId() + " at round "+roundNo);
					link.getNeighbor(this).countDownLatch();
				}

				try
				{
					latch.await();
					System.out.println(id+" notified "+roundNo);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}

				latch = new CountDownLatch(neighborLinks.size());

				outList.clear();

				Message m1 = null;

				if(!qIn.isEmpty())
				{
					while(qIn.size() > 0)
					{
						System.out.println(id+" peeking"+qIn.peek()+ " at round "+roundNo);
						if(qIn.peek().getTs() == roundNo)
						{
							System.out.println(id+ " round matched at round "+roundNo);
							m1 = qIn.remove();
							System.out.println(id+" Processing msg "+m1);

							//System.out.println(m1+" in round "+roundNo);

							if(m1.getType() == EXPLORE)
							{
								System.out.println(id+" Message of type EXPLORE");
								if(m1.getId() > largestSeenSoFar)
								{
									System.out.println(id+" id > largestSeenSoFar");
									exploreFlag = true;

									if(parent != null)
									{
										//send reject to parent
										rejectList.add(parent);
									}

									System.out.println(id+"largestSeenSoFar before: "+largestSeenSoFar);
									largestSeenSoFar = m1.getId();
									parent = m1.getFrom();
									System.out.println(id+"largestSeenSoFar after: "+largestSeenSoFar);
								}
								else if((m1.getId() < largestSeenSoFar))
								{
									System.out.println(id+" id == largestSeenSoFar");
									//send reject
									rejectList.add(m1.getFrom());
								}
								else
								{
									for(Link l : neighborLinks)
									{
										if(l.getNeighbor(this) == m1.getFrom())
										{
											msg = new Message(this, EXP_COMPLETED, msgId++, l.getTs(this, roundNo), l.getNeighbor(this), largestSeenSoFar);
											outList.add(msg);
										}
									}
									//System.out.println(id+" id < largestSeenSoFar..ignoring");
									//ignore
								}
							}
							else if(m1.getType() == REJECT)
							{
								//need to process
								rejectCount++;
							}
							else if(m1.getType() == EXP_COMPLETED)
							{
								//add him as child
								exploreCompletedCount++;
								children.add(m1.getFrom());
							}
							else if(m1.getType() == LEADER)
							{
								System.out.println("Leader is: "+m1.getId());
								System.out.println("Parent:-------------------------------->"+parent.toString());
								System.out.println(id+" Children:------------------------------>"+children.toString());
								for(Process child : children)
								{
									for(Link l : neighborLinks)
									{
										if(l.getNeighbor(this) == child)
										{
											msg = new Message(this, LEADER, msgId++, l.getTs(this, roundNo), l.getNeighbor(this), m1.getId());
											outList.add(msg);
										}
									}
								}

								leaderFound = true;
							}
						}
						else
						{
							break;
						}
					}
				}
			}

			//send explore to all neighbors except parent
			if(exploreFlag)
			{
				System.out.println(id+" INside exploreFlag");
				for(Link l : neighborLinks)
				{
					if(l.getNeighbor(this) != parent)
					{
						System.out.println(id+" sending explore to "+l.getNeighbor(this).getId());
						msg = new Message(this, EXPLORE, msgId++, l.getTs(this, roundNo), l.getNeighbor(this), largestSeenSoFar);
						System.out.println(id+" explore phase..msg "+msg);
						outList.add(msg);
						exploreCount++;
					}
				}
			}

			//send reject to rejectList and clear
			for(Process p : rejectList)
			{
				for(Link l : neighborLinks)
				{
					if(l.getNeighbor(this) == p)
					{
						System.out.println(id+" sending reject to "+l.getNeighbor(this).getId());
						msg = new Message(this, REJECT, msgId++, l.getTs(this, roundNo), l.getNeighbor(this), largestSeenSoFar);
						System.out.println(id+" reject phase..msg "+msg);
						outList.add(msg);
					}
				}
			}
			rejectList.clear();

			System.out.println(id+" exploreCount "+exploreCount);
			System.out.println(id+" exploreCompletedCount "+exploreCompletedCount);
			System.out.println(id+" rejectCount "+rejectCount);

			if(exploreCount == (exploreCompletedCount + rejectCount) && rejectCount > 0 && !exploreCompleted)
			{
				System.out.println(id+" Case matched for interior node");
				//add to outlist expcompleted
				for(Link l : neighborLinks)
				{
					if(l.getNeighbor(this) == parent)
					{
						msg = new Message(this, EXP_COMPLETED, msgId++, l.getTs(this, roundNo), l.getNeighbor(this), largestSeenSoFar);
						outList.add(msg);
						exploreCompleted = true;
						break;
					}
				}
			}
			else if(exploreCount == exploreCompletedCount && !leaderFound)
			{
				System.out.println(id+" I'm leader");
				System.out.println(id+" Case matched for root node");
				//put to outlist children
				for(Process child : children)
				{
					for(Link l : neighborLinks)
					{
						if(l.getNeighbor(this) == child)
						{
							msg = new Message(this, LEADER, msgId++, l.getTs(this, roundNo), l.getNeighbor(this), id);
							outList.add(msg);
						}
					}
				}

				leaderFound = true;
			}
			
			roundNo++;

			Message ready = null;
			if(leaderFound)
			{
				ready = new Message(this, LEADER, msgId++, Integer.MIN_VALUE, null, Integer.MIN_VALUE);
			}
			else
			{
				ready = new Message(this, READY, msgId++, Integer.MIN_VALUE, null, Integer.MIN_VALUE);
			}

			//sending token to neighbors
			try {
				qMaster.put(ready);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public String toString()
	{
		return "ID:"+id;
	}
}
