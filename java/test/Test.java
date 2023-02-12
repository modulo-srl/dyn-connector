import modulo.srl.connector.DynConnector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.Map;
import java.util.HashMap;
import modulo.srl.connector.JSONparser;


/**
 * Connector session callback implementation example.
 *
 * NOTE: YOU SHOULD ASSURE TO LOAD/SAVE THE TOKEN
 * USING PERSISTENT STORAGE (disk file, Database, cache system...),
 * HANDLING ANY EXCEPTIONS.
 *
 */
class dynConnectorCallback implements DynConnector.Callback {
	private String accessToken = null;

	/** Called when the connector get a new access token
	 *
	 * @param accessToken Token to save
	 */
	public void setAccessToken(String accessToken) {
		System.out.println("Test: new access token: " + accessToken);

		this.accessToken = accessToken;

		// The token must to be written to disk or database
		final String filePath = "../DynSession.txt";
		try {
			Files.write(Paths.get(filePath), this.accessToken.getBytes(), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {}
	}

	/** Called when the connector needs last access token
	 *
	 * @return Last access token used
	 */
	public String getAccessToken() {
		if (this.accessToken == null) {
			// The token must to be loaded from disk or database
			final String filePath = "../DynSession.txt";
			try {
				this.accessToken = new String(Files.readAllBytes(Paths.get(filePath)));
			} catch (IOException e) {}
		}

		System.out.println("Test: get access token: " + this.accessToken);

		return this.accessToken;
	}
}


public class Test {

	public static void main(String[] args) {
		DynConnector conn = new DynConnector(
			"pigrecoos.it", 
			"email address", 
			"api secret", 
			new dynConnectorCallback()
		);

		// Enable debugging output (please disable in production)
		conn.setDebug(true, false);

		Map<String, String> req = new HashMap<String, String>();
		req.put("key1", "test1");
		req.put("key2", "test2");
		// NOTE: you can use any other library to serialize objects to JSON
		String jsonReq = JSONparser.toJSON(req);

		//String response = conn.send("test/echo", jsonReq);
		String response = conn.send("auth/test/echo", jsonReq);

		System.out.println("Test: server response: "+response);
	}

}
