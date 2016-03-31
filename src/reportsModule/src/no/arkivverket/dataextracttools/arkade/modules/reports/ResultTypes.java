/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.reports;

/**
 * @version 0.04 2014-02-28
 * @author Riksarkivet
 */
public interface ResultTypes {

    public static final int INFO = 0;
    public static final String INFO_STRING = "info";
    public static final int WARNING = 1;
    public static final String WARNING_STRING = "warning";
    public static final int ERROR = 2;
    public static final String ERROR_STRING = "error";

    public static final int UNDEFINED = 10;
    public static final String UNDEFINED_STRING = "Undefined";
}
