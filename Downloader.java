import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class Downloader {
	private String finalURI;
	private String destPath;
	private HttpURLConnection con;
	
	public Downloader(String url, String path) {
		URIExplore uri = new URIExplore(url);
		Logger.debug(uri.finalURI);
		finalURI = uri.finalURI;
		destPath = path;
		startDownloading();
	}
	
	public void startDownloading(){
		try{
			/* prevent further redirects */
			HttpURLConnection.setFollowRedirects(false);
			/* support for proxy connections */
			if(ConnectionProxy.proxyHTTP != null)
				con = (HttpURLConnection) new URL(finalURI).openConnection(ConnectionProxy.proxyHTTP);
			else
				con = (HttpURLConnection) new URL(finalURI).openConnection();
			
			con.setRequestMethod("GET");
			//TODO: check for 206 (partial content) accept ranges header
			
			//con.setRequestProperty("", "");
			int responseCode = con.getResponseCode();

			Logger.debug(""+con.getHeaderFields());
			
			if(responseCode == HttpURLConnection.HTTP_OK){
				String disposition = con.getHeaderField("Content-Disposition");
				String contentType = con.getContentType();

				String fileName = "";
				if(disposition != null){
					int index = disposition.indexOf("filename=");
					if (index > 0)
						fileName = disposition.substring(index+9, disposition.length());
				}
				else if(contentType.split(";")[0].equals("text/html"))
					fileName = "index.html";
				else
					fileName = finalURI.substring(finalURI.lastIndexOf("/") + 1, finalURI.length());
				
				// input stream from HTTP connection is opened
				InputStream inputStream = con.getInputStream();
				String filesavePath = destPath + File.separator + fileName;
				Logger.debug(filesavePath);
				
				FileOutputStream outputStream = new FileOutputStream(filesavePath);
				
				int bytesRead = -1;
				byte[] buffer = new byte[4096];
				int total = 0;
				while((bytesRead = inputStream.read(buffer)) != -1){
					outputStream.write(buffer, 0, bytesRead);
					total += bytesRead;
					Logger.debug("Wrote : "+bytesRead + " bytes");
				}
				
				Logger.debug((total/1024)/1024+" MB written.");
				outputStream.close();
				inputStream.close();
			}
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			return;
		}finally{
			con.disconnect();
		}
	}
}
