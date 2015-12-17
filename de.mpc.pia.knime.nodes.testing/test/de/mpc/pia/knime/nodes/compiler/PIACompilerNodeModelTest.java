package de.mpc.pia.knime.nodes.compiler;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIPortObject;
import org.knime.core.node.port.PortObject;

import de.mpc.pia.knime.nodes.compiler.PIACompilerNodeFactory;
import de.mpc.pia.knime.nodes.compiler.PIACompilerNodeModel;

public class PIACompilerNodeModelTest {

    private PIACompilerNodeModel nodeModel = null;
    private PIACompilerNodeFactory nodeFactory = null;

    private PortObject[] inPorts;


    @Before
    public void initialize() throws Exception {
        nodeFactory = new PIACompilerNodeFactory();
        nodeModel = nodeFactory.createNodeModel();

        // get the test files as input file port objects
        List<URIContent> uriContents = new ArrayList<URIContent>();
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        URL url = classloader.getResource("/de/mpc/pia/knime/nodes/resources/55merge_mascot_full.mzid");
        URI uri = FileLocator.resolve(url).toURI();
        uriContents.add(new URIContent(uri, "mzid"));

        url = classloader.getResource("/de/mpc/pia/knime/nodes/resources/55merge_tandem.mzid");
        uri = FileLocator.resolve(url).toURI();
        uriContents.add(new URIContent(uri, "mzid"));

        URIPortObject inFiles = new URIPortObject(uriContents);
        inPorts = new PortObject[]{inFiles};
    }


    @After
    public void tearDown() throws Exception {

    }


    /**
     * simple test for the node execution, needs write access in the OS's tmp path
     */
    @Test
    public void testExecutePortObjectArrayExecutionContext() {
        try {
            nodeModel.execute(inPorts, null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error while performing execute()");
        }
    }

    /**
     * tests the results of the parseXMLFileInformation
     */
    @Test
    public void testParseXMLFileInformation() {
        PortObject[] portObject = null;

        try {
            portObject = nodeModel.execute(inPorts, null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error while performing execute()");
        }

        assertNotNull("no object created during execution", portObject);

        String fileInfo = nodeModel.parseXMLFileInformation();

        assertEquals("mascot not found", true, fileInfo.contains("used software: Mascot"));
        assertEquals("x!tandem not found", true, fileInfo.contains("used software: X\\!Tandem"));

        assertEquals("file ID 1 not found", true, fileInfo.contains("ID: 1"));
        assertEquals("file ID 2 not found", true, fileInfo.contains("ID: 2"));

        assertEquals("too many file IDs found", 2, StringUtils.countMatches(fileInfo, "ID: "));
        assertEquals("not found exactly two references to trypsin", 2, StringUtils.countMatches(fileInfo,"enzymes: Trypsin"));
    }


    /**
     * tests the correctness of the generated PIA XML file by counting the
     * number of certain elements
     */
    @Test
    public void testParseXMLFilesContent() {
        try {
            nodeModel.execute(inPorts, null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error while performing execute()");
        }

        String piaXmlFileName = nodeModel.getPiaXMLFileName();
        if (piaXmlFileName == null) {
            fail("No file was created");
        }

        XMLInputFactory xmlif = null;
        XMLStreamReader xmlr = null;

        int nrSpectra = 0;
        int nrAccessions = 0;
        int nrPeptides = 0;
        Set<Integer> treeIDs = new HashSet<Integer>();

        try {
            // set up a StAX reader
            xmlif = XMLInputFactory.newInstance();
            xmlr = xmlif.createXMLStreamReader(new FileReader(piaXmlFileName));

            // move to the root element and check its name.
            xmlr.nextTag();
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "jPiaXML");

            while (xmlr.hasNext()) {
                int event = xmlr.next();

                switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (xmlr.getLocalName()) {
                    case "spectrumMatch":
                        nrSpectra++;
                        break;

                    case "accession":
                        nrAccessions++;
                        break;

                    case "peptide":
                        nrPeptides++;
                        break;

                    case "group":
                        for (int i=0; i < xmlr.getAttributeCount(); i++) {
                            if (xmlr.getAttributeName(i).toString().equalsIgnoreCase("treeId")) {
                                treeIDs.add(Integer.parseInt(xmlr.getAttributeValue(i)));
                            }
                        }
                        break;
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    break;
                }
            }
        } catch (XMLStreamException ex) {
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

        assertEquals("wrong number of spectra", 2478, nrSpectra);
        assertEquals("wrong number of accessions", 2131, nrAccessions);
        assertEquals("wrong number of peptides", 2113, nrPeptides);
        assertEquals("wrong number of trees/clusters", 1856, treeIDs.size());
    }
}
