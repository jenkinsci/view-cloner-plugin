package org.jenkinsci.plugin.viewcloner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import hudson.model.TaskListener;
import jenkins.model.Jenkins;

public class ViewHandler {
	private Jenkins jenkins;
	private PrintStream logger;
	
	ViewHandler(TaskListener listener) {
		this.jenkins = Jenkins.getInstance();
		this.logger = listener.getLogger();
	}

	/**
	 * Returns config.xml of the object that url points at as a Document object.
	 * 
	 * @param url
	 *            Absolute url to the view
	 * @param authStringEnc
	 *            Encrypted username:password to use when accessing url.
	 * @return Returns config.xml as Document of the object that url points at.
	 * @see <a href=
	 *      "https://docs.oracle.com/javase/7/docs/api/org/w3c/dom/Document.html">
	 *      Document</a>
	 */
	public Document getViewConfig(String url, String authStringEnc) {
		logger.println("[Get view config]");
		Document xml = Utils.getConfig(url, authStringEnc);
		logger.println("Successfuly acquired view config from " + url + Utils.CONFIG_XML_PATH);
		return xml;
	}

	/**
	 * Returns names of jobs that are visible in the view.
	 * 
	 * @param view
	 *            Config.xml of a view.
	 * @return Returns List of names that are visible in the view.
	 */
	public List<String> getNamesOfAssignedJobs(Document view) {
		logger.println("[Get assigned jobs]");
		List<String> jobNames = getAssignedJobs(view.getChildNodes(), new ArrayList<String>());
		logger.println("Jobs that are present in the view");
		for(String name : jobNames){
			logger.println(name);
		}
		return jobNames;
	}

	/**
	 * Recursive method designated to parse nodeList and populate jobNames with
	 * values that are second level children to nodes getNodeName()=="jobNames".
	 * 
	 * @param nList
	 * @param jobNames
	 * @return Returns List of node values that are second level children to nodes with
	 *         getNodeName()=="jobNames".
	 * @see <a href=
	 *      "ViewHandler.html#getNamesOfAssignedJobs(org.w3c.dom.Document)">
	 *      getComponentAt(org.w3c.dom.Document)</a>
	 */
	private List<String> getAssignedJobs(NodeList nList, List<String> jobNames) {
		for (int i = 0; i < nList.getLength(); i++) { 								// loop through nList
			if (Utils.isLowestNode(nList.item(i))) { 								// check if lowest node
				if (nList.item(i).getNodeValue() != null) { 						// check if it has value
					if (nList.item(i).getParentNode().getParentNode().getNodeName().equals("jobNames")) { //check if job name
						jobNames.add(nList.item(i).getNodeValue());
					}
				}
			} else {
				getAssignedJobs(nList.item(i).getChildNodes(), jobNames);
			}
		}
		return jobNames;
	}

	/**
	 * Changes provided xml document based on oldValue=newValue value pairs
	 * provided in replacePatternOldNew
	 * 
	 * @param xml
	 *            Document which parameters need to be changed
	 * @param replacePatternOldNew
	 *            Map of oldValue=newValue that should be used when changing xml
	 */
	public void changeConfig(Document xml, Map<String, String> replacePatternOldNew) {
		Utils.changeConfig(xml, replacePatternOldNew);
	}

	/**
	 * Creates a view in jenkins.
	 * 
	 * @param url
	 *            view that createItem will be called on
	 * @param niewViewName
	 *            name of the view we are trying to create
	 * @param viewConfig
	 *            config.xml that view will be created from
	 * @param authStringEnc
	 *            authentication
	 */
	public void createView(String url, String niewViewName, Document viewConfig, String authStringEnc) {
		logger.println("[Create view]");
		String config = Utils.docToString(viewConfig);
		Utils.createView(url, niewViewName, config, authStringEnc);
		logger.println("Creaeted view " + url + "/view/" + niewViewName);
	}
}
