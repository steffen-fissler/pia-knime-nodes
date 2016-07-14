package de.mpc.pia.knime.nodes.compiler;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

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

        addDialogComponent(new DialogComponentColumnNameSelection(
                new SettingsModelString(PIACompilerNodeModel.CFGKEY_INPUT_COLUMN,
                        PIACompilerNodeModel.DEFAULT_INPUT_COLUMN),
                PIACompilerNodeModel.CFGKEY_INPUT_COLUMN, 0, true, false, new ColumnFilter() {

                    @Override
                    public boolean includeColumn(DataColumnSpec colSpec) {
                        if (colSpec.getType().isCompatible(StringValue.class)) {
                                return true;
                        }

                        return false;
                    }

                    @Override
                    public String allFilteredMsg() {
                        return "No possible column with file URL in input data.";
                    }
                }));

        addDialogComponent(new DialogComponentString(
                new SettingsModelString(PIACompilerNodeModel.CFGKEY_NAME, PIACompilerNodeModel.DEFAULT_NAME),
                PIACompilerNodeModel.CFGKEY_NAME));
    }
}

