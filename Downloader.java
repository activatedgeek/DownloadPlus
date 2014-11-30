import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Downloader extends Thread{
	DownloadUnit dUnit;
	private String finalURI;
	private String destPath;
	private HttpURLConnection con;
	private int readTimeout = 5000;
	private int bufferSize = 4096;
	
	private int numSegments = 1;
	private List<Long> segmentSizes = new ArrayList<Long>();
	private List<Long> segmentProgress = new ArrayList<Long>();
	private List<Thread> segmentThread = new ArrayList<Thread>();

	public String fileName;
	public long fileSize;
	private boolean acceptRanges = false;
	
	public Downloader(DownloadUnit dUnit) {
		this.dUnit = dUnit;
		/*** if a new download ***/
		if(dUnit.statusEnum == DownloadUnit.Status.QUEUED){
			URIExplore uri = new URIExplore((String)dUnit.getProperty(DownloadUnit.TableField.ORIGIN));
			dUnit.setProperty(DownloadUnit.TableField.URL, (String)uri.finalURI);
		}
		finalURI = (String)dUnit.getProperty(DownloadUnit.TableField.URL);
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
		
		/* Make a connection and a HEAD request to determine size, name, resume support */
		if(dUnit.statusEnum == DownloadUnit.Status.QUEUED){
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
						dUnit.setProperty(DownloadUnit.TableField.SIZE, "N/A");
					}
					/* set file size */
					dUnit.sizeLong = fileSize;
					
					int responseCode = con.getResponseCode();
					if(responseCode == HttpURLConnection.HTTP_OK){
						setfileName();
						/* Download in segments if server supports Accept-Ranges header */
						String rangeSupport = con.getHeaderField("Accept-Ranges");
						if (rangeSupport != null){
							acceptRanges = true;
							dUnit.setProperty(DownloadUnit.TableField.RESUME, true);
						}
						con.disconnect();
					}
				}
			}catch(Exception e){
				Logger.log(Logger.Status.ERR_CONN, e.getMessage());
				dUnit.statusEnum = DownloadUnit.Status.ERROR;
				dUnit.setProperty(DownloadUnit.TableField.STATUS, "Connection Error");
			}
		}
		/* retrieve existing information */
		else{
			fileName = (String)dUnit.getProperty(DownloadUnit.TableField.FILENAME);
			fileSize = dUnit.sizeLong;
			acceptRanges = (boolean)dUnit.getProperty(DownloadUnit.TableField.RESUME);
		}
		
		/* start processing */
		buildSegments();
		startDownloading();
	}
	
	private void buildSegments(){
		if(dUnit.statusEnum == DownloadUnit.Status.QUEUED){
			if(acceptRanges){
				numSegments = 5;				//TODO: Build segmentation logic
				dUnit.setProperty(DownloadUnit.TableField.RESUME, true);
			}
			dUnit.setProperty(DownloadUnit.TableField.SEGMENTS, numSegments);
		}
		else{
			numSegments = (int)dUnit.getProperty(DownloadUnit.TableField.SEGMENTS);
		}
		
		/* populate progress list */
		if(dUnit.statusEnum == DownloadUnit.Status.QUEUED){			/* for new downloads */
			for(int i=0;i<numSegments;++i){
				segmentProgress.add((long)0);
				dUnit.chunks.add((long)0);
			}
		}
		else{
			for(int i=0;i<numSegments;++i)
				segmentProgress.add((long)dUnit.chunks.get(i));
		}
		
		long segmentSize = -1;
		if(fileSize != -1){
			segmentSize = fileSize/numSegments;
			if(fileSize%numSegments != 0)
				segmentSize += 1;
		}
		
		/* create segment sizes */
		for(int i=0;i<numSegments-1;++i)
			segmentSizes.add((long)segmentSize);
		segmentSizes.add((long)(fileSize - (segmentSize*(numSegments-1))));
		
		/* create downloader threads for downloading */
		long startByte = 0;
		for(int i=0;i<numSegments;++i){
			Thread t = new downloadSegment(i, startByte+segmentProgress.get(i));
			segmentThread.add(t);
			startByte += segmentSizes.get(i);
		}
	}
	
	private void startDownloading(){
		dUnit.setProperty(DownloadUnit.TableField.STATUS, "Downloading");
		/* create temporary file */
		File file = new File(destPath+File.separator+fileName);
		try{
			file.createNewFile();
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_FILE, e.getMessage());
		}
		
		/* start downloading segments */
		for(int i=0;i<segmentThread.size();++i){
			segmentThread.get(i).start();
		}
		
		/* join all threads to wait finishing */
		for(int i=0;i<segmentThread.size();++i){
			try {
				segmentThread.get(i).join();
			} catch (InterruptedException e) {
				Logger.log(Logger.Status.ERR_THREAD, e.getMessage());
			}
		}
		if(numSegments>1)
			mergeSegments();
		
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
			for(int i=0; i<numSegments; i++){
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
		private long startByte;
		
		downloadSegment(int segNo, long startByte) {
			segmentName = fileName + ".part" + segNo;
			segConnection = makeConnection();
			this.startByte = startByte;
			this.segNo = segNo;
		}
		
		/* Thread downloads a particular range of file from server on an independent connection */
		public void run(){
			try{
				segConnection.setRequestMethod("GET");
				segConnection.setReadTimeout(readTimeout);
				if(acceptRanges){
					if(fileSize != -1)
						segConnection.setRequestProperty("Range", "bytes=" + startByte + "-" + (startByte-segmentProgress.get(segNo)+segmentSizes.get(segNo)-1));
					else
						segConnection.setRequestProperty("Range", "bytes="+segmentProgress.get(segNo)+"-");
				}
				
				InputStream inputStream = segConnection.getInputStream();
				/* Path where segment is saved */
				String segmentsavePath = Main.tempFolderPath + File.separator + segmentName;
				if(numSegments == 1){
					segmentsavePath = destPath + File.separator + fileName;
				}
				
				/* append bytes to output stream */
				FileOutputStream outputStream = new FileOutputStream(segmentsavePath, true);
				
				/* number of bytes remaining to be read */
				long readRemaining = segmentSizes.get(segNo);
				int bytesRead = -1;
				byte[] buffer = new byte[bufferSize];
				
				while((bytesRead = inputStream.read(buffer)) != -1){
					if(fileSize > 0){
						readRemaining -= bytesRead;				/* decrement remaining bytes to read */
						/* garbage if more bytes are read than needed */
						
						if(readRemaining < 0)
							bytesRead = (int)readRemaining+bytesRead;	
					}
					outputStream.write(buffer, 0, bytesRead);
					long update = segmentProgress.get(segNo) + bytesRead;
					segmentProgress.set(segNo, update);
					dUnit.chunks.set(segNo, update);
				}
				
				outputStream.close();
				inputStream.close();
			}
			catch(SocketTimeoutException e){
				Logger.log(Logger.Status.ERR_READ, e.getMessage());
			}
			catch(Exception e){
				Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			}finally{
				segConnection.disconnect();
			}
		}
	}
}
