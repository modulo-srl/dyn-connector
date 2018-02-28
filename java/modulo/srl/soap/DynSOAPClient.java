package modulo.srl.soap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Simple SOAP client class to send requests to a SOAP 1.1 Server, without using WSDL
 *
 * @version 1.1
 * @author Modulo srl
 */
public class DynSOAPClient {

	// SOAP server host
	private String serverHost;

	// SOAP connection
	private SOAPConnection soapConnection = null;

	private Boolean debug;
	private String debug_cookies;

	// Further namespace to add to the header
	//private static final String PREFIX_NAMESPACE = "ns";
	//private static final String NAMESPACE = "http://namespace.to.add.to.header";

	/**
	* Create a SOAP connection
	*
	* @param url Server url (ex: "https://subdomain.domain.com/soap")
	*/
	DynSOAPClient(String url) {
		this.serverHost = url;

		this.debug = false;
		this.debug_cookies = "";

		try {
			createSOAPConnection();
		} catch (UnsupportedOperationException | SOAPException e) {
			e.printStackTrace();
		}
	}

    /**
     * Send a SOAP request for a specific operation
     * 
     * @param operation The operation to perform
	 * @param xmlHeader Body of the SOAP message (XML format, can be null)
     * @param xmlBody Body of the SOAP message (XML format, can be null)
     * @return Response from the server
     * @throws SOAPException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
	public String send(String operation, String xmlHeader, String xmlBody)
		throws SOAPException, SAXException, IOException, ParserConfigurationException {

		SOAPElement soapHeader;
		if (xmlHeader != null)
			soapHeader = stringToSOAPElement(xmlHeader);
		else
			soapHeader = null;

		SOAPElement soapBody;
		if (xmlBody != null)
			soapBody = stringToSOAPElement(xmlBody);
		else
			soapBody = null;

		SOAPMessage soapResponse = soapConnection.call(
			createSOAPRequest(operation, soapHeader, soapBody),
			this.serverHost
		);

		String response = this.MessageAsString(soapResponse).trim();

		if (this.debug)
			System.out.println("SOAP response: " + response);

		return response;
	}

	/** Enable or disable debugging output
	 *
	 * @param enable Enable or disable debugging
	 * @param additionalCookies String contains raw cookies to pass to every HTTP call
	 */
	public void setDebug(Boolean enable, String additionalCookies) {
		this.debug = enable;
		this.debug_cookies = additionalCookies;
	}

	/**
	* Create a SOAP connection
	*
	* @throws UnsupportedOperationException
	* @throws SOAPException
	*/
	private void createSOAPConnection() throws UnsupportedOperationException,
		SOAPException {

		SOAPConnectionFactory soapConnectionFactory;
		soapConnectionFactory = SOAPConnectionFactory.newInstance();
		soapConnection = soapConnectionFactory.createConnection();
	}

	/**
	* Create a SOAP request
	*
	* @param operation Operation to perform
	* @param header Header of the SOAP message (can be null)
	* @param body Body of the SOAP message (can be null)
	* @return SOAP message
	* @throws SOAPException
	*/
	private SOAPMessage createSOAPRequest(String operation, SOAPElement header, SOAPElement body)
		throws SOAPException {

		final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();
		final SOAPPart soapPart = soapMessage.getSOAPPart();

		// SOAP Envelope
		final SOAPEnvelope envelope = soapPart.getEnvelope();
		//envelope.addNamespaceDeclaration(PREFIX_NAMESPACE, NAMESPACE);

		// SOAP Header
		if (header != null) {
			final SOAPHeader soapHeader = envelope.getHeader();
			soapHeader.addChildElement(header);
		}

		// SOAP Body
		if (body != null) {
			final SOAPBody soapBody = envelope.getBody();
			soapBody.addChildElement(body);
		}

		// Mime Headers
		final MimeHeaders headers = soapMessage.getMimeHeaders();
		if (!this.debug_cookies.isEmpty())
			headers.addHeader("Cookie", this.debug_cookies);

		if (!operation.isEmpty() && (operation.charAt(0) == '/'))
			operation = operation.substring(1);

		if (this.debug)
			System.out.println("SOAP Action: " + this.serverHost + operation);
		headers.addHeader("SOAPAction", this.serverHost + operation);

		soapMessage.saveChanges();

		if (this.debug)
			System.out.println("SOAP request: "+this.MessageAsString(soapMessage));

		return soapMessage;
	}

	/**
	* Convert SOAPMessage to string
	*
	* @param soapMessage SOAP message to convert
	* @return String (XML)
	**/
	private String MessageAsString(SOAPMessage soapMessage) {
		String s = "";

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
				soapMessage.writeTo(baos);
				s = baos.toString();
				baos.close();
		} catch (Exception e) {
				e.printStackTrace();
			}

		return s;
	}

	/**
	* Convert String to SOAP element
	*
	* @param xmlRequestBody Request body (XML)
	* @return SOAP element
	* @throws SOAPException
	* @throws SAXException
	* @throws IOException
	* @throws ParserConfigurationException
	*/
	private SOAPElement stringToSOAPElement(String xmlRequestBody)
		throws SOAPException, SAXException, IOException, ParserConfigurationException {

		// Load the XML text into a DOM Document
		final DocumentBuilderFactory builderFactory = DocumentBuilderFactory
			.newInstance();
		builderFactory.setNamespaceAware(true);
		final InputStream stream = new ByteArrayInputStream(
			xmlRequestBody.getBytes());
		final Document doc = builderFactory.newDocumentBuilder().parse(stream);

		// Use SAAJ to convert Document to SOAPElement
		// Create SoapMessage
		final MessageFactory msgFactory = MessageFactory.newInstance();
		final SOAPMessage message = msgFactory.createMessage();
		final SOAPBody soapBody = message.getSOAPBody();

		// Return the SOAPBodyElement that contains ONLY the payload
		return soapBody.addDocument(doc);
	}

}
