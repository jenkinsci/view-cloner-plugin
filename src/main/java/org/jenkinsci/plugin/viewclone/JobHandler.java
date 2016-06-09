package org.jenkinsci.plugin.viewclone;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;

import hudson.model.TaskListener;
import jenkins.model.Jenkins;

public class JobHandler {
	private Jenkins jenkins;
	private PrintStream logger;
	
	JobHandler(TaskListener listener) {
		this.jenkins = Jenkins.getInstance();
		this.logger = listener.getLogger();
	}
	
	/**
	 * Get config.xml of jobs in jobNames. 
	 * 
	 * @param jobNames List of job names to get configs from
	 * @param authStringEnc authentication
	 * @return Returns Map jobName:jobConfig
	 * @see <a href=
	 *      "https://docs.oracle.com/javase/7/docs/api/org/w3c/dom/Document.html">
	 *      Document</a>
	 */
	public Map<String, Document> getJobConfigs(List<String> jobNames, String authStringEnc) {
		String url;
		Map<String, Document> map = new HashMap<String, Document>();
		logger.println("[Get job configs]");
		for(String jobName : jobNames){
			if(jenkins.getItem(jobName) != null){
				url = jenkins.getRootUrl() + "job/" + jobName;
				Document xml = Utils.getConfig(url, authStringEnc);
				logger.println("Successfuly acquired job config from " + url + Utils.CONFIG_XML_PATH);
				map.put(jobName, xml);
			}	
		}
		return map;
	}


	/**
	 * Changes given names and configs with 
	 * <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#replace%28char,%20char%29">
	 *      java.lang.String#replace</a> 
	 * 
	 * 
	 * @param jobNameConfig Map newJobName:newJobConfig
	 * @param replacePatternOldNew Map of oldValue=newValue that should be used when changing xml
	 * @return Returns Map newJobName:newJobConfig
	 */
	public Map<String, Document> changeNamesAndConfigs(Map<String, Document> jobNameConfig, Map<String, String> replacePatternOldNew) {
		Map<String, Document> newJobNameConfig = new HashMap<String, Document>();
		Iterator<?> it = jobNameConfig.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Document> pair = (Entry<String, Document>) it.next();
			Utils.changeConfig(pair.getValue(), replacePatternOldNew); // change config.xml
			String newName = Utils.paramChange(pair.getKey(), replacePatternOldNew);  // change job name
			newJobNameConfig.put(newName, pair.getValue());
		}
		return newJobNameConfig;
	}

	/**
	 * Creates jobs in jenkins from newJobName:newJobConfig
	 * 
	 * @param jobNameConfig newJobName:newJobConfig
	 */
	public void createJobs(Map<String, Document> jobNameConfig) {
		Iterator<?> it = jobNameConfig.entrySet().iterator();
		logger.println("[Create jobs]");
		while (it.hasNext()) {
			Map.Entry<String, Document> pair = (Entry<String, Document>) it.next();
			if(jenkins.getItem(pair.getKey()) == null){
				try {
					jenkins.createProjectFromXML(pair.getKey(), Utils.docToStream(pair.getValue()));
					logger.println("Job "+ pair.getKey() + " created");
				} catch (IOException | TransformerFactoryConfigurationError e) {
					throw new RuntimeException(e);
				}
			} else {
				logger.println("Job with name " + pair.getKey() + " already exists");
			}
		}
	}
}
