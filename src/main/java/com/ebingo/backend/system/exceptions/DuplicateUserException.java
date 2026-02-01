package com.ebingo.backend.system.exceptions;


/**
 * Exception thrown when a user already exists in the system.
 * This exception is typically used to indicate that a user creation
 * or registration attempt has failed because the user already exists.
 */
public class DuplicateUserException extends RuntimeException{

    /**
     * Default constructor for DuplicateUserException.
     * Initializes a new instance of the exception without a message or cause.
     */
    public DuplicateUserException() {
        super();
    }

    /**
     * Constructor for DuplicateUserException with a message.
     * Initializes a new instance of the exception with the specified message.
     *
     * @param message The message that describes the error.
     */
    public DuplicateUserException(String message) {
        super(message);
    }

    /**
     * Constructor for DuplicateUserException with a message and cause.
     * Initializes a new instance of the exception with the specified message and cause.
     *
     * @param message The message that describes the error.
     * @param cause The cause of the error.
     */
    public DuplicateUserException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for DuplicateUserException with a cause.
     * Initializes a new instance of the exception with the specified cause.
     *
     * @param cause The cause of the error.
     */
    public DuplicateUserException(Throwable cause) {
        super(cause);
    }
}

