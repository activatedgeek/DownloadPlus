import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

/*
 *	class URIExplore explores a given URL and gets the information about
 *	the contents being served at the server.
 */
public class URIExplore {
	public String finalURI;
	public long contentLength;
	public int segments;
	
	private int responseCode;
	private String url;
	
	private HttpURLConnection con = null;
	
	public URIExplore(String url){
		this.url = url;
		explore();
	}
	
	private void explore(){
		try{
			HttpURLConnection.setFollowRedirects(false);
			con = (HttpURLConnection) new URL(url).openConnection(Downloader.setProxy());
			con.setRequestMethod("HEAD");
			responseCode = con.getResponseCode();
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			con.disconnect();
			if(responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_MOVED_TEMP){
				System.out.println(responseCode);
				return;
			}
		}
		// TODO: Check cycles in redirection
		try{
			con = (HttpURLConnection) new URL(url).openConnection(Downloader.setProxy());
			con.setRequestMethod("GET");
			while(con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP){
				String nextLocation = con.getHeaderField("Location");
				System.out.println(nextLocation + " " + con.getResponseCode());
				con.disconnect();
				con = (HttpURLConnection) new URL(nextLocation).openConnection(Downloader.setProxy());
				con.setRequestMethod("GET");
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			con.disconnect();
		}
	}
}
