// THIS CODE WAS GENERATED WITH THE GENERIC WORKFLOW NODES FOR KNIME NODE GENERATOR
// DO NOT MODIFY
package de.mpc.pia.knime.nodes.PSMCreatePSMSets;

import java.io.InputStream;

import org.knime.core.node.NodeLogger;

import com.genericworkflownodes.knime.config.INodeConfiguration;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeDialog;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeFactory;

import de.mpc.pia.knime.PluginActivator;

/**
    @brief PSMCreatePSMSets Node Factory.
*/
public class PSMCreatePSMSetsNodeFactory extends GenericKnimeNodeFactory {
    
    private static final NodeLogger logger = NodeLogger.getLogger(PSMCreatePSMSetsNodeFactory.class);
    
    @Override
    public PSMCreatePSMSetsNodeModel createNodeModel() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PSMCreatePSMSetsNodeModel(tmpConfig, PluginActivator
                    .getInstance().getPluginConfiguration());
        } catch (Exception e) {
            logger.error("PSMCreatePSMSets model instantiation failed", e);
        }
        return null;

    }

    @Override
    public GenericKnimeNodeDialog createNodeDialogPane() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PSMCreatePSMSetsNodeDialog(tmpConfig);
        } catch (Exception e) {
            logger.error("PSMCreatePSMSets dialog instantiation failed", e);
        }
        return null;
    }

    @Override
    protected InputStream getConfigAsStream() {
        return this.getClass().getResourceAsStream("config/config.xml");
    }
}
