package de.mpc.pia.knime.nodes.analysis;

import org.knime.core.node.NodeView;

import de.mpc.pia.knime.nodes.visualization.ProteinsVisualizationPanel;

/**
 * <code>NodeView</code> for the "PIADefault" Node.
 *
 *
 * @author Julian Uszkoreit
 */
public class PIAAnalysisNodeView extends NodeView<PIAAnalysisNodeModel> {

    /** */
    private ProteinsVisualizationPanel visualizationPanel;


    /**
     * Creates a new view.
     *
     * @param nodeModel The model (class: {@link PIAAnalysisNodeModel})
     */
    protected PIAAnalysisNodeView(final PIAAnalysisNodeModel nodeModel) {
        super(nodeModel);

        visualizationPanel = new ProteinsVisualizationPanel(nodeModel.getFilteredProteinList(),
                nodeModel.getAnalysisModel(), nodeModel.getPSMToSpectrum());
        setComponent(visualizationPanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and
        // update the view.
        PIAAnalysisNodeModel nodeModel =
            (PIAAnalysisNodeModel)getNodeModel();
        assert nodeModel != null;

        visualizationPanel = new ProteinsVisualizationPanel(nodeModel.getFilteredProteinList(),
                nodeModel.getAnalysisModel(), nodeModel.getPSMToSpectrum());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        visualizationPanel.close();
        visualizationPanel = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

    }

}

