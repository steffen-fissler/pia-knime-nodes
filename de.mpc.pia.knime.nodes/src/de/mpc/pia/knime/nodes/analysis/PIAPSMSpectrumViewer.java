package de.mpc.pia.knime.nodes.analysis;

import org.knime.core.node.NodeView;

import de.mpc.pia.knime.nodes.visualization.psmspectrumviewer.PSMSpectrumViewerPanel;


/**
 * View for the PSM spectrum visualization.
 *
 * @author julian
 *
 */
public class PIAPSMSpectrumViewer extends NodeView<PIAAnalysisNodeModel> {

    /** The actually shown spectrum viewer panel */
    private PSMSpectrumViewerPanel psmSpectrumPanel;


    protected PIAPSMSpectrumViewer(PIAAnalysisNodeModel nodeModel) {
        super(nodeModel);

        psmSpectrumPanel = new PSMSpectrumViewerPanel(nodeModel.getFilteredPSMList(),
                nodeModel.getPSMToSpectrum());
        setComponent(psmSpectrumPanel);
    }


    @Override
    protected void modelChanged() {
        PIAAnalysisNodeModel nodeModel =
            (PIAAnalysisNodeModel)getNodeModel();
        assert nodeModel != null;

        psmSpectrumPanel = new PSMSpectrumViewerPanel(nodeModel.getFilteredPSMList(),
                nodeModel.getPSMToSpectrum());
    }


    @Override
    protected void onClose() {
        psmSpectrumPanel = null;
    }


    @Override
    protected void onOpen() {
    }
}
