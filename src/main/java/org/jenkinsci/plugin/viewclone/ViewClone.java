package org.jenkinsci.plugin.viewclone;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.w3c.dom.Document;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Base64;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class ViewClone extends Builder implements SimpleBuildStep {

	private String replacePatternString;
	private String url;
	private String niewViewName;
	
	@DataBoundConstructor
	public ViewClone(String url, String replacePatternString, String niewViewName) {
		this.replacePatternString = replacePatternString;
		this.url = url;
		this.niewViewName = niewViewName;
	}

	public String getNiewViewName() {
		return niewViewName;
	}
	
	public String getReplacePatternString() {
		return replacePatternString;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public void perform(Run<?, ?> run, FilePath path, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		url = Utils.removeEndSlash(url);
		// view that createItem will be called on has to be 1 higher than the view we are cloning
		// so both old and new views would be on the same level
		String urlToParentView = Utils.getUrlToTheParentView(url);
		
		String authStringEnc = Base64.encode(new String(getDescriptor().getUsername() + ":" + getDescriptor().getPassword()).getBytes());
		
		ViewHandler viewHandler = new ViewHandler(listener);
		JobHandler jobHandler = new JobHandler(listener);
		Map<String, String> replacePatternOldNew = Utils.processReplacePatern(replacePatternString);
		
		Document viewConfig = viewHandler.getViewConfig(url, authStringEnc);
		List<String> jobNames = viewHandler.getNamesOfAssignedJobs(viewConfig);
		Map<String, Document> jobNameConfig = jobHandler.getJobConfigs(jobNames, authStringEnc);
		
		Map<String, Document> newJobNameConfig = jobHandler.changeNamesAndConfigs(jobNameConfig, replacePatternOldNew);
		viewHandler.changeConfig(viewConfig, replacePatternOldNew);
		
		jobHandler.createJobs(newJobNameConfig);
		viewHandler.createView(urlToParentView, niewViewName, viewConfig, authStringEnc);
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		private String password;
		private String username;
		
		public DescriptorImpl() {
			load();
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			username = formData.getString("username");
			password = formData.getString("password");
			save();
			return super.configure(req, formData);
		}
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "View clone";
		}
		
		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}
		
	}


}

