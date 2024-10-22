package org.swiftbotsample.app.commands.handlers;

import org.swiftbotsample.app.commands.types.WhackAMoleCommand;
import org.swiftbotsample.app.stores.GameResultStore;
import org.swiftbotsample.cqrs.core.CommandHandler;
import swiftbot.Button;
import swiftbot.SwiftBotAPI;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WhackAMoleCommandHandler extends CommandHandler<WhackAMoleCommand> {

    private volatile boolean gameRunning = false;
    private volatile int score = 0;
    private volatile int combo = 0;
    private volatile int maxScore = 0;
    private final ConcurrentLinkedQueue<Button> buttonQueue = new ConcurrentLinkedQueue<>();
    private final Button[] buttons = new Button[]{Button.A, Button.B, Button.X, Button.Y};

    @Override
    public void handle(WhackAMoleCommand command) throws InterruptedException {
        System.out.println("Whack-A-Mole game started.");
        SwiftBotAPI api = command.api;

        //Step 1: Prompt the user to press A-X-B-Y to start
        if (!waitForStartSequence(api)) {
            System.out.println("Start sequence failed. Exiting game.");
            return;
        }

        //Step 2: Countdown
        countdown(api);

        //Step 3: Start the game
        gameRunning = true;
        score = 0;
        combo = 0;

        //Enable buttons and assign functions
        for (Button button : buttons) {
            final Button b = button; //Ensure correct reference in lambda
            api.enableButton(b, () -> buttonQueue.offer(b));
        }

        //Game loop
        playGame(api);

        //Disable buttons
        for (Button button : buttons) {
            api.disableButton(button);
        }

        //Output the result
        GameResultStore.setLastGameScore(score);
        GameResultStore.setLastGameMaxScore(maxScore);

        System.out.println("Game over. Final score: " + score);
    }

    private boolean waitForStartSequence(SwiftBotAPI api) throws InterruptedException {
        System.out.println("Press buttons in sequence: A-X-B-Y to start.");
        Button[] startSequence = new Button[]{Button.A, Button.X, Button.B, Button.Y};
        int index = 0;

        while (index < startSequence.length) {
            Button pressedButton = waitForButtonPress(api);
            if (pressedButton.equals(startSequence[index])) {
                index++;
                System.out.println("Button " + pressedButton + " pressed correctly.");
            } else {
                System.out.println("Incorrect button. Resetting sequence.");
                index = 0;
            }
        }

        return true;
    }

    private Button waitForButtonPress(SwiftBotAPI api) throws InterruptedException {
        final Object lock = new Object();
        final Button[] pressedButton = new Button[1];

        //Enable buttons with appropriate functions
        for (Button button : buttons) {
            final Button b = button;
            api.enableButton(b, () -> {
                synchronized (lock) {
                    pressedButton[0] = b;
                    lock.notify();
                }
            });
        }

        synchronized (lock) {
            lock.wait();
        }

        //Disable buttons to prevent multiple triggers
        for (Button button : buttons) {
            api.disableButton(button);
        }

        return pressedButton[0];
    }

    private void countdown(SwiftBotAPI api) throws InterruptedException {
        System.out.println("Game starting in...");
        for (int i = 3; i > 0; i--) {
            System.out.println(i + "...");
            Thread.sleep(1000);
        }
        System.out.println("Go!");
    }

    private void playGame(SwiftBotAPI api) throws InterruptedException {
        Random random = new Random();
        int gameDuration = 30000; //30 seconds
        long endTime = System.currentTimeMillis() + gameDuration;

        maxScore = 0;

        while (System.currentTimeMillis() < endTime) {
            //Randomly select a button to light up
            Button targetButton = buttons[random.nextInt(buttons.length)];
            api.setButtonLight(targetButton, true);
            api.setButtonLightBrightness(targetButton, 100);

            //Expected response time
            long responseStartTime = System.currentTimeMillis();
            long responseTimeLimit = 2000; //2 seconds
            boolean buttonPressed = false;

            while (System.currentTimeMillis() - responseStartTime < responseTimeLimit) {
                Button pressedButton = buttonQueue.poll();
                if (pressedButton != null) {
                    if (pressedButton.equals(targetButton)) {
                        //Correct button pressed
                        combo++;
                        int points = 10 * combo;
                        score += points;
                        System.out.println("Correct! Combo: " + combo + ", Points: " + points + ", Total Score: " + score);
                        buttonPressed = true;
                        break;
                    } else {
                        //Incorrect button pressed
                        System.out.println("Incorrect button pressed. Combo broken.");
                        combo = 0;
                        buttonPressed = true;
                        break;
                    }
                }
                Thread.sleep(50);
            }

            //Turn off the button light
            api.setButtonLight(targetButton, false);

            if (!buttonPressed) {
                System.out.println("No button pressed. Combo broken.");
                combo = 0;
            }

            //Calculate max score possible
            maxScore += 10 * (combo > 0 ? combo : 1);

            //Short delay before next mole
            Thread.sleep(500);
        }

        gameRunning = false;
    }
}