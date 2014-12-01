import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/*
 * Data Storage Specification (stored as ID.json)
 * 		FileName: "filename" []
 * 		Original input URL: "origin" [String]
 * 		Resolved URL after all redirects: "url" [String]
 * 		Destination folder: "folder" [String]
 * 		Total Size (in bytes): "size" [Integer]
 * 		Number of segments: "segments" [Integer]
 * 		Resumable: "resume" [Yes/No]
 * 		Queue entry time: "start" [Timestamp]
 * 		Time elapsed from first downloaded byte: "elapsed" [Timestamp]
 * 		Finished Time: "finish" [Timestamp]
 * 		Scheduled Time: "scheduled" [Timestamp]
 * 		Status: "status" ["resumable", "non-resumable", "complete", "failed", "waiting"]
 * 		Completed Chunks: "chunks" [Range String List: "start-end"]
 */
public class JSON {
	public static String dumpPath = System.getProperty("user.home")+File.separator+".downloadPlusPlus"+File.separator+"data";
	static{
		File dump = new File(dumpPath);
		if(!dump.isDirectory())
			dump.mkdirs();
	}
	
	@SuppressWarnings("unchecked")
	public static void dumpDownload(DownloadUnit dUnit){
		JSONObject jsonData = new JSONObject();
		
		/* populate static (one-time set) download related info  */
		jsonData.put("uid", dUnit.getUID());
		jsonData.put("size", dUnit.sizeLong);
		jsonData.put("resume", dUnit.resumable);
		
		jsonData.put("filename", dUnit.getProperty(DownloadUnit.TableField.FILENAME));
		jsonData.put("origin", dUnit.getProperty(DownloadUnit.TableField.ORIGIN));
		jsonData.put("url", dUnit.getProperty(DownloadUnit.TableField.URL));
		jsonData.put("folder", dUnit.getProperty(DownloadUnit.TableField.FOLDER));
		jsonData.put("type", dUnit.getProperty(DownloadUnit.TableField.TYPE));
		jsonData.put("segments", dUnit.getProperty(DownloadUnit.TableField.SEGMENTS));
		
		/* poulate dynamic download properties */
		jsonData.put("status", dUnit.getProperty(DownloadUnit.TableField.STATUS));
		jsonData.put("statusEnum", dUnit.statusEnum.toString());
		jsonData.put("chunks", dUnit.chunks);
		
		try{
			File dataDump = new File(dumpPath+File.separator+dUnit.getUID()+".data");
			if(!dataDump.exists()){
				dataDump.createNewFile();
			}
			FileWriter fw = new FileWriter(dataDump.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			/* convert data into JSON string */
			bw.write(jsonData.toJSONString());
			
			bw.close();
			fw.close();
			Logger.debug("Dumped into "+dataDump.getAbsolutePath());
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_DUMP, e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static DownloadUnit loadDumpDownload(String path){
		JSONParser parser = new JSONParser();
		try{
			JSONObject jsonData = (JSONObject)parser.parse(new FileReader(path));
			DownloadUnit dUnit= new DownloadUnit(null);
			
			/* static download properties */
			dUnit.setUID((long)jsonData.get("uid"));
			dUnit.sizeLong = (long)jsonData.get("size");
			dUnit.resumable = (boolean)jsonData.get("resume");
			
			dUnit.setProperty(DownloadUnit.TableField.FILENAME, jsonData.get("filename"));
			dUnit.setProperty(DownloadUnit.TableField.ORIGIN, jsonData.get("origin"));
			dUnit.setProperty(DownloadUnit.TableField.URL, jsonData.get("url"));
			dUnit.setProperty(DownloadUnit.TableField.FOLDER, jsonData.get("folder"));
			dUnit.setProperty(DownloadUnit.TableField.SIZE, polishSize(dUnit.sizeLong));
			dUnit.setProperty(DownloadUnit.TableField.TYPE, jsonData.get("type"));
			dUnit.setProperty(DownloadUnit.TableField.SEGMENTS, jsonData.get("segments"));
			
			String resumeCap = "No";
			if(dUnit.resumable)
				resumeCap = "Yes";
			dUnit.setProperty(DownloadUnit.TableField.RESUME, resumeCap);
			
			/* set dynamic properties */
			dUnit.setProperty(DownloadUnit.TableField.STATUS, jsonData.get("status"));
			dUnit.statusEnum = DownloadUnit.Status.valueOf((String)jsonData.get("statusEnum"));
			dUnit.chunks = (List<Long>) jsonData.get("chunks");
			long downloaded = 0;
			for(int i=0; i<dUnit.chunks.size(); ++i)
				downloaded += dUnit.chunks.get(i);
			dUnit.setProperty(DownloadUnit.TableField.DOWNLOADED, polishSize(downloaded));
			
			double percent = (double)downloaded/dUnit.sizeLong;
			dUnit.setProperty(DownloadUnit.TableField.PROGRESS, percent);
			dUnit.setProperty(DownloadUnit.TableField.PERCENTAGE, (Math.round(percent*10000)/100.0d)+" %");
			
			/*
			dUnit.setProperty(DownloadUnit.TableField.START, jsonData.get("start"));
			dUnit.setProperty(DownloadUnit.TableField.SCHEDULED, jsonData.get("scheduled"));
			dUnit.setProperty(DownloadUnit.TableField.FINISH, jsonData.get("finish"));
			*/
			
			return dUnit;
		}catch(Exception e){
			e.printStackTrace();
			Logger.log(Logger.Status.ERR_READ, e.getMessage());
			return null;
		}
	}
	
	/* returns a string with suitable units, precision = 2  */
	private static String polishSize(long sizeBytes){
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
		return polishedSize+" "+unit;
	}
}
