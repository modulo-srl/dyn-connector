import modulo.srl.soap.DynSOAPConnector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


/**
 * Connector session callback
 */
class dynConnectorCallback implements DynSOAPConnector.Callback {
	private String sessionToken = null;

	/** Called when the connector get a new session token
	 *
	 * @param sessionToken Token to save
	 */
	public void setSessionToken(String sessionToken) {
		System.out.println("New session token: " + sessionToken);

		this.sessionToken = sessionToken;

		// Write to disk or database
		final String filePath = "../DynSession.txt";
		try {
			Files.write(Paths.get(filePath), this.sessionToken.getBytes(), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {}
	}

	/** Called when the connector needs last session token
	 *
	 * @return Last session token used
	 */
	public String getSessionToken() {
		if (this.sessionToken == null) {
			// Load from disk or database
			final String filePath = "../DynSession.txt";
			try {
				this.sessionToken = new String(Files.readAllBytes(Paths.get(filePath)));
			} catch (IOException e) {}
		}

		System.out.println("Get session token: " + this.sessionToken);
		return this.sessionToken;
	}
}


public class TestSOAP {

	public static void main(String[] args) {
		DynSOAPConnector soap = new DynSOAPConnector("pigrecoos.it", "test", "test", new dynConnectorCallback());

		// Enable debugging output (please disable in production)
		//soap.setDebug(true, true);

		String response = soap.send("echo", "<test><data>this is a test</data></test>");

		System.out.println("Server response: "+response);
	}

}
