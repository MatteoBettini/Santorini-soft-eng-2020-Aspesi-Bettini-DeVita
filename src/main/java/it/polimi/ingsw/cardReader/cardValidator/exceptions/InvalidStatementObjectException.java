package it.polimi.ingsw.cardReader.cardValidator.exceptions;

/**
 * This exception is thrown when a problem is found during the scan of the subject of a RuleStatement
 */
public class InvalidStatementObjectException extends Exception {
    public InvalidStatementObjectException(String message) {
        super(message);
    }
}
