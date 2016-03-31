/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.xml;

import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItem;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItems;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 *
 *
 * @version 0.08 2014-02-28
 * @author Riksarkivet
 *
 */
public class ReportingErrorHandler implements ErrorHandler {

    private ResultItem exceptionsResultItem;
    private long numberOfWarnings = 0;
    private long numberOfErrors = 0;
    private int numberOfResults = 0;
    private int maxNumberOfResults = Integer.MAX_VALUE;

    public ReportingErrorHandler(int maxNumberOfResults) {
        this.maxNumberOfResults = maxNumberOfResults;
        exceptionsResultItem = new ResultItem();
        exceptionsResultItem.setName("listeOverValideringsavvik");
        exceptionsResultItem.setResultItems(new ResultItems());
    }

    @Override
    public void warning(SAXParseException ex) {
        numberOfResults++;
        report(ex, "warning");
        numberOfWarnings++;
    }

    @Override
    public void error(SAXParseException ex) {
        numberOfResults++;
        report(ex, "error");
        numberOfErrors++;
    }

    @Override
    public void fatalError(SAXParseException ex) throws SAXException {
        throw ex;
    }

    private void report(SAXParseException ex, String type) {
        // new ResultItem under resultItems
        if (numberOfResults <= maxNumberOfResults) {

            ResultItem resultItem = new ResultItem();
            resultItem.setName("valideringsavvik");
            resultItem.setType(type);
            resultItem.setResultItems(new ResultItems());

            ResultItem messageResultItem = new ResultItem();
            messageResultItem.setLabel("message");
            messageResultItem.setContent(ex.getMessage());
            resultItem.getResultItems().getResultItem().add(messageResultItem);

            int columnNumber = ex.getColumnNumber();
            int lineNumber = ex.getLineNumber();

            if (lineNumber > -1) {
                ResultItem lineNumberResultItem = new ResultItem();
                lineNumberResultItem.setLabel("lineNumber");
                lineNumberResultItem.setContent("" + lineNumber);
                resultItem.getResultItems().getResultItem().add(lineNumberResultItem);
            }
            if (columnNumber > -1) {
                ResultItem columnNumberResultItem = new ResultItem();
                columnNumberResultItem.setLabel("columnNumber");
                columnNumberResultItem.setContent("" + columnNumber);
                resultItem.getResultItems().getResultItem().add(columnNumberResultItem);
            }

            exceptionsResultItem.getResultItems().getResultItem().add(resultItem);

        }
    }

    public ResultItem getExceptionsResultItem() {
        return exceptionsResultItem;
    }

    public long getNumberOfErrors() {
        return numberOfErrors;
    }

    public long getNumberOfWarnings() {
        return numberOfWarnings;
    }

    public long getNumberOfResults() {
        return numberOfResults;
    }
}
