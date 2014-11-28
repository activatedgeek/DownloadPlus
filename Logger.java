import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class Logger {
	private static boolean log = false;
	private static boolean debug = false;
	/* enumerations for errors, check the map below for message strings */
	public enum Status{
		ERR_CONN,ERR_HTTP,ERR_REDIRECT,ERR_CLOCK
	}
	private static final Map<Status, String> errorMap = new HashMap<Status,String>();
	static{
		errorMap.put(Status.ERR_CONN, "Error establishing connection.");
		errorMap.put(Status.ERR_HTTP, "Error response from HTTP.");
		errorMap.put(Status.ERR_REDIRECT, "Redirection cycle detected.");
		errorMap.put(Status.ERR_CLOCK, "Error operating with clock.");
	}
	
	public static void enableLog(){
		log = true;
	}
	
	public static void enableLog(boolean flag){
		log = flag;
	}

	public static void enableDebug(){
		debug = true;
	}
	
	public static void enableDebug(boolean flag){
		debug = flag;
	}
	
	public static void log(Status status){
		if(log){
			System.out.print("["+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"] ");
			System.out.println(status.toString()+ " -> "+errorMap.get(status));
		}
	}
	
	public static void log(Status status, String extra){
		if(log){
			System.out.print("["+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"] ");
			System.out.println(status.toString()+ " -> "+errorMap.get(status)+" "+extra);
		}
	}
	
	public static void debug(String msg){
		if(debug){
			System.out.println("["+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"] "+msg);
		}
	}
}
