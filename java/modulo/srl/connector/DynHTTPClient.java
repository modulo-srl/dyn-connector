package modulo.srl.connector;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.net.MalformedURLException;
import java.io.IOException;


/**
 * Simple HTTP client class to send requests to a server.
 *
 * @version 1.0
 * @author Modulo srl
 */
public class DynHTTPClient {
    
	// Server host
	private String serverHost;

	private Boolean debug;

	/**
	* Create a connection
	*
	* @param url Server url (ex: "https://subdomain.domain.com/api")
	*/
	DynHTTPClient(String url) {
		if (!url.isEmpty() && (url.charAt(url.length()-1) != '/'))
			url += "/";

		this.serverHost = url;

		this.debug = false;
	}

	/** Enable or disable debugging output
	 *
	 * @param enable Enable or disable debugging
	 */
	public void setDebug(Boolean enable) {
		this.debug = enable;
	}

	/**
     * Send a request for a specific operation.
     * 
     * @param operation Operation to perform
	 * @param accessToken Access token (can be blank)
     * @param body Body of the message (JSON format, can be null)
     * @return Response from the server
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
     */
	public HTTPResponse send(String operation, String accessToken, String body)
		throws MalformedURLException, IOException {
		
		URL url = new URL(this.serverHost+operation);
		
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
	
        conn.setRequestProperty("Accept", "application/json");
		if (!accessToken.isEmpty())
			conn.setRequestProperty("authorization", "Bearer "+accessToken);

		conn.setDoOutput(true);

		if (this.debug)
			System.out.println("DynHTTPClient: operation '"+operation+"': "+body);

		DataOutputStream os = new DataOutputStream(conn.getOutputStream());
		os.writeBytes(body);
		os.flush();
		os.close();

		int responseCode = 0;
		try{
			responseCode = conn.getResponseCode();
		} catch (IOException e) {
			// HttpUrlConnection will throw an IOException if any 4XX
			// response is sent. If we request the status again, this
			// time the internal status will be properly set, and we'll be
			// able to retrieve it.
			responseCode = conn.getResponseCode();
		}

		if (this.debug)
			System.out.println("DynHTTPClient: response code: " + responseCode);

		String output = "";

		java.io.InputStreamReader stream;

		if (responseCode >= 400 && responseCode <= 599)
			stream = new InputStreamReader(conn.getErrorStream());
		else 
			stream = new InputStreamReader(conn.getInputStream());

		BufferedReader br = new BufferedReader(stream);

		String s;
		while ((s = br.readLine()) != null)
			output += s;

		if (this.debug)
        	System.out.println("DynHTTPClient: response body: " + output);

		conn.disconnect();

		return new HTTPResponse(responseCode, output);
	}
    
}

/**
 * HTTP response with status code and body.
 */
class HTTPResponse {

	private int statusCode;
	private String body;

	public HTTPResponse(int statusCode, String body) {
		this.statusCode = statusCode;
		this.body = body;
	}

	public int getStatusCode() {
		return this.statusCode;
	}

	public String getBody() {
		return this.body;
	}
}
