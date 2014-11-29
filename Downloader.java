import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader extends Thread{
	DownloadUnit dUnit;
	private String finalURI;
	private String destPath;
	private HttpURLConnection con;
	private int numSegments = 1;

	public String fileName;
	public long fileSize;
	
	public Downloader(DownloadUnit dUnit) {
		this.dUnit = dUnit;
		URIExplore uri = new URIExplore((String)dUnit.getProperty(DownloadUnit.TableField.ORIGIN));
		dUnit.setProperty(DownloadUnit.TableField.URL, (String)uri.finalURI);
		finalURI = uri.finalURI;
		destPath = (String)dUnit.getProperty(DownloadUnit.TableField.FOLDER);
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
		dUnit.setProperty(DownloadUnit.TableField.FILENAME, (String)fileName);
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
				
				/* set file size (in bytes) and name */
				fileSize = con.getContentLength();
				if(fileSize != -1){
					dUnit.setProperty(DownloadUnit.TableField.SIZE, fileSize+"");
				}
				else{
					dUnit.setProperty(DownloadUnit.TableField.SIZE, (String)"N/A");
				}
				
				int responseCode = con.getResponseCode();
				if(responseCode == HttpURLConnection.HTTP_OK){
					setfileName();
					/* Download in segments if server supports Accept-Ranges header */
					String rangeSupport = con.getHeaderField("Accept-Ranges");
					if (rangeSupport != null){
						numSegments = 5;
						dUnit.setProperty(DownloadUnit.TableField.SEGMENTS, numSegments);
					}
					// TODO: segment calculation logic
					con.disconnect();
					
					startDownloading();
				}
			}
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			dUnit.setProperty(DownloadUnit.TableField.STATUS, "Connection Error");
			return;
		}
	}
	
	private void startDownloading(){
		dUnit.setProperty(DownloadUnit.TableField.STATUS, "Downloading");
		
		/* No segment downloading, directly download the whole file */
		if(numSegments==1){
			dUnit.setProperty(DownloadUnit.TableField.SEGMENTS, 1);
			try{
				con = makeConnection();
				con.setRequestMethod("GET");
				InputStream inputStream = con.getInputStream();
				String filesavePath = destPath + File.separator + fileName;
	
				FileOutputStream outputStream = new FileOutputStream(filesavePath);
	
				int bytesRead = -1;
				byte[] buffer = new byte[4096];
				int total = 0;
				Clock clock = new Clock();
				clock.startTimer();
				while((bytesRead = inputStream.read(buffer)) != -1){
					outputStream.write(buffer, 0, bytesRead);
					total += bytesRead;
					long totaltime = clock.getTotalElapsedTime()+1;
					double rate = ((double)total*1000)/totaltime;
					Logger.debug("Wrote : "+bytesRead + " bytes" + " at rate "+rate+" Bps");
					dUnit.setProperty(DownloadUnit.TableField.TRANSFER_RATE, rate+" Bps");
				}
	
				Logger.debug(((float)total/1024)/1024+" MB written.");
				outputStream.close();
				inputStream.close();
			}catch(Exception e){
				Logger.log(Logger.Status.ERR_CONN, e.getMessage());
				dUnit.setProperty(DownloadUnit.TableField.STATUS, "Connection Error");
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
					dUnit.setProperty(DownloadUnit.TableField.STATUS, "Connection Error");
					return;
				}
			}
			Logger.debug("All threads finished downloading");
			
			mergeSegments();
		}
		dUnit.setProperty(DownloadUnit.TableField.STATUS, "Completed");
	}
	
	/* Merge the downloaded segments into one file and deletes them */
	private void mergeSegments(){
		dUnit.setProperty(DownloadUnit.TableField.STATUS, "Merging");
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
			Logger.log(Logger.Status.ERR_MERGE, e.getMessage());
			dUnit.setProperty(DownloadUnit.TableField.STATUS, "Error Merging");
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
				// number of bytes remaining to be read
				long readRemaining = segmentSize;
				int bytesRead = -1;
				byte[] buffer = new byte[4096];
				int total = 0;
				while((bytesRead = inputStream.read(buffer)) != -1){
					readRemaining-=bytesRead;				// decrement remaining bytes to read
					// garbage if more bytes are read than needed
					if(readRemaining < 0)
						bytesRead = (int)readRemaining+bytesRead;
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
