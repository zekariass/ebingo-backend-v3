package com.ebingo.backend.system.exceptions;

public class UserCreationException extends RuntimeException{

    /**
     * Default constructor for UserCreationException.
     * Initializes a new instance of the exception without a message or cause.
     */
    public UserCreationException() {
        super();
    }


    /**
     * Constructor for UserCreationException with a message.
     * Initializes a new instance of the exception with the specified message.
     *
     * @param message The message that describes the error.
     */
    public UserCreationException(String message) {
        super(message);
    }


    /**
     * Constructor for UserCreationException with a message and cause.
     * Initializes a new instance of the exception with the specified message and cause.
     *
     * @param message The message that describes the error.
     * @param cause The cause of the error.
     */
    public UserCreationException(String message, Throwable cause) {
        super(message, cause);
    }


    /**
     * Constructor for UserCreationException with a cause.
     * Initializes a new instance of the exception with the specified cause.
     *
     * @param cause The cause of the error.
     */
    public UserCreationException(Throwable cause) {
        super(cause);
    }
}

