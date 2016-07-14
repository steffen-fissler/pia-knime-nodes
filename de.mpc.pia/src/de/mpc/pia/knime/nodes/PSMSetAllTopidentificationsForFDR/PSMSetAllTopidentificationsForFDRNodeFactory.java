// THIS CODE WAS GENERATED WITH THE GENERIC WORKFLOW NODES FOR KNIME NODE GENERATOR
// DO NOT MODIFY
package de.mpc.pia.knime.nodes.PSMSetAllTopidentificationsForFDR;

import java.io.InputStream;

import org.knime.core.node.NodeLogger;

import com.genericworkflownodes.knime.config.INodeConfiguration;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeDialog;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeFactory;

import de.mpc.pia.knime.PluginActivator;

/**
    @brief PSMSetAllTopidentificationsForFDR Node Factory.
*/
public class PSMSetAllTopidentificationsForFDRNodeFactory extends GenericKnimeNodeFactory {
    
    private static final NodeLogger logger = NodeLogger.getLogger(PSMSetAllTopidentificationsForFDRNodeFactory.class);
    
    @Override
    public PSMSetAllTopidentificationsForFDRNodeModel createNodeModel() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PSMSetAllTopidentificationsForFDRNodeModel(tmpConfig, PluginActivator
                    .getInstance().getPluginConfiguration());
        } catch (Exception e) {
            logger.error("PSMSetAllTopidentificationsForFDR model instantiation failed", e);
        }
        return null;

    }

    @Override
    public GenericKnimeNodeDialog createNodeDialogPane() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PSMSetAllTopidentificationsForFDRNodeDialog(tmpConfig);
        } catch (Exception e) {
            logger.error("PSMSetAllTopidentificationsForFDR dialog instantiation failed", e);
        }
        return null;
    }

    @Override
    protected InputStream getConfigAsStream() {
        return this.getClass().getResourceAsStream("config/config.xml");
    }
}
