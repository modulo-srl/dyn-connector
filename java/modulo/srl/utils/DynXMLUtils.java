package modulo.srl.utils;

/** XML strings helper class, without using any external library
 *
 * @version 1.1
 * @author Modulo srl
 */
public final class DynXMLUtils {
	private DynXMLUtils() {}

	public static String getXMLNodeContent(String xml, String nodeName) {
		String find = "<"+nodeName+">";
		int posStart = xml.indexOf(find);
	
		if (posStart >= 0) {
			posStart += find.length();

			find = "</" + nodeName + ">";
			int posEnd = xml.indexOf(find, posStart);
			if (posEnd > posStart) {
				String nodeContent = xml.substring(posStart, posEnd).trim();

				if ((nodeContent.length() > 12) && nodeContent.substring(0, 9).equals("<![CDATA["))
					nodeContent = nodeContent.substring(9, nodeContent.indexOf("]]>"));

				return nodeContent;
			}
		}

		return "";
	}
}
