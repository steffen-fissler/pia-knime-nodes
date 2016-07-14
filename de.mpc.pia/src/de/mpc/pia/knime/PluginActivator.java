// THIS CODE WAS GENERATED WITH THE GENERIC WORKFLOW NODES FOR KNIME NODE GENERATOR
// DO NOT MODIFY
package de.mpc.pia.knime;

import java.util.Arrays;
import java.util.List;

import org.osgi.framework.BundleContext;

import com.genericworkflownodes.knime.custom.GenericActivator;
import com.genericworkflownodes.knime.custom.config.IPluginConfiguration;
import com.genericworkflownodes.knime.custom.config.impl.PluginConfiguration;
import com.genericworkflownodes.knime.toolfinderservice.ExternalTool;

public class PluginActivator extends GenericActivator {

    private static PluginActivator INSTANCE = null;   
    private static IPluginConfiguration PLUGIN_CONFIG = null;
    private static final List<ExternalTool> TOOLS = Arrays.asList(new ExternalTool("de.mpc.pia", "PeptideAddFilter", "java"), new ExternalTool("de.mpc.pia", "PeptideConsiderModifications", "java"), new ExternalTool("de.mpc.pia", "PIACompiler", "java"), new ExternalTool("de.mpc.pia", "PIAExecute", "java"), new ExternalTool("de.mpc.pia", "PIAGeneratePipelineXML", "java"), new ExternalTool("de.mpc.pia", "ProteinAddFilter", "java"), new ExternalTool("de.mpc.pia", "ProteinAddInferenceFilter", "java"), new ExternalTool("de.mpc.pia", "ProteinCalculateFDR", "java"), new ExternalTool("de.mpc.pia", "ProteinInfereProteins", "java"), new ExternalTool("de.mpc.pia", "PSMAddFilter", "java"), new ExternalTool("de.mpc.pia", "PSMAddPreferredFDRScore", "java"), new ExternalTool("de.mpc.pia", "PSMCalculateAllFDR", "java"), new ExternalTool("de.mpc.pia", "PSMCalculateCombinedFDRScore", "java"), new ExternalTool("de.mpc.pia", "PSMCreatePSMSets", "java"), new ExternalTool("de.mpc.pia", "PSMSetAllDecoyPattern", "java"), new ExternalTool("de.mpc.pia", "PSMSetAllTopidentificationsForFDR", "java"));

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        initializePlugin();
        INSTANCE = this;
    }

    public static PluginActivator getInstance() {
        return INSTANCE;
    }
    
    @Override
    public List<ExternalTool> getTools() {
        return TOOLS;
    }
    
    @Override
    public IPluginConfiguration getPluginConfiguration() {
        if (PLUGIN_CONFIG == null) {
            // construct the plugin config
            PLUGIN_CONFIG = new PluginConfiguration("de.mpc.pia", "PIA", 
                PluginActivator.getInstance().getProperties(), getClass());
        }
        return PLUGIN_CONFIG;
    }
}
