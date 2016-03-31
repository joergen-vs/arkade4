/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.processes;

import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Activity;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;


/**
 *
 * @version 0.09 2014-02-28
 * @author Riksarkivet
 */
public interface DataExtractProcess {
    
    public String getId();
    
    public String getName();

    public String getLongName();
    
    public String getDescription();
    
    public void setDescription(String description);

    public String getResultDescription();
    
    public void setResultDescription(String resultDescription);
    
    public DatasetDescription getDatasetDescription();

    public void init();
    
    public void setMaxNumberOfResults(int maxNumberOfResults);

    /**
     * Updates the activity with the final results
     */
    public void finish();
    
    public boolean isFinished();
    
    public Activity getActivity();
}
