import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class Downloader {
	private String finalURI;
	private String destPath;
	
	public Downloader(String url, String path) {
		URIExplore uri = new URIExplore(url);
		Logger.debug(uri.finalURI);
		finalURI = uri.finalURI;
		destPath = path;
		startDownloading();
	}
	
	public void startDownloading(){
		try{
			HttpURLConnection con = (HttpURLConnection) new URL(finalURI).openConnection(ConnectionProxy.proxyHTTP);
			con.setRequestMethod("HEAD");
			int responseCode = con.getResponseCode();
			// valid destination link
			if(responseCode == HttpURLConnection.HTTP_OK){
				String disposition = con.getHeaderField("Content-Disposition");
				String contentType = con.getContentType();
				int contentLength = con.getContentLength();

				// setting up the filename
				String fileName = "";
				if(disposition != null){			// get the filename from header
					int index = disposition.indexOf("filename=");
					if (index > 0)
						fileName = disposition.substring(index+9, disposition.length());
				}									// if it is HTML file
				else if(contentType.split(";")[0].equals("text/html"))
					fileName = "index.html";
				else								// get it from URL
					fileName = finalURI.substring(finalURI.lastIndexOf("/") + 1, finalURI.length());
				
				//System.out.println(fileName);
				
				// input stream from HTTP connection is opened
				InputStream inputStream = con.getInputStream();
				String filesavePath = destPath + File.separator + fileName;
				System.out.println(filesavePath);
				// output stream to save new file is opened
				FileOutputStream outputStream = new FileOutputStream(filesavePath);
				
				int bytesRead = -1;
				byte[] buffer = new byte[4096];
				System.out.println(inputStream.read(buffer));
				while((bytesRead = inputStream.read(buffer)) != -1){
					outputStream.write(buffer, 0, bytesRead);
					System.out.println(bytesRead);
				}
				
				System.out.println("successfully downloaded the file");
				outputStream.close();
				inputStream.close();
				con.disconnect();
			}
		}catch(Exception e){
			System.out.println(e.getMessage());
			// TODO: problem in URL, aborting download
			return;
		}
	}
}
