package de.mpc.pia.knime.nodes.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;
import de.mpc.pia.intermediate.piaxml.FilesListXML;
import de.mpc.pia.intermediate.piaxml.PIAInputFileXML;
import uk.ac.ebi.jmzidml.model.mzidml.AbstractParam;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.Enzyme;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
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

    /** the settings key for the input files */
    protected static final String CFGKEY_INPUT_COLUMN = "Input files";
    /** the settings key for the compilations name */
    protected static final String CFGKEY_NAME = "Name";

    /** initial default for the input files column */
    protected static final String DEFAULT_INPUT_COLUMN = "URL";
    /** initial default for the compilation name */
    protected static final String DEFAULT_NAME = "compilation";

    /** the model of the input files' URLs */
    private final SettingsModelString m_input_column =
            new SettingsModelString(PIACompilerNodeModel.CFGKEY_INPUT_COLUMN, PIACompilerNodeModel.DEFAULT_INPUT_COLUMN);
    /** the model of the compilation's name */
    private final SettingsModelString m_name =
            new SettingsModelString(PIACompilerNodeModel.CFGKEY_NAME, PIACompilerNodeModel.DEFAULT_NAME);

    /** information parsed from the created PIA XML file */
    private String informationString;


    private static final String INTERNALS_INFORMATION = "informationString";
    private static final String INTERNALS_FILE_NAME = "piaCompilerInternals";


    /**
     * Constructor for the node model.
     */
    protected PIACompilerNodeModel() {
        super(1, 1);
    }


    /**
     * {@inheritDoc}
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws IOException, InterruptedException  {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        // get the input files
        RowIterator row_it = inData[0].iterator();
        int url_idx  = inData[0].getDataTableSpec().findColumnIndex(m_input_column.getStringValue());

        while (row_it.hasNext()) {
            DataRow row = row_it.next();
            DataCell urlCell = row.getCell(url_idx);

            String fileURL = ((StringValue) urlCell).getStringValue();
            File file = null;
            try {
                // try with URL encoding
                URL url = new URL(fileURL);
                file = new File(url.toURI());
            } catch (Exception e) {
                file = null;
            }

            if ((file == null) || !file.exists() || !file.canRead()) {
                // try with "normal" file name
                file = new File(fileURL);
            }

            if ((file == null) || !file.exists() || !file.canRead()) {
                throw new  IOException("Could not open file: " + fileURL);
            }

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
        } else {
            piaCompiler.setName(DEFAULT_NAME);
        }


        // write the file directly into the binary cell object, gzipping it along the way
        PipedInputStream inputStream = new PipedInputStream();

        Runnable writingRunner = new WritingPipeRunnable(logger, inputStream, piaCompiler);
        Thread writingThread = new Thread(writingRunner);
        writingThread.start();


        BinaryObjectCellFactory bofactory = new BinaryObjectCellFactory(exec);
        logger.debug("starting creation of dataCell");
        DataCell zippedXMLFileCell = bofactory.create(inputStream);
        logger.debug("dataCell created");

        // the stream should be finished after the creation of the dataCell
        inputStream.close();
        logger.debug("pipedInputStream closed");


        List<DataCell> dataCells = new ArrayList<DataCell>();
        dataCells.add(zippedXMLFileCell);

        BufferedDataContainer container = exec.createDataContainer(createTableSpec());
        container.addRowToTable(new DefaultRow(piaCompiler.getName(), dataCells));
        container.close();

        informationString = parseZippedXMLFileInformation(zippedXMLFileCell);

        if (informationString.trim().length() < 1) {
            throw new IllegalStateException("The created XML file could not be parsed.");
        }

        return new BufferedDataTable[]{container.getTable()};
    }


    /**
     * This class handles the writing of the piaCompiler output to the GZipped
     * piped input stream.
     *
     * @author julian
     *
     */
    private class WritingPipeRunnable implements Runnable {
        private NodeLogger logger;

        private PipedOutputStream outStream;
        private GZIPOutputStream zipStream;
        private PIACompiler piaCompiler;
        private OutputStreamWriter osw;

        public WritingPipeRunnable(NodeLogger logger, PipedInputStream inStream,
                PIACompiler piaCompiler) throws IOException {
            this.logger = logger;

            this.outStream = new PipedOutputStream(inStream);
            this.zipStream = new GZIPOutputStream(this.outStream);
            this.osw = new OutputStreamWriter(this.zipStream, StandardCharsets.UTF_8);

            this.piaCompiler = piaCompiler;
        }

        public void run() {
            try {
                piaCompiler.writeOutXML(zipStream);

                osw.flush();
                zipStream.flush();
                outStream.flush();

                osw.close();
                logger.debug("outputStreamWriter closed");
                zipStream.close();
                logger.debug("zipStream closed");
                outStream.close();
                logger.debug("pipedOutputStream closed");
            } catch (IOException e) {
                logger.error("Error writing the gzipped file", e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        informationString = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec[] tableSpec = new DataTableSpec[1];
        tableSpec[0] = createTableSpec();

        return tableSpec;
    }


    /**
     * Creates the returned table specifications
     *
     * @return
     */
    private DataTableSpec createTableSpec() {
        DataType type = DataType.getType(BinaryObjectDataCell.class);

        List<DataColumnSpec> compilerCols = new ArrayList<DataColumnSpec>();
        compilerCols.add(new DataColumnSpecCreator("gzipped PIA XML file", type).createSpec());
        DataTableSpec compilerSpecTable = new DataTableSpec(compilerCols.toArray(new DataColumnSpec[]{}));

        return compilerSpecTable;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_input_column.saveSettingsTo(settings);
        m_name.saveSettingsTo(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_input_column.loadSettingsFrom(settings);
        m_name.loadSettingsFrom(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_input_column.validateSettings(settings);
        m_name.validateSettings(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        File f = new File(nodeInternDir, INTERNALS_FILE_NAME);
        FileInputStream fis = new FileInputStream(f);

        NodeSettingsRO settings = NodeSettings.loadFromXML(fis);
        informationString = settings.getString(INTERNALS_INFORMATION, null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        NodeSettings settings = new NodeSettings(INTERNALS_FILE_NAME);
        settings.addString(INTERNALS_INFORMATION, informationString);

        File f = new File(nodeInternDir, INTERNALS_FILE_NAME);
        FileOutputStream fos = new FileOutputStream(f);
        settings.saveToXML(fos);
    }


    /**
     * Returns information about the GZipped PIA XML file
     */
    private String parseZippedXMLFileInformation(DataCell dataCell) {
        BinaryObjectDataValue boDataCell = null;
        if (dataCell instanceof BinaryObjectDataValue) {
            boDataCell = (BinaryObjectDataValue) dataCell;
        } else {
            return "Wrong object class, expected BinaryObjectDataValue, is " + dataCell.getClass().getCanonicalName();
        }

        StringBuffer textSB = new StringBuffer();
        String projectName = null;

        StreamSource inputSource = null;
        XMLStreamReader xmlr = null;

        // the files and analysissoftware information is stored in memory (should be small)
        FilesListXML filesList = null;
        AnalysisSoftwareList softwareList = null;


        try {
            InputStream inputStream = boDataCell.openInputStream();
            inputStream = new GZIPInputStream(inputStream);
            InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            inputSource = new StreamSource(isr);

            PIACompilerNodeModel.logger.debug("opened gzipped file for parsing");
            XMLInputFactory xif = XMLInputFactory.newInstance();

            // set up a StAX reader
            xmlr = xif.createXMLStreamReader(inputSource);

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

            if (ex.getNestedException() != null) {
                PIACompilerNodeModel.logger.error("Error while parsing XML file", ex.getNestedException());
            } else {
                PIACompilerNodeModel.logger.error("Error while parsing XML file", ex);
            }
        } catch (JAXBException ex) {
            PIACompilerNodeModel.logger.error("Error while parsing XML file", ex);
        } catch (IOException e) {
            PIACompilerNodeModel.logger.error("Error while opening binary object", e);
        } finally {
            if (xmlr != null) {
                try {
                    xmlr.close();
                    PIACompilerNodeModel.logger.debug("closed XMLStreamReader");
                } catch (Exception e) {
                    PIACompilerNodeModel.logger.error("Error while closing XML file", e);
                }
            }

            if (inputSource != null) {
                try {
                    inputSource.getReader().close();
                    PIACompilerNodeModel.logger.debug("closed InputSource's Reader");
                    //inputSource.getInputStream().close();
                    //PIACompilerNodeModel.logger.debug("closed InputSource's stream");
                } catch (Exception e) {
                    PIACompilerNodeModel.logger.error("Error while closing input file/gzip", e);
                }
            }
        }

        PIACompilerNodeModel.logger.debug("Parsed information from text, creating information string.");

        // parsing the file is done, process the information
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

        PIACompilerNodeModel.logger.debug("Created information from zipped PIA XML file succesfully.");

        return textSB.toString();
    }


    /**
     * Getter for the information string, which is created on the execution.
     *
     * @return
     */
    public String getInformationString() {
        return informationString;
    }
}

