package org.jenkinsci.plugin.viewcloner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Utils {

	final static String CONFIG_XML_PATH = "/config.xml";
	/**
	 * Gets config.xml of the object that viewPath points at.
	 * 
	 * @param objectPath
	 *            Absolute url
	 * @param authStringEnc
	 *            authentication
	 * @return Returns config.xml of the object that viewPath points at.
	 */
	static Document getConfig(String objectPath, String authStringEnc) {

		String configPath = objectPath + CONFIG_XML_PATH;
		URL url = null;
		HttpURLConnection conn = null;
		try {
			url = new URL(configPath);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
			Document xml = streamToDoc(conn.getInputStream());
			int responce = conn.getResponseCode();	
			if(responce != 200){
				throw new RuntimeException("Unable to access " + configPath + "\nResponce code: " + responce);
			}
			return xml;
		} catch (IOException e) {
			throw new RuntimeException("Unable to access " + configPath , e);
		}
	}

	/**
	 * Creates a view via jenkins Remote access API.
	 * 
	 * @param parentViewPath
	 *            view that createItem will be called on
	 * @param niewViewName
	 *            name of the view we are trying to create
	 * @param viewConfig
	 *            config.xml that view will be created from
	 * @param authStringEnc
	 *            authentication
	 */
	static void createView(String parentViewPath, String niewViewName, String viewConfig, String authStringEnc) {
		int response = -1;
		try {
			String url = parentViewPath + "/createView?name=" + niewViewName;
			URL obj = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) obj.openConnection();

			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
			conn.setRequestProperty("Content-Type", "text/xml");
			conn.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

			wr.writeBytes(viewConfig);
			wr.flush();
			wr.close();
			response = conn.getResponseCode();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts java.io.InputStream into org.w3c.dom.Document.
	 * 
	 * @param inputStream
	 * @return Returns org.w3c.dom.Document
	 * @see <a href=
	 *      "https://docs.oracle.com/javase/7/docs/api/org/w3c/dom/Document.html">
	 *      Document</a>
	 */
	static Document streamToDoc(InputStream inputStream) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		Document doc = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(inputStream);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException(e);
		}

		return doc;
	}

	/**
	 * Converts org.w3c.dom.Document into java.io.InputStream
	 * 
	 * @param doc
	 * @return Returns java.io.InputStream
	 * @see <a href=
	 *      "https://docs.oracle.com/javase/7/docs/api/org/w3c/dom/Document.html">
	 *      Document</a> 
	 */
	static InputStream docToStream(Document doc) {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Source xmlSource = new DOMSource(doc);
		Result outputTarget = new StreamResult(outputStream);
		try {
			TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
		} catch (TransformerException | TransformerFactoryConfigurationError e) {
			throw new RuntimeException(e);
		}
		return new ByteArrayInputStream(outputStream.toByteArray());
	}

	/**
	 * Changes given config with 
	 * <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#replace%28char,%20char%29">
	 *      java.lang.String#replace</a> 
	 *      
	 * @param xml 
	 * @param replacePatternOldNew
	 */
	public static void changeConfig(Document xml, Map<String, String> replacePatternOldNew) {
		changeConfig(xml.getChildNodes(), replacePatternOldNew);
	}
	
	/**
	 * Changes values of lowest nodes with 
	 * <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#replace%28char,%20char%29">
	 *      java.lang.String#replace</a> 
	 * 
	 * @param nList 
	 * @param replacePatternOldNew
	 */
	private static void changeConfig(NodeList nList, Map<String, String> replacePatternOldNew) {

		for (int i = 0; i < nList.getLength(); i++) {						 	// loop through nList
			if (isLowestNode(nList.item(i))) { 								 	// if lowest node
				if (nList.item(i).getNodeValue() != null) {					 	// if has value
					if (!nList.item(i).getNodeValue().trim().equals("")) { 		// if value is not empty 
						String newValue = paramChange(nList.item(i).getNodeValue(), replacePatternOldNew);
						nList.item(i).setNodeValue(newValue);
					}
				}
			} else {
				changeConfig(nList.item(i).getChildNodes(), replacePatternOldNew);
			}
		}
	}

	/**
	 * Checks if provided node has any children
	 */
	static boolean isLowestNode(Node node) {
		if (node.hasChildNodes()) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 
	 * @param value
	 * @param paramReplacementMap
	 * @return
	 */
	static String paramChange(String value, Map<String, String> paramReplacementMap) {
		Iterator<?> it = paramReplacementMap.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<String, String> pair = (Map.Entry) it.next();
			value = value.replace(pair.getKey(), pair.getValue());
			if (!value.contains("\r\n")) {
				value = value.replace("\n", "\r\n");
			}
		}
		return value;
	}

	/**
	 * 
	 * @param replacePatern
	 * @return
	 */
	static Map<String, String> processReplacePatern(String replacePatern) {
		Map<String, String> paramReplacementMap = new HashMap<String, String>();
		String[] oldNewPair = replacePatern.split(",");
		for (String pair : oldNewPair) {
			String[] values = pair.trim().split("=");
			paramReplacementMap.put(values[0], values[1]);
		}

		return paramReplacementMap;
	}

	/**
	 * 
	 * @param doc
	 * @return
	 */
	static String docToString(Document doc) {
		try {
			StringWriter stringWriter = new StringWriter();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
			return stringWriter.toString();
		} catch (Exception e) {
			throw new RuntimeException("Error converting to String", e);
		}
	}

	/**
	 * Simple method that removes ending "/" if it exists
	 * 
	 * @param url
	 * @return String without ending "/"
	 */
	static String removeEndSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);	
		}
		return url;
	}

	/**
	 * Returns url to the parent of view that give url points at.
	 * Example: url http://localhost:8080/jenkins/view/branches/view/trunk will return http://localhost:8080/jenkins/view/branches
	 * Is used to call doCreateItem from.
	 * 
	 * @param url Absolute url of the view
	 * @return Returns url to the parent of view that give url points at.
	 */
	static String getUrlToTheParentView(String url) {
		Pattern pattern = Pattern.compile("(.*)/view/(.*)");
		Matcher matcher = pattern.matcher(url);
		if (matcher.find()) {
			return url = matcher.group(1);
		} else {
			throw new RuntimeException("Unable to copy view: No views in the provided url " + url);
		}
	}


}
