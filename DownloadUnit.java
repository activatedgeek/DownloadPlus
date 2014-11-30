import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.concurrent.Task;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class DownloadUnit extends Task<Void> {
	public enum TableField {
		FILENAME,ORIGIN,URL,FOLDER,SIZE,TYPE,STATUS,
		TRANSFER_RATE,PROGRESS,PERCENTAGE,SEGMENTS,
		RESUME,START,SCHEDULED,FINISH,DOWNLOADED
	}
	public enum Status{
		QUEUED, SCHEDULED, PAUSED, 
		DOWNLOADING, COMPLETED, ERROR,
		RESUMED
	}
	
	/* download related static information */
	private long uid;
	public long sizeLong;
	public boolean resumable;
	
	private SimpleStringProperty filename;
	private SimpleStringProperty origin;
	private SimpleStringProperty url;
	private SimpleStringProperty folder;
	private SimpleStringProperty size;
	private SimpleStringProperty type;
    private SimpleIntegerProperty segments;
    private SimpleStringProperty resumeCap;
    //private SimpleObjectProperty<LocalDateTime> start, scheduled, finish;
	
    /* download related dynamic information */
	public Status statusEnum;
	public List<Long> chunks;				/* current downloaded bytes in each segment */
	
	private SimpleStringProperty status;
	private SimpleStringProperty transferRate;
    private SimpleStringProperty downloaded;
	private SimpleDoubleProperty progress;
	private SimpleStringProperty percentage;

    public DownloadUnit (String uri){
    	/* setting default static elements */
    	filename = new SimpleStringProperty(uri);
    	origin = new SimpleStringProperty(uri);
    	url = new SimpleStringProperty(uri);
    	folder = new SimpleStringProperty(System.getProperty("user.home")+File.separator+"/Downloads");
    	size = new SimpleStringProperty("--");
    	type = new SimpleStringProperty("--");
    	segments = new SimpleIntegerProperty(1);
    	resumeCap = new SimpleStringProperty("No");
    	
    	/* setting default dynamic elements */
    	statusEnum = Status.QUEUED;
    	resumable = false;
    	chunks = new ArrayList<Long>();
    	
    	status = new SimpleStringProperty("Queued");
    	transferRate = new SimpleStringProperty("--");
    	progress = new SimpleDoubleProperty(0);
    	percentage = new SimpleStringProperty("0.0 %");
    	downloaded = new SimpleStringProperty("0 B");
    	
    	/*
    	start = new SimpleObjectProperty<LocalDateTime>();
    	scheduled = new SimpleObjectProperty<LocalDateTime>();
    	finish = new SimpleObjectProperty<LocalDateTime>();
    	*/
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
    	case TYPE:
    		return type.get();
    	case STATUS:
    		return status.get();
    	case TRANSFER_RATE:
    		return transferRate.get();
    	case PROGRESS:
    		return progress.get();
    	case PERCENTAGE:
    		return percentage.get();
    	case SEGMENTS:
    		return segments.get();
    	case RESUME:
    		return resumeCap.get();
    	case DOWNLOADED:
    		return downloaded.get();
    	/*
    	case START:
    		return start.get();
    	case SCHEDULED:
    		return scheduled.get();
    	case FINISH:
    		return finish.get();
    	*/
    	default:
    		return null;
    	}
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
    	case TYPE:
    		type.set((String)value);
    		break;
    	case STATUS:
    		status.set((String)value);
    		break;
    	case TRANSFER_RATE:
    		transferRate.set((String)value);
    		break;
    	case DOWNLOADED:
    		downloaded.set((String)value);
    		break;
    	case PROGRESS:
    		progress.set((Double)value);
    		updateProgress((Double)value, 1);
    		break;
    	case PERCENTAGE:
    		percentage.set((String)value);
    		break;
    	case SEGMENTS:
    		segments.set((Integer)value);
    		break;
    	case RESUME:
    		resumeCap.set((String)value);
    		break;
    	/*
    	case START:
    		start.set((LocalDateTime)value);
    		break;
    	case SCHEDULED:
    		scheduled.set((LocalDateTime)value);
    		break;
    	case FINISH:
    		finish.set((LocalDateTime)value);
    		break;
    	*/
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
    
    public StringProperty resumeCapProperty(){
    	return resumeCap;
    }
    
    public StringProperty downloadedProperty(){
    	return downloaded;
    }
    
    public StringProperty percentageProperty(){
    	return percentage;
    }

	@Override
	protected Void call() throws Exception {
		return null;
	}
    
}
