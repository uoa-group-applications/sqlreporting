package nz.ac.auckland.aem.sqlreporting.service;

import nz.ac.auckland.aem.sqlreporting.dto.Report;
import nz.ac.auckland.aem.sqlreporting.dto.ReportFactory;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Marnix Cook
 *
 * Implementation that is able to retrieve reports from the JCR
 */
@Service
@Component(immediate = true)
public class ReportingImpl implements Reporting {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReportingImpl.class);

    /**
     * Resource resolver
     */
    private ResourceResolver resourceResolver;

    /**
     * Resource resolver factory
     */
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * Setup the resource resolver on bundle activation
     */
    @Activate
    public void activateService() {
        this.resourceResolver = getResourceResolver();
    }

    /**
     * Make sure the release the resource resolver when the bundle is deactivated
     */
    @Deactivate
    public void deactivateService() {
        if (this.resourceResolver != null) {
            this.resourceResolver.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Report> getAllReports() throws RepositoryException {
        Resource folderResource = this.resourceResolver.getResource(REPORT_PATH);
        if (folderResource == null) {
            LOG.error("You need to create the `{}` path and store your queries there", REPORT_PATH);
            return null;
        }

        List<Report> reports = new ArrayList<Report>();

        Iterator<Resource> childIterator = folderResource.getChildren().iterator();

        while (childIterator.hasNext()) {
            Resource child = childIterator.next();
            Node jcrChild = child.adaptTo(Node.class);
            Report report = ReportFactory.create(jcrChild);

            if (report == null) {
                LOG.error("Could not convert the node at `{}` to a Report instance", child.getPath());
                continue;
            }

            reports.add(report);
        }

        return reports;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Report getReportByName(String reportName) {
        Resource folderResource = this.resourceResolver.getResource(REPORT_PATH);
        if (folderResource == null) {
            LOG.error("You need to create the `{}` path and store your queries there", REPORT_PATH);
            return null;
        }

        Resource reportResource = folderResource.getChild(reportName);
        if (reportResource == null) {
            LOG.error("No child with name `{}` found!", reportName);
            return null;
        }

        Node reportNode = reportResource.adaptTo(Node.class);
        return ReportFactory.create(reportNode);
    }

    /**
     * @return a resource resolver.
     */
    protected ResourceResolver getResourceResolver() {
        try {
            return this.resourceResolverFactory.getAdministrativeResourceResolver(null);
        }
        catch (LoginException lEx) {
            LOG.error("Could not login and retrieve a resource resolver, caused by:", lEx);
        }
        return null;
    }

}
