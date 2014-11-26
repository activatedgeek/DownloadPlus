
public class Downloader {
	public Downloader(String url) {
		URIExplore uri = new URIExplore(url);
		Logger.debug(uri.finalURI);
	}
}
