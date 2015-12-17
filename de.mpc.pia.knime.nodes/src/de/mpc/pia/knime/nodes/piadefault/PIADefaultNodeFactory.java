package de.mpc.pia.knime.nodes.piadefault;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PIADefault" Node.
 * 
 *
 * @author Julian Uszkoreit
 */
public class PIADefaultNodeFactory 
        extends NodeFactory<PIADefaultNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PIADefaultNodeModel createNodeModel() {
        return new PIADefaultNodeModel();
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
    public NodeView<PIADefaultNodeModel> createNodeView(final int viewIndex,
            final PIADefaultNodeModel nodeModel) {
        return new PIADefaultNodeView(nodeModel);
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
        return new PIADefaultNodeDialog();
    }

}

