package co.cloudify.jenkins.plugin;

import java.io.File;
import java.io.PrintStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import co.cloudify.rest.client.CloudifyClient;
import co.cloudify.rest.model.Plugin;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

public class UploadPluginBuildStep extends CloudifyBuildStep {
	private String wagonLocation;
	private String yamlLocation;
	private String outputLocation;

	@DataBoundConstructor
	public UploadPluginBuildStep() {
		super();
	}

	@DataBoundSetter
	public void setWagonLocation(String wagonLocation) {
		this.wagonLocation = wagonLocation;
	}

	public String getWagonLocation() {
		return wagonLocation;
	}

	@DataBoundSetter
	public void setYamlLocation(String yamlLocation) {
		this.yamlLocation = yamlLocation;
	}

	public String getYamlLocation() {
		return yamlLocation;
	}

	@DataBoundSetter
	public void setOutputLocation(String outputLocation) {
		this.outputLocation = outputLocation;
	}
	
	public String getOutputLocation() {
		return outputLocation;
	}
	
	@Override
	protected void perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener,
	        CloudifyClient cloudifyClient) throws Exception {
		PrintStream jenkinsLog = listener.getLogger();
		VariableResolver<String> buildVariableResolver = build.getBuildVariableResolver();

		String effectiveWagonLocation = Util.replaceMacro(wagonLocation, buildVariableResolver);
		String effectiveYamlLocation = Util.replaceMacro(yamlLocation, buildVariableResolver);
		String effectiveOutputLocation = Util.replaceMacro(outputLocation, buildVariableResolver);

		Plugin plugin = cloudifyClient.getPluginsClient().upload(effectiveWagonLocation, effectiveYamlLocation);
		
		if (StringUtils.isNotBlank(effectiveOutputLocation)) {
			File outputFile = new File(build.getWorkspace().child(effectiveOutputLocation).getRemote());
			jenkinsLog.println(String.format("Saving plugin information to %s", outputFile));
			CloudifyPluginUtilities.writeJson(plugin, outputFile);
		}
		jenkinsLog.println("Plugin uploaded successfully");
	}

	@Symbol("uploadCloudifyBlueprint")
	@Extension
	public static class Descriptor extends BuildStepDescriptor<Builder> {
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		public FormValidation doCheckWagonLocation(@QueryParameter String value) {
			return FormValidation.validateRequired(value);
		}

		public FormValidation doCheckYamlLocation(@QueryParameter String value) {
			return FormValidation.validateRequired(value);
		}

		@Override
		public String getDisplayName() {
			return "Upload Cloudify plugin";
		}
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
		        .appendSuper(super.toString())
		        .append("wagonLocation", wagonLocation)
		        .append("yamlLocation", yamlLocation)
		        .append("outputLocation", outputLocation)
		        .toString();
	}
}