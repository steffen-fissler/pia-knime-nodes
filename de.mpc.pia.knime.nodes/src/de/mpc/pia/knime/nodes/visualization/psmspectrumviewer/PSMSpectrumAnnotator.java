package de.mpc.pia.knime.nodes.visualization.psmspectrumviewer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.mpc.pia.knime.nodes.visualization.ReportTableCellRenderer;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.visualization.spectra.SpectrumVisualizer;
import uk.ac.ebi.pride.toolsuite.mzgraph.SpectrumBrowser;
import uk.ac.ebi.pride.utilities.data.core.Spectrum;
import uk.ac.ebi.pride.utilities.mol.ProductIonPair;

/**
 * Visualize a PSM with its automatically annotated spectrum.
 *
 * @author julian
 *
 */
public class PSMSpectrumAnnotator {

    /**
     * Do not instantiate this class
     */
    private PSMSpectrumAnnotator() {
    }


    /**
     * Visualize the given {@link ReportPSM} with the given {@link Spectrum} in
     * the {@link JPanel}.
     *
     * @param psm
     * @param spectrum
     * @param visualizerPanel
     */
    public static void annotateSpectrumInPanel(ReportPSM psm, Spectrum spectrum, JPanel visualizerPanel) {
        visualizerPanel.removeAll();

        if (psm != null) {
            visualizerPanel.setLayout(new GridBagLayout());
            int row = 0;
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(5, 5, 5, 5);

            // sequence >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            c.gridx = 0;
            c.gridy = row;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.FIRST_LINE_START;
            c.fill = GridBagConstraints.NONE;
            visualizerPanel.add(new JLabel("Sequence:"), c);
            c.gridx = 1;
            c.gridy = row++;
            c.fill = GridBagConstraints.HORIZONTAL;
            JTextField sequenceField = new JTextField(psm.getSequence());
            sequenceField.setEditable(false);
            visualizerPanel.add(sequenceField, c);
            // sequence <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

            // scores >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            c.gridx = 0;
            c.gridy = row;
            c.fill = GridBagConstraints.NONE;
            visualizerPanel.add(new JLabel("Scores:"), c);

            JPanel scores = new JPanel(new GridLayout(psm.getScores().size(), 2));
            for (ScoreModel score : psm.getScores()) {
                scores.add(new JLabel(score.getName()));
                scores.add(new JLabel(ReportTableCellRenderer.getStringOfNumber(score.getValue())));
            }

            c.gridx = 1;
            c.gridy = row++;
            c.fill = GridBagConstraints.NONE;
            visualizerPanel.add(scores, c);
            // scores <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

            // spectrum browser >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            c.gridx = 0;
            c.gridy = row++;
            c.gridwidth = 2;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.BOTH;

            if (spectrum != null) {
                SpectrumBrowser specBrowser = SpectrumVisualizer.createSpectrumBrowser(spectrum, psm, ProductIonPair.B_Y);
                visualizerPanel.add(specBrowser, c);
            } else {
                visualizerPanel.add(new JLabel("no spectrum match for selected PSM"));
            }
            // spectrum browser <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        } else {
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(5, 5, 5, 5);
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 0;

            if (psm == null) {
                visualizerPanel.add(new JLabel("no PSM given"));
            }
        }

        visualizerPanel.revalidate();
    }
}
