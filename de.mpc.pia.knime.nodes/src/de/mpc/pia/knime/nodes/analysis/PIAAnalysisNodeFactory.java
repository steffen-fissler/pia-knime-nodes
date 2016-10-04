package de.mpc.pia.knime.nodes.analysis;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PIADefault" Node.
 *
 *
 * @author Julian Uszkoreit
 */
public class PIAAnalysisNodeFactory
        extends NodeFactory<PIAAnalysisNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PIAAnalysisNodeModel createNodeModel() {
        return new PIAAnalysisNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<PIAAnalysisNodeModel> createNodeView(final int viewIndex,
            final PIAAnalysisNodeModel nodeModel) {
        switch (viewIndex) {
        case 1:
            return new PIAPSMSpectrumViewer(nodeModel);

        case 0:
        default:
            return new PIAAnalysisNodeView(nodeModel);
        }
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
        return new PIAAnalysisNodeDialog();
    }

}

