package de.mpc.pia.knime.nodes.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIPortObject;
import org.knime.core.data.uri.URIPortObjectSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.piaxml.FilesListXML;
import de.mpc.pia.intermediate.piaxml.PIAInputFileXML;
import de.mpc.pia.knime.nodes.filestore.FileStorePIAXMLPortObject;
import de.mpc.pia.knime.nodes.utils.FileHelper;
import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of PIACompiler.
 *
 *
 * @author Julian Uszkoreit
 */
public class PIACompilerNodeModel extends NodeModel {

    // the logger instance
    protected static final NodeLogger logger = NodeLogger
            .getLogger(PIACompilerNodeModel.class);

    /** the settings key for the compilations name */
    static final String CFGKEY_NAME = "Name";

    /** initial default count value. */
    static final String DEFAULT_NAME = "";

    /** the model of the compilation's name */
    private final SettingsModelString m_name =
            new SettingsModelString(PIACompilerNodeModel.CFGKEY_NAME, PIACompilerNodeModel.DEFAULT_NAME);

    /** the name of the created PIA XML file */
    private String piaXmlFileName;


    /** the basic save name of the compilation  */
    private static final String piaXmlBaseName = "compilation.pia.xml";


    /**
     * Constructor for the node model.
     */
    protected PIACompilerNodeModel() {
        super(new PortType[]{ IURIPortObject.TYPE },
                new PortType[] { IURIPortObject.TYPE });

        piaXmlFileName = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        piaXmlFileName = null;
        PIACompiler piaCompiler = new PIACompiler();

        // get the input files
        IURIPortObject filePort = (IURIPortObject) inObjects[0];
        List<URIContent> uris = filePort.getURIContents();
        for (URIContent uric : uris) {
            URI uri = uric.getURI();
            File file = new File(uri);
            String fileName = file.getAbsolutePath();
            String name = file.getName();

            piaCompiler.getDataFromFile(name, fileName, null, null);
        }

        piaCompiler.buildClusterList();

        piaCompiler.buildIntermediateStructure();

        // set the compilations name (if given)
        String piaName = m_name.getStringValue();
        if ((piaName != null) && (piaName.trim().length() > 0)) {
            piaCompiler.setName(piaName);
            logger.debug("set name of compilation to '" + piaName + "'");
        }

        File dir = FileHelper.createTempDirectory("PIACompiler");

        File outFile = new File(dir, piaXmlBaseName);
        piaCompiler.writeOutXML(outFile.getAbsolutePath());

        // set the piaXmlFileName after writing the file
        piaXmlFileName = outFile.getAbsolutePath();


        FileStoreFactory fileStoreFactory = FileStoreFactory.createNotInWorkflowFileStoreFactory();
        FileStore fileStore = fileStoreFactory.createFileStore(outFile.getName());
        List<FileStore> storeList = new ArrayList<FileStore>();
        storeList.add(fileStore);
        FileStorePIAXMLPortObject piaXMLFileObject = new FileStorePIAXMLPortObject(storeList, outFile);

        piaXmlFileName = piaXMLFileObject.getFileName();

        return new PortObject[]{piaXMLFileObject};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        piaXmlFileName = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PortObjectSpec[] out_spec = new PortObjectSpec[1];
        out_spec[0] = new URIPortObjectSpec(new String[]{"pia.xml"});

        return out_spec;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_name.saveSettingsTo(settings);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_name.loadSettingsFrom(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_name.validateSettings(settings);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TODO: load the standard and error output data here
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TODO: save the standard and error output data here
    }


    /**
     * Parses some file information from the created PIA XML file and writes
     * it into the returned String.
     */
    public String parseXMLFileInformation() {
        if (piaXmlFileName == null) {
            return null;
        }

        StringBuffer textSB = new StringBuffer();
        String projectName = null;

        textSB.append("Filename: ");
        textSB.append(piaXmlFileName);

        XMLInputFactory xmlif = null;
        XMLStreamReader xmlr = null;

        // the files and analysissoftware information is stored in memory (should be small)
        FilesListXML filesList = null;
        AnalysisSoftwareList softwareList = null;

        try {
            // set up a StAX reader
            xmlif = XMLInputFactory.newInstance();
            xmlr = xmlif.createXMLStreamReader(new FileReader(piaXmlFileName));

            // move to the root element and check its name.
            xmlr.nextTag();
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "jPiaXML");

            // get project attributes
            for (int attrIdx=0; attrIdx < xmlr.getAttributeCount(); attrIdx++) {
                if (xmlr.getAttributeName(attrIdx).toString().equals("name")) {
                    projectName = xmlr.getAttributeValue(attrIdx);
                }
            }

            if (projectName != null) {
                textSB.append("\n");
                textSB.append("Project name: ");
                textSB.append(projectName);
            }

            // move to the first not-root element
            xmlr.nextTag();
            while (xmlr.hasNext()) {
                String tag = xmlr.getLocalName();

                if (tag.equalsIgnoreCase("filesList")) {
                    logger.debug("parsing " + tag);
                    JAXBContext jaxbContext = JAXBContext.newInstance(FilesListXML.class);
                    Unmarshaller um = jaxbContext.createUnmarshaller();
                    JAXBElement<FilesListXML> umFilesList = um.unmarshal(xmlr, FilesListXML.class);
                    filesList = umFilesList.getValue();
                } else if (tag.equalsIgnoreCase("AnalysisSoftwareList")) {
                    logger.debug("parsing " + tag);
                    JAXBContext jaxbContext = JAXBContext.newInstance(AnalysisSoftwareList.class);
                    Unmarshaller um = jaxbContext.createUnmarshaller();
                    JAXBElement<AnalysisSoftwareList> umSoftwareList = um.unmarshal(xmlr, AnalysisSoftwareList.class);
                    softwareList = umSoftwareList.getValue();
                } else {
                    // skipping unneeded tags
                    if (xmlr.hasNext()) {
                        xmlr.nextTag();
                    }
                }

                if ((filesList != null) && (softwareList != null)) {
                    break;
                }

                // skip the whitespace between tags
                if (xmlr.getEventType() == XMLStreamConstants.CHARACTERS) {
                    xmlr.next();
                }

                // check, if end of file reached
                if (xmlr.getLocalName().equalsIgnoreCase("jPiaXML") &&
                        (xmlr.getEventType() == XMLStreamConstants.END_ELEMENT)) {
                    // finished, reached the closing tag of jPiaXML
                    break;
                }
            }

        } catch (XMLStreamException ex) {
            PIACompilerNodeModel.logger.error("Error while parsing XML file", ex);
        } catch (JAXBException ex) {
            PIACompilerNodeModel.logger.error("Error while parsing XML file", ex);
        } catch (FileNotFoundException ex) {
            PIACompilerNodeModel.logger.error("File not found: " + piaXmlFileName, ex);
        } finally {
            if (xmlr != null) {
                try {
                    xmlr.close();
                } catch (XMLStreamException e) {
                    PIACompilerNodeModel.logger.error("Error while closing XML file", e);
                }
            }
        }

        // parsing the file is done, process teh information
        if ((filesList != null) && (softwareList != null)) {
            // create a hashmap for the used software
            HashMap<String, String> softwareMap = new HashMap<String, String>();

            if (softwareList.getAnalysisSoftware() != null) {
                ListIterator<AnalysisSoftware> softwareIter = softwareList.getAnalysisSoftware().listIterator();

                while (softwareIter.hasNext()) {
                    AnalysisSoftware software = softwareIter.next();

                    String name = null;
                    if (software.getSoftwareName().getCvParam() != null) {
                        name = software.getSoftwareName().getCvParam().getName();
                    } else if (software.getSoftwareName().getUserParam() != null) {
                        name = software.getSoftwareName().getUserParam().getName();
                    }

                    softwareMap.put(software.getId(), name);
                }
            }

            if (filesList.getFiles() != null) {
                // gets the information for the input files
                ListIterator<PIAInputFileXML> fileIter = filesList.getFiles().listIterator();

                while (fileIter.hasNext()) {
                    PIAInputFileXML inputFileXML = fileIter.next();

                    textSB.append("\n\nName: ");
                    textSB.append(inputFileXML.getName());
                    textSB.append("\nFile path: ");
                    textSB.append(inputFileXML.getFileName());
                    textSB.append("\nID: ");
                    textSB.append(inputFileXML.getId());


                    for (SpectrumIdentificationProtocol prot : inputFileXML.getAnalysisProtocolCollection().getSpectrumIdentificationProtocol()) {
                        textSB.append("\nused software: ");
                        textSB.append(softwareMap.get(prot.getAnalysisSoftwareRef()));

                        if (prot.getEnzymes() != null) {
                            textSB.append("\n\tenzymes: ");
                            for (Enzyme enzyme : prot.getEnzymes().getEnzyme()) {
                                if (enzyme.getEnzymeName() != null) {
                                    for (AbstractParam param : enzyme.getEnzymeName().getParamGroup()) {
                                        textSB.append(param.getName());
                                        textSB.append(" ");
                                    }
                                }
                            }
                        }

                        if (prot.getModificationParams() != null) {
                            textSB.append("\n\tmodifications: ");
                            for (SearchModification mod : prot.getModificationParams().getSearchModification()) {
                                textSB.append("\n\t\t");
                                textSB.append(mod.getMassDelta());
                                textSB.append("@");
                                textSB.append(mod.getResidues());
                            }
                        }
                    }
                }
            }
        }

        return textSB.toString();
    }


    /**
     * getter for the piaXmlFileName
     * @return
     */
    public String getPiaXMLFileName() {
        return piaXmlFileName;
    }
}

