package nz.ac.auckland.aem.sqlreporting;

import nz.ac.auckland.aem.contentgraph.dbsynch.DatabaseSynchronizer;
import nz.ac.auckland.aem.contentgraph.dbsynch.services.helper.ConnectionInfo;
import nz.ac.auckland.aem.contentgraph.dbsynch.services.helper.Database;
import nz.ac.auckland.aem.sqlreporting.dto.Report;
import nz.ac.auckland.aem.sqlreporting.service.Reporting;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marnix Cook
 *
 * Reporting choice servlet allows the user to find the report
 * they want to run.
 */
@SlingServlet(
    paths = "/bin/sqlreporting/choose.do",
    methods = {"GET", "POST"}
)
public class ReportingServlet extends SlingAllMethodsServlet {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReportingServlet.class);

    @Reference
    private DatabaseSynchronizer dbSynch;

    /**
     * Bind reporting service here
     */
    @Reference
    private Reporting reporting;

    /**
     * Output a list of reports that can be ran
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        try {
            response.getWriter().write(
                "<html>" +
                    "<body>" +
                        "<h1>SQL reporting</h1>" +
                        "<p>Which report do you want to run?</p>" +
                        "<form action='?' method='post'>" +
                            toSelectList(reporting.getAllReports()) +
                            "<button type='submit'>Run</button>" +
                        "</form>" +
                    "</body>" +
                "</html>"
            );
        }
        catch (RepositoryException rEx) {
            LOG.error("Something went wrong retrieving the reports", rEx);
        }
    }

    /**
     * Run the report and output it properly
     *
     * @param request
     * @param response
     *
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String reportName = request.getParameter("reportName");
        if (StringUtils.isBlank(reportName)) {
            LOG.error("No reportName parameter provided, aborting.");
            response.setStatus(404);
            return;
        }

        Report report = reporting.getReportByName(reportName);
        if (report == null) {
            LOG.error("No report by name `{}` found", reportName);
            response.setStatus(404);
            return;
        }

        // make it a download
        response.setHeader(
                "Content-Disposition",
                String.format("attachment; filename=\"%s\"", getFileName(reportName))
        );

        PrintWriter writer = response.getWriter();

        ConnectionInfo connInfo = dbSynch.getConnectionInfo();
        Database db = null;
        try {
            db = new Database(connInfo);
            PreparedStatement pStmt = db.preparedStatement(report.getQuery());
            ResultSet rSet = pStmt.executeQuery();

            // mapping
            Map<String, Integer> nameIdMapping = mapColumns(report.getLabels(), rSet);
            outputReportHeaders(report, writer);


            // iterate over all columns
            while (rSet.next()) {
                outputReportResultRow(report, writer, rSet, nameIdMapping);
            }

            rSet.close();
        }
        catch (SQLException sqlEx) {
            LOG.error("Something went wrong with the database", sqlEx);
        }
        finally {
            if (db != null) {
                db.close();
            }
        }
    }

    protected String getFileName(String reportName) {
        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");
        return
            String.format("%s-%s.tsv",
                reportName,
                sdFormat.format(Calendar.getInstance().getTime())
            );
    }

    /**
     * Output one line of the report sourced from the resultset
     */
    protected void outputReportResultRow(Report report, PrintWriter writer, ResultSet rSet, Map<String, Integer> nameIdMapping) throws SQLException {
        int idx = 0;

        Map<String, String> labels = report.getLabels();

        for (Map.Entry<String, String> labelEntry : labels.entrySet()) {
            Integer columnIdx = nameIdMapping.get(labelEntry.getKey());

            if (columnIdx == null) {
                continue;
            }

            writer.print(rSet.getString(columnIdx));
            ++idx;
            if (idx != labels.size()) {
                writer.print("\t");
            }
        }

        writer.println();
    }

    /**
     * Output the headers
     */
    protected void outputReportHeaders(Report report, PrintWriter writer) {
        // write header
        int idx = 0;
        for (Map.Entry<String, String> labelEntry : report.getLabels().entrySet()) {
            writer.print(labelEntry.getValue());

            ++idx;
            if (idx != report.getLabels().size()) {
                writer.print("\t");
            }
        }
        writer.println();
    }

    /**
     * Sets up a map that maps the column indices to the column names that were specified in
     * the query.
     *
     * @param labels are the labels setup in the map
     * @param rSet the resultset to analyze.
     * @return (column columnIdx) mapping
     * @throws SQLException
     */
    protected Map<String, Integer> mapColumns(Map<String, String> labels, ResultSet rSet) throws SQLException {
        Map<String, Integer> columnMap = new HashMap<String, Integer>();
        ResultSetMetaData rSetMd = rSet.getMetaData();

        // iterate over all labels
        for (Map.Entry<String, String> labelEntry : labels.entrySet()) {
            for (int idx = 1; idx <= rSetMd.getColumnCount(); ++idx) {

                // the column name we're looking for?
                if (rSetMd.getColumnLabel(idx).equals(labelEntry.getKey())) {
                    columnMap.put(labelEntry.getKey(), idx);
                }

            }
        }

        return columnMap;
    }

    /**
     * Ugly hack to output a select box
     */
    protected String toSelectList(List<Report> reports) {
        if (reports == null) {
            return "";
        }

        StringBuffer strBuff = new StringBuffer();
        strBuff.append("<select name='reportName'>");
        for (Report report : reports) {
            strBuff.append(
                String.format(
                    "<option value='%s'>%s</option>",
                        report.getName(),
                        report.getTitle()
                )
            );
        }
        strBuff.append("</select>");
        return strBuff.toString();
    }

}
