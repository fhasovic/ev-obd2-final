package com.reversecoder.obd2.commands;

/**
 * Class for any obd command.
 */
public class AnyObdCommand extends ObdCommand {

    private String commandName;

    /**
     * Default ctor.
     */
    public AnyObdCommand(String name, String command) {
        super(command);
        commandName = name;
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link AnyObdCommand} object.
     */
    public AnyObdCommand(AnyObdCommand other) {
        super(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performCalculations() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFormattedResult() {
        return String.valueOf(getResult());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCalculatedResult() {
        return String.valueOf(getResult());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return commandName;
    }

}
