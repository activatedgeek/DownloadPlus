import org.json.simple.JSONObject;

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
	/* FIXME: suppressing warning for generic object types */
	@SuppressWarnings("unchecked")
	public static int buildData(){
		JSONObject jsonData = new JSONObject();
		jsonData.put("hey", "there");
		return 0;
	}
}
