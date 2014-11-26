
public class Main {
	/* Enable/Disable Logging here */
	static{
		Logger.enableLog();
		Logger.enableDebug();
		ConnectionProxy.setHTTPProxy("192.168.36.22", 3128, "ee12b1021", "123");
		//ConnectionProxy.unsetHTTPProxy();
	}
	
	public static void main(String[] args) {
		String url = "https://put.io/v2/files/253883579/download?token=aac7236a800511e2852d001018321b64";
		//String url = "http://iitmweb.iitm.ac.in/phase2/courses/coursecontents/106108113/mod01lec01.mp4";
		//String url = "https://www.facebook.com/";
		Downloader d = new Downloader(url, "/home/chinmay/Desktop");
	}
}
