import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
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
	private Set<String> redirectionSet;
	
	private HttpURLConnection con = null;
	
	public URIExplore(String url){
		this.finalURI = url;		
		explore();
	}

	// processing the link
	private void explore(){
		try{
			HttpURLConnection.setFollowRedirects(false);
			con = (HttpURLConnection) new URL(finalURI).openConnection(Downloader.setProxy());
			con.setRequestMethod("HEAD");
			responseCode = con.getResponseCode();
		} catch(Exception e){
			// TODO: ConnectException 
		} finally{
			con.disconnect();
			if(responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_MOVED_TEMP){
				// TODO: Invalid URL
				System.out.println(responseCode);
				return;
			}
		}
		
		try{
			// if first connection returned OK, no redirections
			if(responseCode != HttpURLConnection.HTTP_OK){
				redirectionSet = new HashSet<String>();
				redirectionSet.add(finalURI);
				
				con = (HttpURLConnection) new URL(finalURI).openConnection(Downloader.setProxy());
				con.setRequestMethod("HEAD");
				while(con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP){
					finalURI = con.getHeaderField("Location");
					if(!redirectionSet.add(finalURI)){
						// TODO: cycle in redirection error
						break;
					}
					//System.out.println(finalURI + " " + con.getResponseCode());
					con.disconnect();
					con = (HttpURLConnection) new URL(finalURI).openConnection(Downloader.setProxy());
					con.setRequestMethod("HEAD");
				}
			}
		}catch(Exception e){
			// TODO: ConnectException
		}finally{
			con.disconnect();
		}
		contentLength = Integer.parseInt(con.getHeaderField("Content-Length"));
		// TODO: build segment logic
		segments = 5;
	}
}
