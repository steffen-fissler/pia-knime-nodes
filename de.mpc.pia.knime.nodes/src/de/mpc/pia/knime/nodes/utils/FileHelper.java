package de.mpc.pia.knime.nodes.utils;

import java.io.File;
import java.util.Random;

public class FileHelper {

    /**
     * Local random number generator to ensure uniqueness of file names.
     */
    private static Random randomNumberGenerator = new Random();


    /**
     * Private creator to avoid instantiation of this class
     */
    private FileHelper() {

    }


    /**
     * Creates a  temporary path with the given prefix
     *
     * @param prefix
     * @return
     */
    public static File createTempDirectory(String prefix) {
        int num = randomNumberGenerator.nextInt(Integer.MAX_VALUE);
        File dir = new File(System.getProperty("java.io.tmpdir")
                + File.separator
                + String.format("%s%06d", prefix, num));

        while (dir.exists()) {
            num = randomNumberGenerator.nextInt(Integer.MAX_VALUE);
            dir = new File(System.getProperty("java.io.tmpdir")
                    + File.separator
                    + String.format("%s%06d", num));
        }

        dir.mkdirs();
        dir.deleteOnExit();

        return dir;
    }
}
