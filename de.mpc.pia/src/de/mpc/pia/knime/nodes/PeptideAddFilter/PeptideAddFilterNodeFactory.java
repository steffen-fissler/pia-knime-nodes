// THIS CODE WAS GENERATED WITH THE GENERIC WORKFLOW NODES FOR KNIME NODE GENERATOR
// DO NOT MODIFY
package de.mpc.pia.knime.nodes.PeptideAddFilter;

import java.io.InputStream;

import org.knime.core.node.NodeLogger;

import com.genericworkflownodes.knime.config.INodeConfiguration;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeDialog;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeFactory;

import de.mpc.pia.knime.PluginActivator;

/**
    @brief PeptideAddFilter Node Factory.
*/
public class PeptideAddFilterNodeFactory extends GenericKnimeNodeFactory {
    
    private static final NodeLogger logger = NodeLogger.getLogger(PeptideAddFilterNodeFactory.class);
    
    @Override
    public PeptideAddFilterNodeModel createNodeModel() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PeptideAddFilterNodeModel(tmpConfig, PluginActivator
                    .getInstance().getPluginConfiguration());
        } catch (Exception e) {
            logger.error("PeptideAddFilter model instantiation failed", e);
        }
        return null;

    }

    @Override
    public GenericKnimeNodeDialog createNodeDialogPane() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            return new PeptideAddFilterNodeDialog(tmpConfig);
        } catch (Exception e) {
            logger.error("PeptideAddFilter dialog instantiation failed", e);
        }
        return null;
    }

    @Override
    protected InputStream getConfigAsStream() {
        return this.getClass().getResourceAsStream("config/config.xml");
    }
}
