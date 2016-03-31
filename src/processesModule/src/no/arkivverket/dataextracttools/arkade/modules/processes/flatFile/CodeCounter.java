/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import no.arkivverket.dataextracttools.arkade.modules.reports.ResultTypes;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItem;

/**
 * Counts occurrences of a code value and returns the result as a result item
 * part of an activity in a session report.
 *
 * @version 0.06 2014-03-21
 * @author Riksarkivet
 */
public class CodeCounter {

    private String codeValue = "";
    private final String explanation;
    private long occurrences = 0;
    private final boolean undefined;
    private ResultItem resultItem;

    /**
     *
     * @param codeValue
     * @param explanation
     * @param undefined
     */
    public CodeCounter(String codeValue, String explanation, boolean undefined) {
        this.codeValue = codeValue;
        this.explanation = explanation;
        this.undefined = undefined;
    }

    public String getCodeValue() {
        return codeValue;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isUndefined() {
        return undefined;
    }

    public void addOccurrence() {
        occurrences++;
    }

    public long getOccurrences() {
        return occurrences;
    }

    /**
     *
     * @return
     */
    public ResultItem getResult() {
        String resultType = null;

        if (!undefined && occurrences > 0) {
            // Defined and occurs
            resultType = ResultTypes.INFO_STRING;
        } else if (!undefined && occurrences == 0) {
            // Defined, but does not occur
            resultType = ResultTypes.WARNING_STRING;
        } else if (undefined) {
            // Not defined
            if (codeValue.equals("")) {
                // Empty code value
                resultType = ResultTypes.WARNING_STRING;
            } else {
                resultType = ResultTypes.ERROR_STRING;
            }
        }

        resultItem = new ResultItem();
        resultItem.setName(codeValue);
        resultItem.setType(resultType);
        if (explanation != null) {
            resultItem.setLabel(explanation);
        }
        resultItem.setContent("" + occurrences);
        return resultItem;
    }

    @Override
    public String toString() {
        if (resultItem != null) {
            return resultItem.toString();
        }
        return "";
    }
}
