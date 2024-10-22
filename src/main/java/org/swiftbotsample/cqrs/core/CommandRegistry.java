package org.swiftbotsample.cqrs.core;

import org.swiftbotsample.app.ButtonName;
import org.swiftbotsample.cqrs.annotations.CommandAttribute;
import org.reflections.Reflections;
import swiftbot.Button;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CommandRegistry {
    private final Map<Class<? extends Command>, CommandHandler<? extends Command>> commandHandlerMap = new HashMap<>();
    private final Map<Set<Button>, Class<? extends Command>> buttonCommandMap = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(CommandRegistry.class.getName());
    private final Set<Class<? extends Command>> commands;

    public CommandRegistry(String packageName) {
        commands = loadCommands(packageName);
        loadCommandsAndHandlers(packageName);
    }

    private Set<Class<? extends Command>> loadCommands(String packageName) {
        Reflections reflections = new Reflections(packageName);
        return reflections.getTypesAnnotatedWith(CommandAttribute.class)
                .stream()
                .filter(Command.class::isAssignableFrom)
                .map(cls -> (Class<? extends Command>) cls)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private void loadCommandsAndHandlers(String packageName) {
        Reflections reflections = new Reflections(packageName);

        //Scan and register commands and their handlers
        Set<Class<?>> commandClasses = reflections.getTypesAnnotatedWith(CommandAttribute.class);
        Set<Class<? extends CommandHandler<?>>> handlerClasses = (Set<Class<? extends CommandHandler<?>>>) (Set<?>) reflections.getSubTypesOf(CommandHandler.class);

        for (Class<? extends CommandHandler<?>> handlerClass : handlerClasses) {
            try {
                CommandHandler<?> handlerInstance = handlerClass.getDeclaredConstructor().newInstance();
                Class<? extends Command> commandClass = getCommandType(handlerClass);

                if (commandClass != null && commandClasses.contains(commandClass)) {
                    commandHandlerMap.put(commandClass, handlerInstance);
                    LOGGER.log(Level.INFO, "Registered handler for command: {0}", commandClass.getSimpleName());

                    //Handle button mappings
                    CommandAttribute commandAttr = commandClass.getAnnotation(CommandAttribute.class);
                    if (commandAttr != null && commandAttr.buttons().length > 0) {
                        Set<Button> buttonSet = mapButtonNamesToButtons(commandAttr.buttons());

                        //Check for conflicts
                        if (buttonCommandMap.containsKey(buttonSet)) {
                            throw new IllegalArgumentException(
                                    "Button combination " + buttonSet + " is already registered to command " +
                                            buttonCommandMap.get(buttonSet).getSimpleName());
                        } else {
                            buttonCommandMap.put(buttonSet, commandClass);
                            LOGGER.log(Level.INFO, "Registered button combination {0} for command {1}",
                                    new Object[]{buttonSet, commandClass.getSimpleName()});
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to register handler: " + handlerClass.getName(), e);
            }
        }
    }

    private Set<Button> mapButtonNamesToButtons(ButtonName[] buttonNames) {
        return Arrays.stream(buttonNames)
                .map(this::getButtonFromName)
                .collect(Collectors.toSet());
    }

    private Button getButtonFromName(ButtonName buttonName) {
        switch (buttonName) {
            case A:
                return Button.A;
            case B:
                return Button.B;
            case X:
                return Button.X;
            case Y:
                return Button.Y;
            default:
                throw new IllegalArgumentException("Unknown ButtonName: " + buttonName);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Command> getCommandType(Class<? extends CommandHandler<?>> handlerClass) {
        try {
            return (Class<? extends Command>) ((java.lang.reflect.ParameterizedType)
                    handlerClass.getGenericSuperclass()).getActualTypeArguments()[0];
        } catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
            LOGGER.log(Level.WARNING, "Failed to determine command type for handler: " + handlerClass.getName(), e);
            return null;
        }
    }

    public Class<? extends Command> getCommandForButtons(Set<Button> buttons) {
        return buttonCommandMap.get(buttons);
    }

    @SuppressWarnings("unchecked")
    public <T extends Command> CommandHandler<T> getHandler(Class<T> commandClass) {
        return (CommandHandler<T>) commandHandlerMap.get(commandClass);
    }

    public Map<Set<Button>, Class<? extends Command>> getButtonCommandMap() {
        return buttonCommandMap;
    }

    public Set<Class<? extends Command>> getCommands() {
        return commands;
    }
}