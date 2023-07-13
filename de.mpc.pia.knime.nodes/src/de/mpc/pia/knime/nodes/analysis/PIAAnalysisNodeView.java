package de.mpc.pia.knime.nodes.analysis;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

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
    private JPanel visualizationPanel;

    /** the analysis model */
    private PIAAnalysisModel analysisModel;

    /** the hash of the currently loaded model file */
    private int modelFileHash;

    /** the loading animation */
    private ImageIcon spinner = new ImageIcon(this.getClass().getResource("/de/mpc/pia/knime/nodes/analysis/spinner.gif"));

    /** the current nodeModel */
    private PIAAnalysisNodeModel currentNodeModel;


    /**
     * Creates a new view.
     *
     * @param nodeModel
     */
    protected PIAAnalysisNodeView(final PIAAnalysisNodeModel nodeModel) {
        super(nodeModel);

        currentNodeModel = nodeModel;
        modelFileHash = nodeModel.getAnalysisModelFileHash();
        LOGGER.debug("loading model in constructor");

        loadAndShowTheModel();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        PIAAnalysisNodeModel nodeModel = getNodeModel();
        assert nodeModel != null;

        currentNodeModel = nodeModel;
        int newHash = nodeModel.getAnalysisModelFileHash();
        if (modelFileHash != newHash) {
            LOGGER.debug("loading model in modelChanged");
            loadAndShowTheModel();
            modelFileHash = newHash;
        }
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
        // nothing to do on opening
    }


    /**
     * Loads the model from file and shows it when done. While loading, a spinning circle is shown to the user.
     */
    private void loadAndShowTheModel() {
        // Background task for loading the model.
        SwingWorker<PIAAnalysisModel, Void> worker = new SwingWorker<PIAAnalysisModel, Void>() {
            @Override
            public PIAAnalysisModel doInBackground() {
                return currentNodeModel.loadAnalysisModelFromFile();
            }

            @Override
            public void done() {
                //Remove the "Loading images" label.
                try {
                    analysisModel = get();
                    visualizationPanel = new ProteinsVisualizationPanel(currentNodeModel.getFilteredProteinList(analysisModel),
                            analysisModel, currentNodeModel.getPSMToSpectrum());
                } catch (Exception e) {
                    LOGGER.error("Error while loading model file", e);

                    visualizationPanel = new JPanel();
                    visualizationPanel.setLayout(new BoxLayout(visualizationPanel, BoxLayout.Y_AXIS));
                    visualizationPanel.add(Box.createVerticalGlue());
                    JLabel loadingLabel = new JLabel("Error while loading model file, check logging.");
                    loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    visualizationPanel.add(loadingLabel);
                    visualizationPanel.add(Box.createVerticalGlue());
                }

                setComponent(visualizationPanel);
            }
        };

        // reset all visualization and show the loading screen
        visualizationPanel = new JPanel();
        visualizationPanel.setLayout(new BoxLayout(visualizationPanel, BoxLayout.Y_AXIS));
        visualizationPanel.setPreferredSize(new Dimension(1200, 800));
        visualizationPanel.add(Box.createVerticalGlue());
        JLabel spinnerLabel = new JLabel(spinner);
        spinnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        visualizationPanel.add(spinnerLabel);
        JLabel loadingLabel = new JLabel("Loading PIA model...");
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        visualizationPanel.add(loadingLabel);
        visualizationPanel.add(Box.createVerticalGlue());

        setComponent(visualizationPanel);

        // execute the worker, which will load the model and update the panel afterwards
        worker.execute();
    }
}

