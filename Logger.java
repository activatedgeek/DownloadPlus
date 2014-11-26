import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class Logger {
	private static boolean enable = false;
	private static boolean debug = false;
	public enum Status{
		ERR_CONN,ERR_HTTP,ERR_REDIRECT
	}
	private static final Map<Status, String> errorMap = new HashMap<Status,String>();
	static{
		errorMap.put(Status.ERR_CONN, "Error establishing connection.");
		errorMap.put(Status.ERR_HTTP, "Error response from HTTP.");
		errorMap.put(Status.ERR_REDIRECT, "Redirection cycle detected.");
	}

	public Logger(){}
	
	public static void enableLog(){
		enable = true;
	}
	
	public static void enableLog(boolean flag){
		enable = flag;
	}

	public static void enableDebug(){
		debug = true;
	}
	
	public static void enableDebug(boolean flag){
		debug = flag;
	}
	
	public static void log(Status status){
		if(enable){
			System.out.print("["+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"] ");
			System.out.println(status.toString()+ " -> "+errorMap.get(status));
		}
	}
	
	public static void log(Status status, String extra){
		if(enable){
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
