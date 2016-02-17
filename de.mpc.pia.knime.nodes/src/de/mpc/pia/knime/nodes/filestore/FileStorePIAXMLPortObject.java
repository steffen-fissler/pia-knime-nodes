package de.mpc.pia.knime.nodes.filestore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIPortObjectSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.ModelContentOutPortView;

/**
 * A store object which saves exactly one PIA XML file.
 *
 * <p>
 * Much code is used copied and adjusted from com.genericworkflownodes.knime.base.data.port.AbstractFileStoreURIPortObject
 *
 * @author julian
 *
 */
public class FileStorePIAXMLPortObject extends FileStorePortObject implements IURIPortObject {

    /** a PIA XML file is always a pia.xml file */
    private final static URIPortObjectSpec fileSpec = new URIPortObjectSpec(new String[]{"pia.xml"});

    /**
     * The key of the rel-path setting stored while loading/saving.
     */
    private static final String SETTINGS_KEY_REL_PATH = "rel-path";


    /** the file stored in this port object */
    private URIContent m_uriContent;

    /** te actual file name of the file (without path) */
    private String m_relPath;


    public FileStorePIAXMLPortObject() {
    }


    public FileStorePIAXMLPortObject(final List<FileStore> fileStores, File srcFile) {
        super(fileStores);

        if (fileStores.size() > 0) {
            File piaXMLFile = fileStores.get(0).getFile();
            m_uriContent = new URIContent(piaXMLFile.toURI(), "pia.xml");
            m_relPath = piaXMLFile.getAbsolutePath();

            try {
                Files.move(srcFile.toPath(), piaXMLFile.toPath());
            } catch (IOException ex) {
                throw new IllegalStateException("Could not copy the created PIA XML file", ex);
            }
        } else {
            throw new IllegalStateException("No file given in fileStores!");
        }
    }


    @Override
    public String getSummary() {
        // TODO Auto-generated method stub
        return "this is a summery of the FileStorePIAXMLPortObject";
    }


    @Override
    public JComponent[] getViews() {
        try {
            ModelContent model = new ModelContent("Model Content");
            save(model, new ExecutionMonitor());
            return new JComponent[] { new ModelContentOutPortView(model) };
        } catch (CanceledExecutionException ex) {
            // shouldn't happen
        }
        return null;
    }


    @Override
    public List<URIContent> getURIContents() {
        List<URIContent> contents = new ArrayList<>();
        contents.add(m_uriContent);
        return contents;
    }


    @Override
    public URIPortObjectSpec getSpec() {
        return fileSpec;
    }


    /**
     * Save the currently managed files as model content.
     *
     * @param model
     *            The {@link ModelContentWO} object to fill with the list of
     *            files.
     * @param exec
     *            The associated execution context.
     */
    void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        // store the managed URIs
        if (m_uriContent != null) {
            ModelContentWO child = model.addModelContent("file-0");
            m_uriContent.save(child);
            child.addString(SETTINGS_KEY_REL_PATH, m_relPath);
        }
    }


    /**
     * Reconstruct the {@link AbstractFileStoreURIPortObject} from the given
     * {@link ModelContentRO}.
     *
     * @param model
     *            The {@link ModelContentRO} from where the object should be
     *            reconstructed.
     * @param spec
     *            The expected {@link PortObjectSpec}.
     * @param exec
     *            The current {@link ExecutionContext}.
     * @throws InvalidSettingsException
     *             Thrown if the content is invalid.
     */
    void load(final ModelContentRO model, PortObjectSpec spec, ExecutionMonitor exec)
            throws InvalidSettingsException {
        List<URIContent> list = new ArrayList<URIContent>();
        List<String> relPathList = new ArrayList<String>();
        for (String key : model.keySet()) {
            if (key.startsWith("file-")) {
                ModelContentRO child = model.getModelContent(key);
                list.add(URIContent.load(child));
                relPathList.add(child.getString(SETTINGS_KEY_REL_PATH));
            }
        }

        m_uriContent = list.get(0);
        m_relPath = relPathList.get(0);
    }


    /**
     * Access to the PortObjectSerializer.
     *
     * @return The PortObjectSerializer.
     */
    public static final PortObjectSerializer<FileStorePIAXMLPortObject> getPortObjectSerializer() {
        return FileStorePIAXMLPortObjectSerializer.getSerializer();
    }


    /**
     * Getter for the filename of the stored PIA XML file.
     *
     * @return
     */
    public String getFileName() {
        if (m_uriContent != null) {
            return m_uriContent.getURI().getPath();
        }
        return null;
    }
}
