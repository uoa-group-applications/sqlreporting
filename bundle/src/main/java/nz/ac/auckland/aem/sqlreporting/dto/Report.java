package nz.ac.auckland.aem.sqlreporting.dto;

import java.util.List;
import java.util.Map;

/**
 * @author Marnix Cook
 *
 * Holds information about a report
 */
public class Report {

    private String name;
    private String title;
    private String query;
    private Map<String, String> labels;

    /**
     * Initialize data-members
     *
     * @param name is the identifying name of the node
     * @param title is the title stored inside the node
     * @param query is the query that is to be executed
     */
    public Report(String name, String title, String query, Map<String, String> labels) {
        this.name = name;
        this.title = title;
        this.query = query;
        this.labels = labels;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
