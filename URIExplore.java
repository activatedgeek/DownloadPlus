import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/*
 *	URIExplore explores a given URL, gets the information about
 *	the contents being served at the server.
 *	Following contents are returned:
 *		finalURI -> file at the existing URI
 *		contentLength -> length of the content being served
 *		segments -> number of segments used to download multipart
 */
public class URIExplore {
	public String finalURI;
	
	private int responseCode;
	private Set<String> redirectionSet = null;					/* check redirection loops */
	
	private HttpURLConnection con = null;
	
	public URIExplore(String url){
		finalURI = url;
		explore();
	}

	private void explore(){
		/* try connection establishment */
		try{
			HttpURLConnection.setFollowRedirects(false);
			con = (HttpURLConnection) new URL(finalURI).openConnection(Downloader.setProxy());
			con.setRequestMethod("HEAD");
			responseCode = con.getResponseCode();
		} catch(Exception e){
			/* error establishing connection */
			Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			finalURI = null;
			return;
		} finally{
			con.disconnect();
			/* everything is an error except 200,301 (permanent move),302 (temporary move) */
			if(responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_MOVED_TEMP && responseCode != HttpURLConnection.HTTP_MOVED_PERM){
				Logger.log(Logger.Status.ERR_HTTP, "Status Code: "+responseCode);
				return;
			}
		}

		try{
			/* trace redirection links */
			if(responseCode != HttpURLConnection.HTTP_OK){
				/* create a set to check for unique links */
				redirectionSet = new HashSet<String>();
				redirectionSet.add(finalURI);
				
				con = (HttpURLConnection) new URL(finalURI).openConnection(Downloader.setProxy());
				con.setRequestMethod("HEAD");
				responseCode = con.getResponseCode();
				/* follow redirects till codes 301 or 302 */
				while(responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM){
					finalURI = con.getHeaderField("Location");
					/* terminate connection and routine if cycle detected */
					if(!redirectionSet.add(finalURI)){
						Logger.log(Logger.Status.ERR_REDIRECT, "Starting at: "+finalURI);
						finalURI = null;
						con.disconnect();
						return;
					}
					con.disconnect();
					
					/* setup a new connection to the redirected URI */
					con = (HttpURLConnection) new URL(finalURI).openConnection(Downloader.setProxy());
					con.setRequestMethod("HEAD");
					responseCode = con.getResponseCode();
				}
			}
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			finalURI = null;
		}finally{
			con.disconnect();
		}

		redirectionSet = null;
		con = null;
	}
}
