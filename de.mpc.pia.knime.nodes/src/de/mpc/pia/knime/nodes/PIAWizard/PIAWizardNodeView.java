package de.mpc.pia.knime.nodes.PIAWizard;

import org.knime.core.node.NodeView;

import de.mpc.pia.knime.nodes.visualization.VisualizationPanel;

/**
 * <code>NodeView</code> for the "PIA Wizard" Node.
 *
 *
 * @author Julian Uszkoreit
 */
public class PIAWizardNodeView extends NodeView<PIAWizardNodeModel> {

	private VisualizationPanel m_visualization_panel;

	/**
	 * Creates a new view.
	 *
	 * @param nodeModel The model (class: {@link PIAWizardNodeModel})
	 */
	protected PIAWizardNodeView(final PIAWizardNodeModel nodeModel) {
		super(nodeModel);
		// TODO instantiate the components of the view here.

		if ((nodeModel != null) && (nodeModel.getPIAViewModel() != null))  {
			m_visualization_panel = new VisualizationPanel(nodeModel.getPIAViewModel().getPIAModeller());
		} else {
			m_visualization_panel = new VisualizationPanel(null);
		}
		setComponent(m_visualization_panel);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {

	    // TODO retrieve the new model from your nodemodel and
	    // update the view.
	    PIAWizardNodeModel nodeModel =
	        (PIAWizardNodeModel)getNodeModel();
	    assert nodeModel != null;

	    // be aware of a possibly not executed nodeModel! The data you retrieve
	    // from your nodemodel could be null, emtpy, or invalid in any kind.

	    if ((nodeModel != null) && (nodeModel.getPIAViewModel() != null))  {
		    m_visualization_panel.updateView(nodeModel.getPIAViewModel().getPIAModeller());
		} else {
			m_visualization_panel.updateView(null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {
		// TODO things to do when closing the view

	    PIAWizardNodeModel.logger.warn("in viewer: onClose()");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {
		// TODO things to do when opening the view

	    PIAWizardNodeModel.logger.warn("in viewer: onOpen()");
	}

}

