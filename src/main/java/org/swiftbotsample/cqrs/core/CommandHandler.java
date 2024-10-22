package org.swiftbotsample.cqrs.core;

import java.util.concurrent.ExecutionException;

/**
 * Abstract handler for a specific command type.
 * @param <T> the type of command this handler processes
 */
public abstract class CommandHandler<T extends Command> {
    public abstract void handle(T command) throws InterruptedException, ExecutionException;
}