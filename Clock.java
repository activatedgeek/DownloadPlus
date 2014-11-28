public class Clock {
	private long start;
	private long elapsed;
	public enum Resolution{
		MILLISEC, SEC, MINUTE, HOUR, DAY
	}
	
	public Clock(){
		start = -1;
		elapsed = 0;
	}
	
	public void startTimer(){
		start = System.currentTimeMillis();
	}
	
	/* return elapsed milliseconds */
	public long getTotalElapsedTime(){
		/* start timer before using */
		if(start == -1){
			Logger.log(Logger.Status.ERR_CLOCK);
			return -1;
		}
		elapsed = System.currentTimeMillis() - start;
		return elapsed;
	}
	
	/* get elapsed time from start of clock (in different resolutions) */
	public long getTotalElapsedTime(Resolution reso){
		/* start timer before using */
		if(start == -1){
			Logger.log(Logger.Status.ERR_CLOCK);
			return -1;
		}
		
		elapsed = System.currentTimeMillis() - start;
		if(reso == Resolution.MILLISEC)
			return elapsed;
		
		long elapsedReso = elapsed/1000;
		if(reso == Resolution.SEC)
			return elapsedReso;
		
		elapsedReso /= 60;
		if(reso == Resolution.MINUTE)
			return elapsedReso;
		
		elapsedReso /= 60;
		if(reso == Resolution.HOUR)
			return elapsedReso;
		
		elapsedReso /= 24;
		return elapsedReso;
	}
	
	/* get elapsed time from last lap */
	public long getLastElapsedLapTime(){
		return System.currentTimeMillis()-elapsed;
	}
	
	public void resetTimer(){
		start = System.currentTimeMillis();
		elapsed = 0;
	}
}