package org.entando.entando.plugins.jpsolr.web.content.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.entando.entando.web.common.model.PagedMetadata;

@JsonPropertyOrder({"page", "pageSize", "lastPage", "totalItems",
        "csvCategories", "includeAttachments", "filters", "doubleFilters"})
public class SolrContentPagedMetadata<T> extends PagedMetadata<T> {

    @JsonInclude(Include.NON_NULL)
    private String lang;
    private String[] csvCategories = new String[0];
    @JsonInclude(Include.NON_NULL)
    private String text;
    @JsonInclude(Include.NON_NULL)
    private String searchOption;
    @JsonInclude(Include.NON_NULL)
    private boolean includeAttachments;
    private SolrFilter[][] doubleFilters = new SolrFilter[0][0];

    public SolrContentPagedMetadata() {
        super();
    }

    public SolrContentPagedMetadata(AdvRestContentListRequest req, Integer totalItems) {
        super(req, totalItems);
        if (null != req.getDoubleFilters()) {
            this.setDoubleFilters(req.getDoubleFilters());
        }
        if (null != req.getCsvCategories()) {
            this.setCsvCategories(req.getCsvCategories());
        }
        this.setLang(req.getLang());
        this.setText(req.getText());
        this.setSearchOption(req.getSearchOption());
        this.setIncludeAttachments(req.isIncludeAttachments());
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String[] getCsvCategories() {
        return csvCategories;
    }

    public void setCsvCategories(String[] csvCategories) {
        this.csvCategories = csvCategories;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSearchOption() {
        return searchOption;
    }

    public void setSearchOption(String searchOption) {
        this.searchOption = searchOption;
    }

    public boolean isIncludeAttachments() {
        return includeAttachments;
    }

    public void setIncludeAttachments(boolean includeAttachments) {
        this.includeAttachments = includeAttachments;
    }

    public SolrFilter[][] getDoubleFilters() {
        return doubleFilters;
    }

    public void setDoubleFilters(SolrFilter[][] doubleFilters) {
        this.doubleFilters = doubleFilters;
    }
}
