package co.cloudify.jenkins.plugin.integrations;

import java.io.PrintStream;
import java.util.Date;
import java.util.Map;

import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.kohsuke.stapler.DataBoundSetter;

import co.cloudify.jenkins.plugin.BlueprintUploadSpec;
import co.cloudify.jenkins.plugin.CloudifyBuildStep;
import co.cloudify.jenkins.plugin.CloudifyEnvironmentData;
import co.cloudify.jenkins.plugin.CloudifyPluginUtilities;
import co.cloudify.rest.client.CloudifyClient;
import co.cloudify.rest.model.Blueprint;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public abstract class IntegrationBuildStep extends CloudifyBuildStep {
    private String deploymentId;
    private boolean echoEnvData;
    private boolean debugOutput;
    protected String envDataLocation;
    protected Map<String, Object> inputs;

    public boolean isEchoEnvData() {
        return echoEnvData;
    }

    @DataBoundSetter
    public void setEchoEnvData(boolean echoOutputs) {
        this.echoEnvData = echoOutputs;
    }

    public boolean isDebugOutput() {
        return debugOutput;
    }

    @DataBoundSetter
    public void setDebugOutput(boolean debugOutput) {
        this.debugOutput = debugOutput;
    }

    public String getEnvDataLocation() {
        return envDataLocation;
    }

    @DataBoundSetter
    public void setEnvDataLocation(String outputsLocation) {
        this.envDataLocation = outputsLocation;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    @DataBoundSetter
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    protected void performImpl(Run<?, ?> run, Launcher launcher, TaskListener listener, FilePath workspace,
            EnvVars envVars, CloudifyClient cloudifyClient) throws Exception {
        PrintStream logger = listener.getLogger();

        String blueprintId = generateBlueprintId();
        String deploymentId = expandString(envVars, this.deploymentId);
        if (StringUtils.isBlank(deploymentId)) {
            deploymentId = generateDeploymentId();
        }
        String envDataLocation = expandString(envVars, this.envDataLocation);

        BlueprintUploadSpec uploadSpec = getBlueprintUploadSpec();
        Blueprint blueprint = uploadSpec.upload(cloudifyClient.getBlueprintsClient(), blueprintId);
        CloudifyEnvironmentData envData = CloudifyPluginUtilities.createEnvironment(
                listener, workspace, cloudifyClient,
                blueprint.getId(), deploymentId, inputs, envDataLocation,
                echoEnvData, debugOutput);
        JsonObject dataJsonObject = envData.toJson();
        if (echoEnvData) {
            logger.println(String.format("Environment data: %s",
                    CloudifyPluginUtilities.toString(dataJsonObject)));
        }
        if (envDataLocation != null) {
            FilePath outputFilePath = workspace.child(envDataLocation);
            logger.println(String.format(
                    "Writing environment data to %s", outputFilePath));
            CloudifyPluginUtilities.writeJson(dataJsonObject, outputFilePath);
        }
    }

    protected abstract String getIntegrationName();

    protected abstract BlueprintUploadSpec getBlueprintUploadSpec();

    /**
     * @return A generated blueprint ID. May be overridden by subclasses for specialized implementations.
     */
    protected String generateBlueprintId() {
        return String.format("%s-%s", getIntegrationName(), Long.toHexString(new Date().getTime()));
    }

    /**
     * @return A generated dID. May be overridden by subclasses for specialized implementations.
     */
    protected String generateDeploymentId() {
        return generateBlueprintId();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("deploymentId", deploymentId)
                .append("debugOutput", debugOutput)
                .append("echoOutputs", echoEnvData)
                .append("inputs", inputs)
                .append("outputsLocation", envDataLocation)
                .toString();
    }
}
