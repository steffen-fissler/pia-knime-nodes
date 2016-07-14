// THIS CODE WAS GENERATED WITH THE GENERIC WORKFLOW NODES FOR KNIME NODE GENERATOR
// DO NOT MODIFY
package de.mpc.pia.knime.nodes.PSMAddFilter;

import java.io.InputStream;

import org.knime.core.node.NodeLogger;

import com.genericworkflownodes.knime.config.INodeConfiguration;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeDialog;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeFactory;

import de.mpc.pia.knime.PluginActivator;

/**
    @brief PSMAddFilter Node Factory.
*/
public class PSMAddFilterNodeFactory extends GenericKnimeNodeFactory {
    
    private static final NodeLogger logger = NodeLogger.getLogger(PSMAddFilterNodeFactory.class);
    
    @Override
    public PSMAddFilterNodeModel createNodeModel() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PSMAddFilterNodeModel(tmpConfig, PluginActivator
                    .getInstance().getPluginConfiguration());
        } catch (Exception e) {
            logger.error("PSMAddFilter model instantiation failed", e);
        }
        return null;

    }

    @Override
    public GenericKnimeNodeDialog createNodeDialogPane() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PSMAddFilterNodeDialog(tmpConfig);
        } catch (Exception e) {
            logger.error("PSMAddFilter dialog instantiation failed", e);
        }
        return null;
    }

    @Override
    protected InputStream getConfigAsStream() {
        return this.getClass().getResourceAsStream("config/config.xml");
    }
}
