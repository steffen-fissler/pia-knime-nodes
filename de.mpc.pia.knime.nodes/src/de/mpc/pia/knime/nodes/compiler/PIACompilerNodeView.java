package de.mpc.pia.knime.nodes.compiler;

import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "PIACompiler" Node.
 *
 *
 * @author Julian Uszkoreit
 */
public class PIACompilerNodeView extends NodeView<PIACompilerNodeModel> {

    /** textarea showing the file's information */
    private JTextArea informationArea;


    /**
     * Creates a new view.
     *
     * @param nodeModel The model (class: {@link PIACompilerNodeModel})
     */
    protected PIACompilerNodeView(final PIACompilerNodeModel nodeModel) {
        super(nodeModel);

        informationArea = new JTextArea(nodeModel.getInformationString(), 40, 80);
        JScrollPane scrollpane = new JScrollPane(informationArea);
        informationArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        setComponent(scrollpane);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        PIACompilerNodeModel nodeModel =
                (PIACompilerNodeModel)getNodeModel();
        assert nodeModel != null;

        informationArea.setText(nodeModel.getInformationString());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // TODO things to do when closing the view
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // TODO things to do when opening the view
    }

}
