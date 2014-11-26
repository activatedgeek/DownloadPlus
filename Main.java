
public class Main {
	/* Enable/Disable Logging here */
	static{
		Logger.enableLog();
		Logger.enableDebug();
		//ConnectionProxy.setHTTPProxy("localhost", 8080);
		ConnectionProxy.unsetHTTPProxy();
	}
	
	public static void main(String[] args) {
		//String url = "https://put.io/v2/files/247219415/download?token=aac7236a800511e2852d001018321b64";
		new Downloader("http://www.le.co.in");
	}
}