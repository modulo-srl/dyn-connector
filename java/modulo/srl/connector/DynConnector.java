package modulo.srl.connector;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;


/**
 * Simple client class to send requests to a Dyn server
 *
 * @version 1.2
 * @author Modulo srl
 */
public class DynConnector {

	/**
	 * Connector session callback
	 */
	public interface Callback {

		/** Called when connector get new access token
		 *
		 * @param accessToken Access token to save
		 */
		void setAccessToken(String accessToken);

		/** Called when connector needs access token
		 *
		 * @return Access token string (can be null)
		 */
		String getAccessToken();
	}

	// Auth
	private String clientID;
	private String clientSecret;
	private String accessToken;

	// Connection
	private modulo.srl.connector.DynHTTPClient httpClient = null;

	private static final String DYN_BASE_URI = "api/";
	private static final String DYN_ACCESSTOKEN_URI = "auth/token";

	private Boolean debug;

	private Callback accessTokenCallback;


	/** Create a Dyn connector
	 *
	 * @param host Server host (ex: "subdomain.domain.com")
	 * @param clientID API client ID
	 * @param clientSecret API client Secret
	 * @param accessTokenCallback Access token callback class (DynConnector.Callback)
	 */
	public DynConnector(String host, String clientID, String clientSecret, Callback accessTokenCallback) {
		this.debug = false;

		if (!host.isEmpty() && (host.charAt(host.length()-1) != '/'))
			host += "/";

		String serverHost = "https://" + host + DYN_BASE_URI;

		this.clientID = clientID;
		this.clientSecret = clientSecret;

		try {
			this.httpClient = new modulo.srl.connector.DynHTTPClient(serverHost);
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		}

		this.accessTokenCallback = accessTokenCallback;

		if (this.accessTokenCallback != null) {
			this.accessToken = this.accessTokenCallback.getAccessToken();
			if (this.accessToken == null)
				this.accessToken = "";
		} else
			this.accessToken = "";
	}

	/** Send a Dyn request for a specific operation
	 *
	 * If the operation needs auth, and current session is not logged-in, try to perform Auth automatically
	 *
	 * @param operation The operation to perform
	 * @param jsonRequest JSON format
	 * @return DynResponse
	 */
	public String send(String operation, String jsonRequest) {
		DynResponse dynResponse;

		dynResponse = this.sendRequest(operation, jsonRequest);
		
		if (!dynResponse.success()) {
			// There are an error

			if (dynResponse.getErrorID().equals("unauthorized")) {
				// Authentication needed

				dynResponse = this.doAuth();

				if (dynResponse.success()) {
					// Retry request
					dynResponse = this.sendRequest(operation, jsonRequest);

					if (!dynResponse.success()) {
						if (this.debug)
							System.out.println("DynConnector: ERROR: " + dynResponse.getErrorDesc());
					}
				}

			} else {
				if (this.debug)
					System.out.println("DynConnector: ERROR: " + dynResponse.getErrorDesc());
			}
		}

		return dynResponse.getResponse();
	}

	/** Enable or disable debugging output
	 *
	 * @param enable Enable or disable debugging
	 * @param low_level Enable low level debugging too
	 */
	public void setDebug(Boolean enable, Boolean low_level) {
		this.debug = enable;
		this.httpClient.setDebug(low_level);
	}

	/** Perform Auth
	 *
	 * @return DynResponse
	 */
	private DynResponse doAuth() {
		Map<String, String> req = new HashMap<String, String>();
		req.put("grant_type", "client_credentials");
		req.put("client_id", this.clientID);
		req.put("client_secret", this.clientSecret);

		String jsonReq = JSONparser.toJSON(req);

		DynResponse dynResponse = this.sendRequest(DYN_ACCESSTOKEN_URI, jsonReq);

		if (dynResponse.success()) {
			String respJson = dynResponse.getResponse();

			JSONparser parser = new JSONparser(respJson);

			String s = parser.getString("access_token");
			if (s == null)
				return new DynResponse("", "general error", "JSON response not parsable");

			this.accessToken = s;

			if (this.accessTokenCallback != null)
				this.accessTokenCallback.setAccessToken(this.accessToken);

		} else {
			if (this.debug)
				System.out.println("DynConnector: ERROR: " + dynResponse.getError());
		}

		return dynResponse;
	}

	class jsonError {
		public String errorID;
		public String errorDescription;
	}

	/** Send low level request
	 *
	 * Adds accessToken to header when operation is not "auth"
	 *
	 * @param operation Operation to perform
	 * @param jsonRequest JSON string
	 * @return DynResponse
	 */
	private DynResponse sendRequest(String operation, String jsonRequest) {
		DynResponse dynResponse;

		String token = "";
		if (!operation.equals(DYN_ACCESSTOKEN_URI))
			token = this.accessToken;

		try {
			if (this.debug)
				System.out.println("DynConnector: request: " + operation + " " + jsonRequest);

			HTTPResponse httpResponse = this.httpClient.send(operation, token, jsonRequest);

			int statusCode = httpResponse.getStatusCode();
			if (statusCode == 200)
				dynResponse = new DynResponse(httpResponse.getBody(), "", "");
			else {
				String jsonResp = httpResponse.getBody();

				JSONparser parser = new JSONparser(jsonResp);

				String s = parser.getString("error");
				if (s == null)
					return new DynResponse("", "general error", "JSON response not parsable");
	
				dynResponse = new DynResponse(
					jsonResp, 
					s, 
					"["+String.valueOf(statusCode)+"] " + parser.getString("error_description")
				);

			}

		} catch (IOException e) {

			if (this.debug)
				System.out.println("DynConnector: ERROR: " + e.getMessage());

			dynResponse = new DynResponse("", "general error", e.getMessage());
		}

		if (this.debug)
			System.out.println("DynConnector: response: " + dynResponse.getResponse());

		return dynResponse;
	}
}

/** 
 * Dyn response object
 */
class DynResponse {

	private final String errorID;
	private final String errorDesc;
	private final String jsonResponse;

	DynResponse(String jsonResponse, String errorID, String errorDesc) {
		this.jsonResponse = jsonResponse;
		this.errorID = errorID;
		this.errorDesc = errorDesc;
	}

	public Boolean success() {
		return (this.errorID.isEmpty());
	}

	public String getResponse() {
		return this.jsonResponse;
	}

	public String getErrorID() {
		return this.errorID;
	}

	public String getErrorDesc() {
		return this.errorDesc;
	}

	public String getError() {
		return "["+this.errorID+"] "+this.errorDesc;
	}
}
