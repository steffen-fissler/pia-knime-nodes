package de.mpc.pia.knime.nodes.analysis;

import org.knime.core.node.NodeView;

import de.mpc.pia.knime.nodes.PIAAnalysisModel;
import de.mpc.pia.knime.nodes.visualization.psmspectrumviewer.PSMSpectrumViewerPanel;


/**
 * View for the PSM spectrum visualization. Visualizes all identified (filtered)
 * PSMs. If the output PSM list contains PSM sets these are resolved into single
 * PSMs.
 *
 * @author julian
 *
 */
public class PIAPSMSpectrumViewer extends NodeView<PIAAnalysisNodeModel> {

    /** The actually shown spectrum viewer panel */
    private PSMSpectrumViewerPanel psmSpectrumPanel;

    /** the analysis model */
    private PIAAnalysisModel analysisModel;

    protected PIAPSMSpectrumViewer(PIAAnalysisNodeModel nodeModel) {
        super(nodeModel);
        // creation in modelChanged, because it is called every time
    }


    @Override
    protected void modelChanged() {
        PIAAnalysisNodeModel nodeModel =
            (PIAAnalysisNodeModel)getNodeModel();
        assert nodeModel != null;

        analysisModel = nodeModel.loadAnalysisModelFromFile();
        psmSpectrumPanel = new PSMSpectrumViewerPanel(nodeModel.getFilteredPSMList(analysisModel),
                nodeModel.getPSMToSpectrum());
        setComponent(psmSpectrumPanel);
    }


    @Override
    protected void onClose() {
        psmSpectrumPanel = null;
        analysisModel = null;
    }


    @Override
    protected void onOpen() {
    }
}
