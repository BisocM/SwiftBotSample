package org.swiftbotsample.app;

import swiftbot.Button;
import swiftbot.SwiftBotAPI;
import org.swiftbotsample.cqrs.core.CommandRegistry;
import org.swiftbotsample.cqrs.core.MenuManager;
import org.swiftbotsample.cqrs.core.Command;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ButtonListener {
    private final SwiftBotAPI swiftBot;
    private final CommandRegistry commandRegistry;
    private final MenuManager menuManager;
    private final Map<Button, Boolean> buttonStates = new ConcurrentHashMap<>();
    private final Map<Button, Long> buttonPressTimes = new ConcurrentHashMap<>();
    private static final long COMBINATION_TIME_WINDOW = 500; //milliseconds

    public ButtonListener(SwiftBotAPI swiftBot, CommandRegistry commandRegistry, MenuManager menuManager) {
        this.swiftBot = swiftBot;
        this.commandRegistry = commandRegistry;
        this.menuManager = menuManager;

        //Define the buttons manually
        Button[] buttons = new Button[]{
                Button.A,
                Button.B,
                Button.X,
                Button.Y
        };

        //Initialize button states and enable buttons
        for (Button button : buttons) {
            buttonStates.put(button, false);
            buttonPressTimes.put(button, 0L);
            swiftBot.enableButton(button, () -> onButtonPressed(button));
        }
    }

    public synchronized void onButtonPressed(Button button) {
        buttonStates.put(button, true);
        buttonPressTimes.put(button, System.currentTimeMillis());

        //Slight delay to check for simultaneous button presses
        try {
            Thread.sleep(50); //Adjust as needed for your hardware
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Set<Button> pressedButtons = getPressedButtonsWithinTimeWindow();
        Class<? extends Command> commandClass = commandRegistry.getCommandForButtons(pressedButtons);

        if (commandClass != null) {
            //Execute the command
            try {
                Command command = commandClass.getDeclaredConstructor(SwiftBotAPI.class).newInstance(swiftBot);
                menuManager.executeCommand(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Reset button state after handling
        buttonStates.put(button, false);
    }

    private Set<Button> getPressedButtonsWithinTimeWindow() {
        long currentTime = System.currentTimeMillis();
        Set<Button> pressedButtons = new HashSet<>();
        for (Map.Entry<Button, Long> entry : buttonPressTimes.entrySet()) {
            long pressTime = entry.getValue();
            if ((currentTime - pressTime) <= COMBINATION_TIME_WINDOW) {
                pressedButtons.add(entry.getKey());
            }
        }
        return pressedButtons;
    }

    public void simulateButtonPress(Button button) {
        onButtonPressed(button);
    }
}