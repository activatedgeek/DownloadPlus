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
	private HttpURLConnection con;
	private int readTimeout = 5000;
	private int bufferSize = 4096;
	
	private int numSegments = 1;
	private List<Long> segmentSizes = new ArrayList<Long>();
	private List<Double> segmentSpeeds = new ArrayList<Double>();
	private List<downloadSegment> segmentThread = new ArrayList<downloadSegment>();
	
	//private List<Long> segmentProgress = new ArrayList<Long>();
	
	private volatile long downloaded = 0;
	private volatile boolean okToMerge = true, downloading = true;
	private int transferRefreshRate = 200;			/* update transfer speed in 200 ms */
	
	public Downloader(DownloadUnit dUnit) {
		this.dUnit = dUnit;
		/*** if a new download ***/
		if(dUnit.statusEnum == DownloadUnit.Status.QUEUED){
			URIExplore uri = new URIExplore((String)dUnit.getProperty(DownloadUnit.TableField.ORIGIN));
			dUnit.setProperty(DownloadUnit.TableField.URL, (String)uri.finalURI);
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
					dUnit.sizeLong = con.getContentLengthLong();
					dUnit.setProperty(DownloadUnit.TableField.TYPE, con.getContentType());
					
					int responseCode = con.getResponseCode();
					if(responseCode == HttpURLConnection.HTTP_OK){
						int resp = setfileName();
						
						/* Download in segments if server supports Accept-Ranges header */
						String rangeSupport = con.getHeaderField("Accept-Ranges");
						if (rangeSupport != null){
							dUnit.resumable = true;
							dUnit.setProperty(DownloadUnit.TableField.RESUME, "Yes");
						}
						con.disconnect();
						if(resp<0)
							return;
					}
				}
			}catch(Exception e){
				Logger.log(Logger.Status.ERR_CONN, "First run "+e.getMessage());
				dUnit.statusEnum = DownloadUnit.Status.ERROR;
				dUnit.setProperty(DownloadUnit.TableField.STATUS, "Connection Error");
			}
		}

		/* update filesize in GUI */
		if(dUnit.sizeLong > 0)
			dUnit.setProperty(DownloadUnit.TableField.SIZE, polishSize(dUnit.sizeLong,""));
		else
			dUnit.setProperty(DownloadUnit.TableField.SIZE, "--");
		
		/* start processing */
		buildSegments();
		startDownloading();
	}

	public void destroyDownload(){
		downloading = false;
		for(int i=0; i<segmentThread.size();++i)
			segmentThread.get(i).destroySegment();
		try{
			(new File((String)dUnit.getProperty(DownloadUnit.TableField.FOLDER)+File.separator+(String)dUnit.getProperty(DownloadUnit.TableField.FILENAME))).delete();
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_DESTROY, e.getMessage());
		}
	}
	
	/* Sets the filename for a file at given URL */
	private int setfileName(){
		String disposition = con.getHeaderField("Content-Disposition");
		String contentType = con.getContentType();
		String fileName = "";

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
		else{
			String finalURI = (String)dUnit.getProperty(DownloadUnit.TableField.URL);
			fileName = finalURI.substring(finalURI.lastIndexOf("/") + 1, finalURI.length());
		}
		dUnit.setProperty(DownloadUnit.TableField.FILENAME, fileName);
		
		File file = new File((String)dUnit.getProperty(DownloadUnit.TableField.FOLDER)+File.separator+fileName);
		if(file.exists()){
			dUnit.statusEnum = DownloadUnit.Status.ERROR;
			dUnit.setProperty(DownloadUnit.TableField.STATUS, "Duplicate file found");
			Logger.log(Logger.Status.ERR_FILE, "File already exists");
			return -1;
		}
		return 0;
	}
	
	/* Sets up a connection to the given finalURI */
	private HttpURLConnection makeConnection(){
		try{
			/* support for proxy connections */
			String finalURI = (String)dUnit.getProperty(DownloadUnit.TableField.URL);
			if(ConnectionProxy.proxyHTTP != null)
				return (HttpURLConnection) new URL(finalURI).openConnection(ConnectionProxy.proxyHTTP);
			else
				return (HttpURLConnection) new URL(finalURI).openConnection();
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			return null;
		}
	}

	/* returns a string with suitable units, precision = 2  */
	private String polishSize(long sizeBytes, String suffix){
		double polishedSize = sizeBytes;
		String unit = "B";
		if(polishedSize>1024){
			polishedSize /= 1024;
			unit = "KB";
		}
		if(polishedSize>1024){
			polishedSize /= 1024;
			unit = "MB";
		}
		if(polishedSize>1024){
			polishedSize /= 1024;
			unit = "GB";
		}
		if(polishedSize>1024){
			polishedSize /= 1024;
			unit = "TB";
		}
		polishedSize = Math.round(polishedSize*100)/100.0d;
		return polishedSize+" "+unit+suffix;
	}
	
	public void pauseDownload(){
		for(int i=0;i<segmentThread.size();++i)
			segmentThread.get(i).pauseSegment();
		dUnit.statusEnum = DownloadUnit.Status.PAUSED;
		dUnit.setProperty(DownloadUnit.TableField.STATUS, "Paused");
	}
	
	private void buildSegments(){
		if(dUnit.statusEnum == DownloadUnit.Status.QUEUED){
			if(dUnit.resumable){
				numSegments = 5;				//TODO: Build segmentation logic
				dUnit.setProperty(DownloadUnit.TableField.RESUME, "Yes");
			}
			dUnit.setProperty(DownloadUnit.TableField.SEGMENTS, numSegments);
		}
		else{
			numSegments = (int)dUnit.getProperty(DownloadUnit.TableField.SEGMENTS);
		}
		
		/* populate progress list */
		if(dUnit.statusEnum == DownloadUnit.Status.QUEUED){			/* for new downloads */
			for(int i=0;i<numSegments;++i)
				dUnit.chunks.add((long)0);
		}
		else{
			downloaded = 0;
			for(int i=0;i<numSegments;++i)
				downloaded += (long)dUnit.chunks.get(i);
		}
		
		long segmentSize = -1;
		if(dUnit.sizeLong != -1){
			segmentSize = dUnit.sizeLong/numSegments;
			if(dUnit.sizeLong%numSegments != 0)
				segmentSize += 1;
		}
		
		/* create segment sizes */
		for(int i=0;i<numSegments-1;++i)
			segmentSizes.add((long)segmentSize);
		segmentSizes.add((long)(dUnit.sizeLong - (segmentSize*(numSegments-1))));
		
		/* create downloader threads for downloading */
		long startByte = 0;
		for(int i=0;i<numSegments;++i){
			segmentSpeeds.add((double)0);
			downloadSegment t = new downloadSegment(i, startByte+dUnit.chunks.get(i));
			segmentThread.add(t);
			startByte += segmentSizes.get(i);
		}
		
		/* thread for updating the download speed */
		new Thread(){
			public void run(){
				while(downloading){
					try {
						long speed = 0;
						for(int i=0;i<segmentSpeeds.size();++i){
							speed += segmentSpeeds.get(i);
						}
						dUnit.setProperty(DownloadUnit.TableField.TRANSFER_RATE, polishSize(speed,"ps"));
						dUnit.setProperty(DownloadUnit.TableField.DOWNLOADED, polishSize(downloaded, ""));
						if(dUnit.sizeLong>0){
							double percent = (double)downloaded/dUnit.sizeLong;
							
							dUnit.setProperty(DownloadUnit.TableField.PROGRESS, percent);
							dUnit.setProperty(DownloadUnit.TableField.PERCENTAGE, (Math.round(percent*10000)/100.0d)+" %");
						}
						sleep(transferRefreshRate);
					} catch (InterruptedException e) {
						Logger.log(Logger.Status.ERR_THREAD, "Transfer rate update thread: "+e.getMessage());
					}
				}
			}
		}.start();
	}
	
	private void startDownloading(){
		dUnit.statusEnum = DownloadUnit.Status.DOWNLOADING;
		dUnit.setProperty(DownloadUnit.TableField.STATUS, "Downloading");
		/* create temporary file */
		File file = new File((String)dUnit.getProperty(DownloadUnit.TableField.FOLDER)+File.separator+(String)dUnit.getProperty(DownloadUnit.TableField.FILENAME));
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
		
		/* final update */
		long temptotal = 0;
		for(int i=0;i<dUnit.chunks.size();++i)
			temptotal += dUnit.chunks.get(i);
		dUnit.setProperty(DownloadUnit.TableField.DOWNLOADED, polishSize(temptotal, ""));
		if(dUnit.sizeLong>0){
			double percent = (double)temptotal/dUnit.sizeLong;
			downloaded = temptotal;
			
			dUnit.setProperty(DownloadUnit.TableField.PROGRESS, percent);
			dUnit.setProperty(DownloadUnit.TableField.PERCENTAGE, (Math.round(percent*10000)/100.0d)+" %");
		}
		
		downloading = false;
		
		if(numSegments>1 && okToMerge)
			mergeSegments();
		if(okToMerge)
			dUnit.setProperty(DownloadUnit.TableField.STATUS, "Completed");
	}
	
	/* Merge the downloaded segments into one file and deletes them */
	private void mergeSegments(){
		dUnit.setProperty(DownloadUnit.TableField.STATUS, "Merging");
		String fileName = (String)dUnit.getProperty(DownloadUnit.TableField.FILENAME);
		String filesavePath = (String)dUnit.getProperty(DownloadUnit.TableField.FOLDER) + File.separator + fileName;
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
		
		private boolean running = true, destroy = false;
		
		
		downloadSegment(int segNo, long startByte) {
			segmentName = (String)dUnit.getProperty(DownloadUnit.TableField.FILENAME) + ".part" + segNo;
			segConnection = makeConnection();
			this.startByte = startByte;
			this.segNo = segNo;
		}
		
		/* change running flag */
		public void pauseSegment(){
			running = false;
			okToMerge = false;
		}
		
		/* set the destroy flag, prevent concurrency issues */
		private void destroySegment(){
			running = false;
			okToMerge = false;
			destroy = true;
		}
		
		/* Thread downloads a particular range of file from server on an independent connection */
		public void run(){
			try{
				segConnection.setRequestMethod("GET");
				segConnection.setReadTimeout(readTimeout);
				if(dUnit.resumable){
					if(dUnit.sizeLong != -1)
						segConnection.setRequestProperty("Range", "bytes=" + startByte + "-" + (startByte-dUnit.chunks.get(segNo)+segmentSizes.get(segNo)-1));
					else
						segConnection.setRequestProperty("Range", "bytes="+dUnit.chunks.get(segNo)+"-");
				}
				
				InputStream inputStream = segConnection.getInputStream();
				/* Path where segment is saved */
				String segmentsavePath = Main.tempFolderPath + File.separator + segmentName;
				if(numSegments == 1){
					segmentsavePath = (String)dUnit.getProperty(DownloadUnit.TableField.FOLDER) + File.separator + (String)dUnit.getProperty(DownloadUnit.TableField.FILENAME);
				}
				
				/* append bytes to output stream */
				FileOutputStream outputStream;
				if((long)dUnit.chunks.get(segNo) > 0 && dUnit.statusEnum == DownloadUnit.Status.RESUMED)
					outputStream = new FileOutputStream(segmentsavePath, true);
				else
					outputStream = new FileOutputStream(segmentsavePath, false);
				
				/* number of bytes remaining to be read */
				long readRemaining = segmentSizes.get(segNo);
				int bytesRead = -1;
				byte[] buffer = new byte[bufferSize];
				Clock clock = new Clock();
				clock.startTimer();
				while((bytesRead = inputStream.read(buffer)) != -1){
					long lap = clock.getLapElapsedTime();
					if(!running)
						break;
					if(dUnit.sizeLong > 0){
						readRemaining -= bytesRead;				/* decrement remaining bytes to read */
						/* garbage if more bytes are read than needed */
						if(readRemaining < 0)
							bytesRead = (int)readRemaining+bytesRead;
					}
					downloaded += bytesRead;
					outputStream.write(buffer, 0, bytesRead);
					long update = dUnit.chunks.get(segNo) + bytesRead;
					dUnit.chunks.set(segNo, update);
					if(lap>0)
						segmentSpeeds.set(segNo, (double)(bytesRead*1000)/lap);	
				}
				
				outputStream.close();
				inputStream.close();
				if(destroy){
					try{
						(new File(segmentsavePath)).delete();	
					}catch(Exception e){
						Logger.log(Logger.Status.ERR_DESTROY, e.getMessage());
					}
				}
			}
			catch(SocketTimeoutException e){
				okToMerge = false;
				dUnit.statusEnum = DownloadUnit.Status.ERROR;
				dUnit.setProperty(DownloadUnit.TableField.STATUS, "Error");
				Logger.log(Logger.Status.ERR_READ, e.getMessage());
			}
			catch(Exception e){
				okToMerge = false;
				dUnit.statusEnum = DownloadUnit.Status.ERROR;
				dUnit.setProperty(DownloadUnit.TableField.STATUS, "Error");
				Logger.log(Logger.Status.ERR_CONN, e.getMessage());
			}finally{
				segConnection.disconnect();
			}
		}
	}
}
