import java.net.*;

public class Downloader {
	public Downloader(String url) {
		new URIExplore(url);
	}
	
	//String proxyInfo = "ee12b1021:123@192.168.36.22:3128"
	public static Proxy setProxy(){
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.36.22", 3128));
		Authenticator auth = new Authenticator(){
			public PasswordAuthentication getPasswordAuthentication(){
				return (new PasswordAuthentication("ee12b1021", "123".toCharArray()));
			}
		};
		Authenticator.setDefault(auth);
		return proxy;
	}
	
}
