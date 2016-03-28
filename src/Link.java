import java.util.Random;

public class Link
{
	private Random random;
	private Process p1;
	private Process p2;
	private int lastSent1 = 0;
	private int lastSent2 = 0;
	
	public Link(Process p1, Process p2)
	{
		random = new Random();
		this.p1 = p1;
		this.p2 = p2;
	}
	
	public Process getP1() {
		return p1;
	}

	public Process getP2() {
		return p2;
	}

	public int getTs(Process p, int p_round)
	{
		if(p == p1)
		{
			lastSent1 = random.nextInt(20) + 1 + Math.max(p_round, lastSent1);
			return lastSent1;
		}
		else
		{
			lastSent2 = random.nextInt(20) + 1 + Math.max(p_round, lastSent2);
			return lastSent2;
		}
	}
	
	public Process getNeighbor(Process p)
	{
		if(p == p1)
		{
			return p2;
		}
		
		return p1;
	}
}
