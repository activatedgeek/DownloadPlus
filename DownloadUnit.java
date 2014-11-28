
import java.time.LocalDateTime;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;

public class DownloadUnit extends Task<Void>   {
	public enum TableField {
		FILE_NAME,SIZE,TRANSFER_RATE,PERCENT_DOWNLOADED,FILE_DOWNLOADED,
		STATUS
	}

    private static final int NUM_ITERATIONS = 100;
	private String fileName = null, origin = null, url = null, folder = null;
    private String size = null, status = null;
    private int segments;
    private boolean resume;
    private LocalDateTime start, scheduled, elapsed, finish;
    //TODO: save chunks list
    
    private String transferRate = null;

    public DownloadUnit (String url){
    	origin = url;
    	fileName = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
    	this.fileName = fileName;
    	//TODO: update the GUI
    }
    
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		//TODO: update GUI
	}
	
	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
		//TODO: update GUI
	}

	public String getTransferRate() {
		return transferRate;
	}

	public void setTransferRate(String transferRate) {
		this.transferRate = transferRate;
		//TODO: update GUI
	}
	
	public void update(TableField field,Object value) {	
		switch(field) {
		case FILE_NAME:
			this.setFileName((String)value);
		
		case SIZE:
			this.setSize((String)value);
			break;
			
		case FILE_DOWNLOADED:
			this.setSize((String)value);
			break;
					
		case TRANSFER_RATE:
			this.setTransferRate((String)value);
			break;
		
		case STATUS:
			this.setStatus((String)value);
			break;
			
		case PERCENT_DOWNLOADED:
			//update the progress bar
			this.updateProgressBar((int)value);
			break;
			
		default:
			break;
		}
		
		
	}

		
	public void updateProgressBar(int percentage) {
		
		this.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
		
		if(percentage==0) {
			this.updateMessage("Waiting...");
			this.updateProgress(0,1);
		}
	    if (percentage>0 && percentage<100) {  
	    	this.updateMessage("Running...");
	    //Thread.sleep(2000);
        //this.updateMessage("Running...");
	      //  for (int i = 0; i < NUM_ITERATIONS; i++) {
	        updateProgress((1.0 * percentage) / NUM_ITERATIONS, 1);
	      //    Thread.sleep(400);s
        }
	    else if (percentage==100) {
	    	this.updateMessage("Done");
	    	this.updateProgress(1, 1);
	    }
	}

	@Override
	protected Void call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
    
}