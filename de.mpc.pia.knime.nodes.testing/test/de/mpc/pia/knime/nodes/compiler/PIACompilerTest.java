package de.mpc.pia.knime.nodes.compiler;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.mpc.pia.intermediate.compiler.PIACompiler;
import de.mpc.pia.intermediate.compiler.PIASimpleCompiler;

public class PIACompilerTest {

    private File inputFile1;
    private File inputFile2;

    private String piaIntermediateFileName = "PIACompilerTest.pia.xml";

    @Before
    public void initialize() throws Exception {
        // get the test files as input file port objects
        inputFile1 = new File(PIACompilerTest.class.getResource("/de/mpc/pia/knime/nodes/resources/55merge_mascot_full.mzid").getPath());
        inputFile2 = new File(PIACompilerTest.class.getResource("/de/mpc/pia/knime/nodes/resources/55merge_tandem.mzid").getPath());
    }


    @After
    public void tearDown() throws Exception {

    }


    /**
     * simply check, whether the compilation and writing it works
     * @throws IOException
     */
    @Test
    public void testPIACompilation() throws IOException {
        PIACompiler piaCompiler = new PIASimpleCompiler();

        assertEquals("Mascot file could not be parsed", true,
                piaCompiler.getDataFromFile("file1", inputFile1.getAbsolutePath(), null, null));
        
        assertEquals("X!Tandem file could not be parsed", true,
                piaCompiler.getDataFromFile("file2", inputFile2.getAbsolutePath(), null, null));

        System.err.println("file written");
        
        piaCompiler.buildClusterList();
        piaCompiler.buildIntermediateStructure();

        File piaIntermediateFile = File.createTempFile(piaIntermediateFileName, null);

        piaCompiler.writeOutXML(piaIntermediateFile.getAbsolutePath());

        assertEquals("File was not created", true,
                piaIntermediateFile.exists());

        piaIntermediateFile.delete();
    }
}
