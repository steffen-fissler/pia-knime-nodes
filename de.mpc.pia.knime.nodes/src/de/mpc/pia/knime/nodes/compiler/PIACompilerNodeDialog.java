package de.mpc.pia.knime.nodes.compiler;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "PIACompiler" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Julian Uszkoreit
 */
public class PIACompilerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring PIACompiler node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected PIACompilerNodeDialog() {
        super();

        addDialogComponent(new DialogComponentString(
                new SettingsModelString(PIACompilerNodeModel.CFGKEY_NAME, PIACompilerNodeModel.DEFAULT_NAME),
                "Name:"));
    }
}

