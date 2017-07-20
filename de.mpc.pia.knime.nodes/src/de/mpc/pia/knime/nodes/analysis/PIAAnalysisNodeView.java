package de.mpc.pia.knime.nodes.analysis;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;

import de.mpc.pia.knime.nodes.PIAAnalysisModel;
import de.mpc.pia.knime.nodes.visualization.ProteinsVisualizationPanel;

/**
 * <code>NodeView</code> for the "PIADefault" Node.
 *
 *
 * @author Julian Uszkoreit
 */
public class PIAAnalysisNodeView extends NodeView<PIAAnalysisNodeModel> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PIAAnalysisNodeView.class);

    /** the actually schon visualization panel */
    private ProteinsVisualizationPanel visualizationPanel;

    /** the analysis model */
    private PIAAnalysisModel analysisModel;


    /**
     * Creates a new view.
     *
     * @param nodeModel The model (class: {@link PIAAnalysisNodeModel})
     */
    protected PIAAnalysisNodeView(final PIAAnalysisNodeModel nodeModel) {
        super(nodeModel);
        // creation in modelChanged, because it is called every time
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        PIAAnalysisNodeModel nodeModel =
            (PIAAnalysisNodeModel)getNodeModel();
        assert nodeModel != null;

        LOGGER.debug("loading model in modelChanged");
        analysisModel = nodeModel.loadAnalysisModelFromFile();
        visualizationPanel = new ProteinsVisualizationPanel(nodeModel.getFilteredProteinList(analysisModel),
                analysisModel, nodeModel.getPSMToSpectrum());
        setComponent(visualizationPanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        visualizationPanel = null;
        analysisModel = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

    }

}

