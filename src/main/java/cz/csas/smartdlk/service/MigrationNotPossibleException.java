package cz.csas.smartdlk.service;

public class MigrationNotPossibleException extends RuntimeException {

    public MigrationNotPossibleException(String message) {
        super(message);
    }
}
