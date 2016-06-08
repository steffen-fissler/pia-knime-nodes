package de.mpc.pia.knime.nodes.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PSMTopIdentificationFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;

public class ObjectSerializer {


    private ObjectSerializer() {
        // not accidentally call this
    }


    /**
     * Write an object to a Base64 string.
     *
     * @param o
     * @return
     * @throws IOException
     */
    public static String serializeBase64(Serializable o) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream( baos );
            oos.writeObject(o);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {

        }

        return null;
    }


    /**
     * Read an object from Base64 string.
     *
     * @param s
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object unserializeFromBase64(String s) {
        try {
            byte [] data = Base64.getDecoder().decode(s);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return o;
        } catch (Exception e) {

        }

        return null;
    }



    /**
     * Serialize the given filter to an base64 encoded String
     * @param filter
     * @return
     */
    public static String serializeFilter(AbstractFilter filter) {
        Object[] stringRep = new Object[4];

        stringRep[0] = filter.getShortName();
        if (filter instanceof PSMScoreFilter) {
            stringRep[0] = PSMScoreFilter.prefix + ((PSMScoreFilter) filter).getScoreShortName();
        } else if (filter instanceof PSMTopIdentificationFilter) {
            stringRep[0] = PSMTopIdentificationFilter.prefix + ((PSMTopIdentificationFilter) filter).getScoreShortName();
        } else if (filter instanceof PeptideScoreFilter) {
            stringRep[0] = PeptideScoreFilter.prefix + ((PeptideScoreFilter) filter).getScoreShortName();
        }
        stringRep[1] = new Boolean(filter.getFilterNegate());
        stringRep[2] = filter.getFilterComparator().getName();
        stringRep[3] = filter.getFilterValue();

        String serializedFilter = ObjectSerializer.serializeBase64(stringRep);
        return serializedFilter;
    }


    /**
     * Unserialize the given base64 encoded filter
     *
     * @param serializedFilter
     * @return
     */
    public static AbstractFilter unserializeFilter(String serializedFilter) {
        Object stringRep = ObjectSerializer.unserializeFromBase64(serializedFilter);

        if (stringRep instanceof Object[]) {
            Object[] arrObj = (Object[])stringRep;

            String shortName = (String)arrObj[0];
            Boolean negateFilter = (Boolean)arrObj[1];
            String comparatorName = (String)arrObj[2];
            String filterValue = arrObj[3].toString();


            StringBuilder messageBuffer = new StringBuilder();
            AbstractFilter filter = null;

            if (shortName.startsWith(PSMScoreFilter.prefix) ||
                    shortName.startsWith(PSMTopIdentificationFilter.prefix) ||
                    shortName.startsWith(PeptideScoreFilter.prefix)) {
                FilterComparator comparator =
                        FilterComparator.getFilterComparatorByName(comparatorName);

                if (shortName.startsWith(PSMScoreFilter.prefix)) {
                    String scoreShort = shortName.substring(PSMScoreFilter.prefix.length());
                    filter = new PSMScoreFilter(comparator, negateFilter,
                            Double.parseDouble(filterValue), scoreShort);
                } else if (shortName.startsWith(PSMTopIdentificationFilter.prefix)) {
                    String scoreShort = shortName.substring(PSMTopIdentificationFilter.prefix.length());
                    filter = new PSMTopIdentificationFilter(comparator,
                            Integer.parseInt(filterValue), negateFilter, scoreShort);
                } else if (shortName.startsWith(PeptideScoreFilter.prefix)) {
                    String scoreShort = shortName.substring(PeptideScoreFilter.prefix.length());
                    filter = new PeptideScoreFilter(comparator, negateFilter,
                            Double.parseDouble(filterValue), scoreShort);
                }
            } else {
                filter = FilterFactory.newInstanceOf(shortName, comparatorName,
                        filterValue, negateFilter, messageBuffer);
            }

            return filter;
        }

        return null;
    }
}
