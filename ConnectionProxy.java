import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;

public class ConnectionProxy {
	public static Proxy proxyHTTP = null;
	public ConnectionProxy(){
		proxyHTTP = null;
		Authenticator.setDefault(null);
	}

	/* HTTP(S) proxy without authentication */
	public static void setHTTPProxy(String host, int port){
		proxyHTTP = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
	}
	
	/* HTTP(S) proxy with authentication */
	public static void setHTTPProxy(String host, int port, String username, String passwd){
		proxyHTTP = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
		Authenticator auth = new Authenticator(){
			public PasswordAuthentication getPasswordAuthentication(){
				return (new PasswordAuthentication(username, passwd.toCharArray()));
			}
		};
		Authenticator.setDefault(auth);
	}
	
	/* unset proxy settings */
	public static void unsetHTTPProxy(){
		proxyHTTP = null;
		Authenticator.setDefault(null);
	}
}
