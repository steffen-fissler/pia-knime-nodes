// THIS CODE WAS GENERATED WITH THE GENERIC WORKFLOW NODES FOR KNIME NODE GENERATOR
// DO NOT MODIFY
package de.mpc.pia.knime.nodes.PSMAddPreferredFDRScore;

import java.io.InputStream;

import org.knime.core.node.NodeLogger;

import com.genericworkflownodes.knime.config.INodeConfiguration;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeDialog;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeFactory;

import de.mpc.pia.knime.PluginActivator;

/**
    @brief PSMAddPreferredFDRScore Node Factory.
*/
public class PSMAddPreferredFDRScoreNodeFactory extends GenericKnimeNodeFactory {
    
    private static final NodeLogger logger = NodeLogger.getLogger(PSMAddPreferredFDRScoreNodeFactory.class);
    
    @Override
    public PSMAddPreferredFDRScoreNodeModel createNodeModel() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PSMAddPreferredFDRScoreNodeModel(tmpConfig, PluginActivator
                    .getInstance().getPluginConfiguration());
        } catch (Exception e) {
            logger.error("PSMAddPreferredFDRScore model instantiation failed", e);
        }
        return null;

    }

    @Override
    public GenericKnimeNodeDialog createNodeDialogPane() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PSMAddPreferredFDRScoreNodeDialog(tmpConfig);
        } catch (Exception e) {
            logger.error("PSMAddPreferredFDRScore dialog instantiation failed", e);
        }
        return null;
    }

    @Override
    protected InputStream getConfigAsStream() {
        return this.getClass().getResourceAsStream("config/config.xml");
    }
}
