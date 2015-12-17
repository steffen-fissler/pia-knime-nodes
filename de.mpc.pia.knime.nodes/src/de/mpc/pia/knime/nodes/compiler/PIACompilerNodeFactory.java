package de.mpc.pia.knime.nodes.compiler;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PIACompiler" Node.
 *
 *
 * @author Julian Uszkoreit
 */
public class PIACompilerNodeFactory extends NodeFactory<PIACompilerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PIACompilerNodeModel createNodeModel() {
        return new PIACompilerNodeModel();
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
    public NodeView<PIACompilerNodeModel> createNodeView(final int viewIndex,
            final PIACompilerNodeModel nodeModel) {
        return new PIACompilerNodeView(nodeModel);
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
        return new PIACompilerNodeDialog();
    }

}

