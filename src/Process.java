import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Process implements Runnable
{
	private int id;
	ArrayList<Link> neighborLinks;
	private BlockingQueue<Message> qIn, qMaster, qRound;
	
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
	
	private ArrayList<Message> outList = new ArrayList<Message>();
	
	public Process(int id)
	{
		this.id = id;
		neighborLinks = new ArrayList<Link>();
		
		Message msg;
		Iterator<Link> it = neighborLinks.iterator();
		while(it.hasNext())
		{
			Link l = it.next();
			msg = new Message(this, EXPLORE, msgId++, l.getTs(this, roundNo), l.getNeighbor(this));
			outList.add(msg);
		}
		
		qIn = new LinkedBlockingQueue<Message>();
		
		latch = new CountDownLatch(neighborLinks.size());
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
				System.out.println("Process "+this.getId()+" terminated!");
				break;
			}
			if(msg.getType() == 'N')
			{
				Iterator<Message> it = outList.iterator();
				Message sendMsg;
				while(it.hasNext())
				{
					sendMsg = it.next();
					
					for(Link link : neighborLinks)
					{
						if(sendMsg.getTo() == link.getNeighbor(this))
						{
							link.getNeighbor(this).writeToQueueIn(sendMsg);
						}
					}
				}
				
				for(Link link : neighborLinks)
				{
					link.getNeighbor(this).countDownLatch();
				}
				
				try
				{
					latch.await();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
				latch = new CountDownLatch(neighborLinks.size());
				
				
			}
			
			Message ready = new Message(this, READY, msgId++, Integer.MIN_VALUE, null);
			//sending token to neighbors
			try {
				qMaster.put(ready);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
