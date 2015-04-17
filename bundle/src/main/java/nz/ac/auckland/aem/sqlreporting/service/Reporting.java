package nz.ac.auckland.aem.sqlreporting.service;

import nz.ac.auckland.aem.sqlreporting.dto.Report;

import javax.jcr.RepositoryException;
import java.util.List;

/**
 * @author Marnix Cook
 *
 * Interface definition of the reporting service implementation
 */
public interface Reporting {

    public static final String REPORT_PATH = "/etc/sqlreports";

    /**
     * Retrieves a list of reports from the JCR
     *
     * @return
     * @throws RepositoryException
     */
    public List<Report> getAllReports() throws RepositoryException;

    /**
     * Retrieve a single report instance or return null when doesn't exit
     *
     * @param reportName the report to retrieve
     * @return the report instance or null
     */
    public Report getReportByName(String reportName);
}
