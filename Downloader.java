import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader extends Thread{
	private String finalURI;
	private String destPath;
	private HttpURLConnection con;
	private int numSegments = 1;

	public String fileName;
	public long fileSize;
	
	public Downloader(String url, String path) {
		
		URIExplore uri = new URIExplore(url);
		Logger.debug(uri.finalURI);
		finalURI = uri.finalURI;
		destPath = path;
	}
	
	/* Sets the filename for a file at given URL */
	private void setfileName(){
		String disposition = con.getHeaderField("Content-Disposition");
		String contentType = con.getContentType();

		if(disposition != null){
			int index = disposition.indexOf("filename=");
			if (index > 0){
				fileName = disposition.substring(index+9, disposition.length());
				if(fileName.substring(0, 1).equals("\""))
					fileName = fileName.substring(1,fileName.length()-1);
			}
		}
		else if(contentType.split(";")[0].equals("text/html"))
			fileName = "index.html";
		else
			fileName = finalURI.substring(finalURI.lastIndexOf("/") + 1, finalURI.length());	
	}
	
	/* Sets up a connection to the given finalURI */
	private HttpURLConnection makeConnection(){
		try{
			/* support for proxy connections */
			if(ConnectionProxy.proxyHTTP != null)
				return (HttpURLConnection) new URL(finalURI).openConnection(ConnectionProxy.proxyHTTP);
			else
				return (HttpURLConnection) new URL(finalURI).openConnection();
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			return null;
		}
	}
	
	/* Setup the download parameters */
	public void run(){
		/* prevent further redirects */
		HttpURLConnection.setFollowRedirects(false);
		
		/* Make a connection and a HEAD request to determine size and name */
		try{
			con = makeConnection();
			if (con != null){
				con.setRequestMethod("HEAD");
				Logger.debug(""+con.getHeaderFields());
				
				/* set file size (in bytes) and name */
				fileSize = con.getContentLength();			
				int responseCode = con.getResponseCode();
				if(responseCode == HttpURLConnection.HTTP_OK){
					setfileName();
					/* Download in segments if server supports Accept-Ranges header */
					String rangeSupport = con.getHeaderField("Accept-Ranges");
					if (rangeSupport != null)
						numSegments = 5;
					// TODO: segment calculation logic
					con.disconnect();
					
					startDownloading();
				}
			}
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			return;
		}
	}
	
	private void startDownloading(){
		/* No segment downloading, directly download the whole file */
		if(numSegments==1){
			try{
				con = makeConnection();
				con.setRequestMethod("GET");
				InputStream inputStream = con.getInputStream();
				String filesavePath = destPath + File.separator + fileName;
	
				FileOutputStream outputStream = new FileOutputStream(filesavePath);
	
				int bytesRead = -1;
				byte[] buffer = new byte[4096];
				int total = 0;
				while((bytesRead = inputStream.read(buffer)) != -1){
					outputStream.write(buffer, 0, bytesRead);
					total += bytesRead;
					Logger.debug("Wrote : "+bytesRead + " bytes");
				}
	
				Logger.debug(((float)total/1024)/1024+" MB written.");
				outputStream.close();
				inputStream.close();
			}catch(Exception e){
				Logger.log(Logger.Status.ERR_CONN, e.getMessage());
				return;
			}finally{
				con.disconnect();
			}
		}
		/* Make threads for each segment download */
		else{
			long startByte = 0;
			long segmentSize = fileSize/numSegments + numSegments;
			Thread t_list[] = new Thread[numSegments];
			// make threads
			for(int i=1; i<=numSegments; i++){
				t_list[i-1] = new downloadSegment(i, startByte, segmentSize);
				t_list[i-1].start();
				startByte += segmentSize;
			}
			for(int i=0; i<numSegments; i++){
				try {
					t_list[i].join();
				} catch (InterruptedException e) {
					Logger.log(Logger.Status.ERR_CONN, e.getMessage());
					return;
				}
			}
			Logger.debug("All threads finished downloading");
			// merge the downloaded segments
			mergeSegments();
		}
	}
	
	/* Merge the downloaded segments into one file and deletes them */
	private void mergeSegments(){
		String filesavePath = destPath + File.separator + fileName;
		try {
			FileOutputStream finalFile = new FileOutputStream(filesavePath);
			FileInputStream inputSegment;
			byte buffer[] = new byte[4096];
			int bytesRead = -1;
			// read each segment and delete afterwards
			for(int i=1; i<=numSegments; i++){
				String segmentPath = Main.tempFolderPath + File.separator + fileName + ".part" + i;
				inputSegment = new FileInputStream(segmentPath);
				while((bytesRead=inputSegment.read(buffer)) != -1)
					finalFile.write(buffer, 0, bytesRead);
				inputSegment.close();
				// delete the segment
				File file = new File(segmentPath);
				file.delete();
			}
			finalFile.close();
		} catch (Exception e) {
			Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			return;
		}		
	}
	
	/* Class to work as thread for downloading one segment */
	private class downloadSegment extends Thread{
		private String segmentName;
		private HttpURLConnection segConnection;
		private int segNo;
		private long startByte, segmentSize;
		
		downloadSegment(int segNo, long startByte, long segmentSize) {
			segmentName = fileName + ".part" + segNo;
			segConnection = makeConnection();
			this.startByte = startByte;
			this.segmentSize = segmentSize;
			this.segNo = segNo;
		}
		
		/* Thread downloads a particular range of file from server on an independent connection */
		public void run(){
			try{
				segConnection.setRequestMethod("GET");
				segConnection.setRequestProperty("Range", "bytes=" + startByte + "-" + (startByte+segmentSize-1));
				InputStream inputStream = segConnection.getInputStream();
				// Path where segment is saved
				String segmentsavePath = Main.tempFolderPath + File.separator + segmentName;
				
				FileOutputStream outputStream = new FileOutputStream(segmentsavePath);
				int bytesRead = -1;
				byte[] buffer = new byte[4096];
				int total = 0;
				while((bytesRead = inputStream.read(buffer)) != -1){
					outputStream.write(buffer, 0, bytesRead);
					total += bytesRead;
					Logger.debug("Segment" + segNo + ": Wrote : "+bytesRead + " bytes");
				}
	
				Logger.debug("Segment" + segNo + ": " + ((float)total/1024)/1024+" MB written.");
				outputStream.close();
				inputStream.close();
			}catch(Exception e){
				Logger.log(Logger.Status.ERR_CONN, e.getMessage());
				return;
			}finally{
				segConnection.disconnect();
			}
		}
	}
}
