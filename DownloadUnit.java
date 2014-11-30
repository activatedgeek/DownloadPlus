import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class DownloadUnit /*extends Task<Void>*/ {
	public enum TableField {
		FILENAME,ORIGIN,URL,FOLDER,SIZE,STATUS,TRANSFER_RATE,
		PROGRESS,SEGMENTS,RESUME,START,SCHEDULED,FINISH
	}
	public enum Status{
		QUEUED, SCHEDULED, PAUSED, 
		DOWNLOADING, COMPLETED, ERROR
	}
	
	private long uid;
	public Status statusEnum;
	public long sizeLong;
	public List<Long> chunks;
	
	private SimpleStringProperty filename;
	private SimpleStringProperty origin;
	private SimpleStringProperty url;
	private SimpleStringProperty folder;
	private SimpleStringProperty size;
	private SimpleStringProperty status;
	private SimpleStringProperty transferRate;
	private SimpleDoubleProperty progress;
    private SimpleIntegerProperty segments;
    private SimpleBooleanProperty resume;
    private SimpleObjectProperty<LocalDateTime> start, scheduled, finish;

    public DownloadUnit (String uri){
    	statusEnum = Status.QUEUED;
    	chunks = new ArrayList<Long>();
    	
    	filename = new SimpleStringProperty(uri);
    	origin = new SimpleStringProperty(uri);
    	url = new SimpleStringProperty(uri);
    	folder = new SimpleStringProperty(System.getProperty("user.home")+File.separator+"/Downloads");
    	size = new SimpleStringProperty("0 B");
    	status = new SimpleStringProperty("Waiting");
    	transferRate = new SimpleStringProperty("0 Bps");
    	progress = new SimpleDoubleProperty(0);
    	segments = new SimpleIntegerProperty(0);
    	resume = new SimpleBooleanProperty(false);
    	start = new SimpleObjectProperty<LocalDateTime>();
    	scheduled = new SimpleObjectProperty<LocalDateTime>();
    	finish = new SimpleObjectProperty<LocalDateTime>();
    }
    
    public void setUID(long uid){
    	this.uid = uid;
    }
    
    public long getUID(){
    	return uid;
    }

    public Object getProperty(TableField tf){
    	switch(tf){
    	case FILENAME:
    		return filename.get();
    	case ORIGIN:
    		return origin.get();
    	case URL:
    		return url.get();
    	case FOLDER:
    		return folder.get();
    	case SIZE:
    		return size.get();
    	case STATUS:
    		return status.get();
    	case TRANSFER_RATE:
    		return transferRate.get();
    	case PROGRESS:
    		return progress.get();
    	case SEGMENTS:
    		return segments.get();
    	case RESUME:
    		return resume.get();
    	case START:
    		return start.get();
    	case SCHEDULED:
    		return scheduled.get();
    	case FINISH:
    		return finish.get();
    	}
    	return null;
    }

    public void setProperty(TableField tf, Object value){
    	switch(tf){
    	case FILENAME:
    		filename.set((String)value);
    		break;
    	case ORIGIN:
    		origin.set((String)value);
    		break;
    	case URL:
    		url.set((String)value);
    		break;
    	case FOLDER:
    		folder.set((String)value);
    		break;
    	case SIZE:
    		size.set((String)value);
    		break;
    	case STATUS:
    		status.set((String)value);
    		break;
    	case TRANSFER_RATE:
    		transferRate.set((String)value);
    		break;
    	case PROGRESS:
    		progress.set((Double)value);
    		break;
    	case SEGMENTS:
    		segments.set((Integer)value);
    		break;
    	case RESUME:
    		resume.set((boolean)value);
    		break;
    	case START:
    		start.set((LocalDateTime)value);
    		break;
    	case SCHEDULED:
    		scheduled.set((LocalDateTime)value);
    		break;
    	case FINISH:
    		finish.set((LocalDateTime)value);
    		break;
    	default:
    		break;
    	}
    }
    
    public StringProperty filenameProperty(){
    	return filename;
    }
    

    public StringProperty sizeProperty(){
    	return size;
    }

    public StringProperty statusProperty(){
    	return status;
    }

    public StringProperty transferRateProperty(){
    	return transferRate;
    }
    
    /*
	public void updateProgressBar(double percentage) {
		this.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
		
		if(percentage == 0) {
			this.updateProgress(0,1);
		}
		else if (percentage > 0 && percentage < 100) {
	        updateProgress((1.0 * percentage) / 100, 1);
        }
	    else if (percentage >= 100) {
	    	this.updateProgress(1, 1);
	    }
	}

	@Override
	protected Void call() throws Exception {
	      this.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
	      this.updateMessage("Waiting...");
	      Thread.sleep(1000);
	      this.updateMessage("Running...");
	      for (int i = 0; i < 100; i++) {
	        updateProgress((1.0 * i) / 100, 1);
	        Thread.sleep(100);
	      }
	      this.updateMessage("Done");
	      this.updateProgress(1, 1);
	      return null;
	}
	*/
    
}