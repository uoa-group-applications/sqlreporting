package nz.ac.auckland.aem.sqlreporting.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Marnix Cook
 *
 * This class is able to generate a new Report instance based off of
 * the contents of a JCR node.
 */
public class ReportFactory {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReportFactory.class);

    /**
     * Creates a report node
     *
     * @param reportNode is the node to convert
     * @return is a report instance, or null when something went wrong
     */
    public static Report create(Node reportNode) {
        if (reportNode == null) {
            return null;
        }

        if (!validReportingNode(reportNode)) {
            LOG.error("Missing a parameter, not parsing the node");
            return null;
        }

        try {
            return new Report(
                    reportNode.getName(),
                    reportNode.getProperty("title").getString(),
                    reportNode.getProperty("query").getString(),
                    pairUp(reportNode.getProperty("columns").getValues())
                );
        }
        catch (RepositoryException rEx) {
            LOG.error("Something went wrong reading from the JCR", rEx);
        }

        return null;
    }

    /**
     * @return a mapified version of comma separated column-list.
     */
    private static Map<String, String> pairUp(Value[] columns) throws RepositoryException {
        Map<String, String> mapping = new LinkedHashMap<String, String>();
        for (Value column : columns) {
            String[] splitCol = column.getString().split(",");
            if (splitCol.length == 2) {
                mapping.put(splitCol[0].trim(), splitCol[1].trim());
            } else {
                mapping.put(splitCol[0].trim(), splitCol[0].trim());
            }
        }
        return mapping;
    }


    /**
     * @return true if all the necessary nodes are there
     */
    public static boolean validReportingNode(Node reportNode) {
        try {
            return
                    reportNode.hasProperty("title") &&
                    reportNode.hasProperty("query") &&
                    reportNode.hasProperty("columns");
}
        catch (RepositoryException rEx) {
            LOG.error("Error reading from the repository", rEx);
        }
        return false;

    }
}
