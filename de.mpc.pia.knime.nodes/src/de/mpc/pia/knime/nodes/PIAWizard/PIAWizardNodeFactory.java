package de.mpc.pia.knime.nodes.PIAWizard;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PIA Wizard" Node.
 * 
 *
 * @author Julian Uszkoreit
 */
public class PIAWizardNodeFactory 
        extends NodeFactory<PIAWizardNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PIAWizardNodeModel createNodeModel() {
        return new PIAWizardNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<PIAWizardNodeModel> createNodeView(final int viewIndex,
            final PIAWizardNodeModel nodeModel) {
        return new PIAWizardNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new PIAWizardNodeDialog();
    }

}

