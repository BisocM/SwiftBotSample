package org.swiftbotsample.app.commands.handlers;

import org.swiftbotsample.app.commands.types.LightShowCommand;
import org.swiftbotsample.cqrs.core.CommandHandler;
import swiftbot.Button;
import swiftbot.SwiftBotAPI;
import swiftbot.Underlight;

public class LightShowCommandHandler extends CommandHandler<LightShowCommand> {

    @Override
    public void handle(LightShowCommand command) throws InterruptedException {
        System.out.println("Light Show command received.");
        SwiftBotAPI api = command.api;

        //Define the buttons array
        Button[] buttons = new Button[]{
                Button.A,
                Button.B,
                Button.X,
                Button.Y
        };

        //Start the light show
        chaseEffect(api);
        rainbowCycle(api);
        waveEffect(api);

        //Reset the lights at the end
        api.disableUnderlights();
        for (Button button : buttons) {
            api.setButtonLight(button, false);
        }

        System.out.println("Light show completed.");
    }

    //Chase effect: Lights up the underlights one after another
    private void chaseEffect(SwiftBotAPI api) throws InterruptedException {
        System.out.println("Starting chase effect...");
        Underlight[] underlights = getUnderlightsArray();
        int[] color = {255, 255, 0}; //Yellow color

        //Repeat 10 times.
        for (int i = 0; i < 10; i++) {
            for (Underlight light : underlights) {
                api.disableUnderlights();
                api.setUnderlight(light, color);
                Thread.sleep(20);
            }
        }

        api.disableUnderlights();
    }

    //Rainbow cycle: Cycles through colors across the underlights
    private void rainbowCycle(SwiftBotAPI api) throws InterruptedException {
        System.out.println("Starting rainbow cycle...");
        Underlight[] underlights = getUnderlightsArray();
        int steps = 100; //Number of steps in the color transition

        for (int i = 0; i < steps; i++) {
            for (int j = 0; j < underlights.length; j++) {
                float hue = (float) ((i + j * (steps / underlights.length)) % steps) / steps;
                int[] rgb = hsvToRgb(hue, 1.0f, 1.0f);
                api.setUnderlight(underlights[j], rgb);
            }
            Thread.sleep(20);
        }

        api.disableUnderlights();
    }

    //Wave effect: Creates a wave-like effect by fading lights in and out
    private void waveEffect(SwiftBotAPI api) throws InterruptedException {
        System.out.println("Starting wave effect...");
        Underlight[] underlights = getUnderlightsArray();
        int maxBrightness = 255;

        //Repeat 20 times.
        for (int i = 0; i < 20; i++) {
            for (int phase = 0; phase < underlights.length * 2; phase++) {
                for (int j = 0; j < underlights.length; j++) {
                    int distance = Math.abs(phase - j);
                    int brightness = maxBrightness - (distance * 60);
                    brightness = Math.max(0, Math.min(maxBrightness, brightness));
                    int[] color = {brightness, brightness, 255}; //Blue wave
                    api.setUnderlight(underlights[j], color);
                }
                Thread.sleep(20);
            }
        }

        api.disableUnderlights();
    }

    //Helper method to get the underlights array
    private Underlight[] getUnderlightsArray() {
        return new Underlight[]{
                Underlight.FRONT_LEFT,
                Underlight.MIDDLE_LEFT,
                Underlight.BACK_LEFT,
                Underlight.BACK_RIGHT,
                Underlight.MIDDLE_RIGHT,
                Underlight.FRONT_RIGHT
        };
    }

    //Helper method to convert HSV to RGB
    private int[] hsvToRgb(float h, float s, float v) {
        int r, g, b;

        int i = (int) Math.floor(h * 6);
        float f = h * 6 - i;
        int p = Math.round(v * (1 - s) * 255);
        int q = Math.round(v * (1 - f * s) * 255);
        int t = Math.round(v * (1 - (1 - f) * s) * 255);
        int vi = Math.round(v * 255);

        switch (i % 6) {
            case 0:
                r = vi;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = vi;
                b = p;
                break;
            case 2:
                r = p;
                g = vi;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = vi;
                break;
            case 4:
                r = t;
                g = p;
                b = vi;
                break;
            case 5:
                r = vi;
                g = p;
                b = q;
                break;
            default:
                r = g = b = 0;
                break;
        }
        return new int[]{r, g, b};
    }
}