package modulo.srl.soap;

import modulo.srl.utils.DynXMLUtils;
import java.io.IOException;
import javax.xml.soap.SOAPException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;


/**
 * Simple SOAP client class to send requests to a Dyn SOAP Server
 *
 * @version 1.1
 * @author Modulo srl
 */
public class DynSOAPConnector {

	/**
	 * Connector session callback
	 */
	public interface Callback {

		/** Called when connector get new session token
		 *
		 * @param sessionToken Session token to save
		 */
		void setSessionToken(String sessionToken);

		/** Called when connector needs session token
		 *
		 * @return Session token string (can be null)
		 */
		String getSessionToken();
	}

	// Auth
	private String authUID;
	private String masterToken;
	private String sessionToken;

	// SOAP connection
	private modulo.srl.soap.DynSOAPClient dynSoapClient = null;

	private static final String DYN_BASE_URI = "api/";

	private Boolean debug;

	private Callback sessionTokenCallback;


	/** Create a Dyn connector
	 *
	 * @param host Server host (ex: "subdomain.domain.com")
	 * @param authUID API Auth ID
	 * @param masterToken API Master Token
	 * @param sessionTokenCallback Session token callback class (DynSOAPConnector.Callback)
	 */
	public DynSOAPConnector(String host, String authUID, String masterToken, Callback sessionTokenCallback) {
		this.debug = false;

		if (!host.isEmpty() && (host.charAt(host.length()-1) != '/'))
			host += "/";

		String serverHost = "https://" + host + DYN_BASE_URI;

		this.authUID = authUID;
		this.masterToken = masterToken;

		try {
			this.dynSoapClient = new modulo.srl.soap.DynSOAPClient(serverHost);
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		}

		this.sessionTokenCallback = sessionTokenCallback;

		if (this.sessionTokenCallback != null) {
			this.sessionToken = this.sessionTokenCallback.getSessionToken();
			if (this.sessionToken == null)
				this.sessionToken = "";
		} else
			this.sessionToken = "";
	}

	/** Send a Dyn request for a specific operation
	 *
	 * If the operation needs auth, and current session is not logged-in, try to perform Auth automatically
	 *
	 * @param operation The operation to perform
	 * @param xmlRequest XML format
	 * @return DynResponse
	 */
	public String send(String operation, String xmlRequest) {
		DynResponse dynResponse;

		dynResponse = this.sendRequest(operation, xmlRequest);

		if (!dynResponse.success()) {
			// There are an error

			if (dynResponse.getErrorCode() == 70) {
				// Authentication needed, try to login
				dynResponse = this.doAuth();

				if (dynResponse.success()) {
					// Retry request
					dynResponse = this.sendRequest(operation, xmlRequest);

					if (!dynResponse.success()) {
						if (this.debug)
							System.out.println("DYN ERROR: " + dynResponse.getErrorReason());
					}
				}

			} else {
				if (this.debug)
					System.out.println("DYN ERROR: " + dynResponse.getErrorReason());
			}
		}

		return dynResponse.getResponse();
	}

	/** Enable or disable debugging output
	 *
	 * @param enable Enable or disable debugging
	 * @param low_level Enable low level debugging too
	 * @param additionalCookies String contains raw cookies to pass to every HTTP call
	 */
	public void setDebug(Boolean enable, Boolean low_level, String additionalCookies) {
		this.debug = enable;
		this.dynSoapClient.setDebug(low_level, additionalCookies);
	}

	/** Enable or disable debugging output
	 *
	 * @param enable Enable or disable debugging
	 * @param low_level Enable low level debugging too
	 */
	public void setDebug(Boolean enable, Boolean low_level) {
		this.setDebug(enable, low_level, "");
	}

	/** Perform Auth
	 *
	 * @return DynResponse
	 */
	private DynResponse doAuth() {
		String authXML =
				"<uid>" + this.authUID + "</uid>" +
				"<master_token>" + this.masterToken + "</master_token>";

		DynResponse dynResponse = this.sendRequest("auth", authXML);

		if (dynResponse.success()) {
			String response = dynResponse.getResponse();

			if (DynXMLUtils.getXMLNodeContent(response, "auth").equals("true")) {
				this.sessionToken = DynXMLUtils.getXMLNodeContent(response, "session_token");

				if (this.sessionTokenCallback != null)
					this.sessionTokenCallback.setSessionToken(this.sessionToken);
			} else
				dynResponse = new DynResponse("<error><code>-1</code><reason>invalid response</reason></error>");

		} else {
			if (dynResponse.getErrorCode() == 71) {
				// Authentication failed

				if (this.debug)
					System.out.println("DYN ERROR: " + dynResponse.getErrorReason());
			}
		}

		return dynResponse;
	}

	/** Send low level request
	 *
	 * Adds sessionToken to header when operation is not "auth"
	 *
	 * @param operation Operation to perform
	 * @param xmlRequest XML string
	 * @return DynResponse
	 */
	private DynResponse sendRequest(String operation, String xmlRequest) {
		DynResponse dynResponse;

		try {
			String header = null;

			if (!operation.equals("auth")) {
				if (this.sessionToken.length() > 0)
					header = "<session_token>" + sessionToken + "</session_token>";
			}

			if (this.debug)
				System.out.println("Request: " + operation + " " + xmlRequest);

			String response = this.dynSoapClient.send(operation, header,"<Request>" + xmlRequest + "</Request>");
			dynResponse = new DynResponse(response);

		} catch (IOException | SOAPException | SAXException | ParserConfigurationException e) {

			if (this.debug)
				System.out.println(e);

			dynResponse = new DynResponse("<error><code>-1</code><reason>"+e.getMessage()+"</reason></error>");
		}

		if (this.debug)
			System.out.println("Response: " + dynResponse.getResponse());

		return dynResponse;
	}

}

/** Dyn response object
 */
class DynResponse {
	private final int errorCode;
	private final String errorReason;
	private final String xmlResponse;

	DynResponse(String xmlResponse) {

		if ((xmlResponse == null) || (xmlResponse.length() == 0)) {
			this.errorCode = -1;
			this.errorReason = "empty response";
			this.xmlResponse = xmlResponse;

		} else {
			String response = DynXMLUtils.getXMLNodeContent(xmlResponse, "Response");

			if (response.length() == 0) {
				this.xmlResponse = xmlResponse;
				this.errorCode = -1;
				this.errorReason = "malformed response";

			} else {
				this.xmlResponse = response;
				int pos = response.indexOf(">");
				if (pos > 0) {
					System.out.println(response);
					String firstTag = response.substring(0, pos).trim().substring(1);

					if (firstTag.equals("error")) {
						String errCodeStr = DynXMLUtils.getXMLNodeContent(response, "code");

						if (errCodeStr.length() > 0) {
							this.errorCode = Integer.parseInt(errCodeStr);
							this.errorReason = DynXMLUtils.getXMLNodeContent(response, "reason");
						} else {
							this.errorCode = -1;
							this.errorReason = "malformed response";
						}

					} else {
						// Valid response
						this.errorCode = 0;
						this.errorReason = "";
					}

				} else {
					this.errorCode = -1;
					this.errorReason = "malformed response";
				}
			}
		}
	}

	public Boolean success() {
		return (this.errorCode == 0);
	}

	public String getResponse() {
		return this.xmlResponse;
	}

	public int getErrorCode() {
		return this.errorCode;
	}

	public String getErrorReason() {
		return this.errorReason;
	}

}
