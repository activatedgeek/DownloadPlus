import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

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
	public static String dumpPath = System.getProperty("user.home")+File.separator+"Downloads";
	
	@SuppressWarnings("unchecked")
	public static void buildAndDumpJSON(DownloadUnit dUnit){
		JSONObject jsonData = new JSONObject();
		
		/* populate standard download related info  */
		jsonData.put("uid", dUnit.getUID());
		jsonData.put("filename", dUnit.getProperty(DownloadUnit.TableField.FILENAME));
		jsonData.put("origin", dUnit.getProperty(DownloadUnit.TableField.ORIGIN));
		jsonData.put("url", dUnit.getProperty(DownloadUnit.TableField.URL));
		jsonData.put("folder", dUnit.getProperty(DownloadUnit.TableField.FOLDER));
		jsonData.put("size", dUnit.getProperty(DownloadUnit.TableField.SIZE));
		jsonData.put("status", dUnit.getProperty(DownloadUnit.TableField.STATUS));
		jsonData.put("segments", dUnit.getProperty(DownloadUnit.TableField.SEGMENTS));
		jsonData.put("resume", dUnit.getProperty(DownloadUnit.TableField.RESUME));
		jsonData.put("start", dUnit.getProperty(DownloadUnit.TableField.START));
		jsonData.put("scheduled", dUnit.getProperty(DownloadUnit.TableField.SCHEDULED));
		jsonData.put("finish", dUnit.getProperty(DownloadUnit.TableField.FINISH));
		
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
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_DUMP, e.getMessage());
		}
	}
	
	public static DownloadUnit loadDumpJSON(String path){
		JSONParser parser = new JSONParser();
		try{
			JSONObject jsonData = (JSONObject)parser.parse(new FileReader(path));
			DownloadUnit dUnit= new DownloadUnit(null);
			
			/* extract data from JSON and set download unit properties */
			dUnit.setUID((long)jsonData.get("uid"));
			dUnit.setProperty(DownloadUnit.TableField.FILENAME, jsonData.get("filename"));
			dUnit.setProperty(DownloadUnit.TableField.ORIGIN, jsonData.get("origin"));
			dUnit.setProperty(DownloadUnit.TableField.URL, jsonData.get("url"));
			dUnit.setProperty(DownloadUnit.TableField.FOLDER, jsonData.get("folder"));
			dUnit.setProperty(DownloadUnit.TableField.SIZE, jsonData.get("size"));
			dUnit.setProperty(DownloadUnit.TableField.STATUS, jsonData.get("status"));
			dUnit.setProperty(DownloadUnit.TableField.SEGMENTS, jsonData.get("segments"));
			dUnit.setProperty(DownloadUnit.TableField.RESUME, jsonData.get("resume"));
			dUnit.setProperty(DownloadUnit.TableField.START, jsonData.get("start"));
			dUnit.setProperty(DownloadUnit.TableField.SCHEDULED, jsonData.get("scheduled"));
			dUnit.setProperty(DownloadUnit.TableField.FINISH, jsonData.get("finish"));
			
			return dUnit;
		}catch(Exception e){
			Logger.log(Logger.Status.ERR_READ, e.getMessage());
			return null;
		}
	}
}
