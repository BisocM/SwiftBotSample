package org.swiftbotsample.app.commands.handlers;

import org.swiftbotsample.app.stores.ImageStore;
import org.swiftbotsample.app.commands.types.CaptureImageCommand;
import org.swiftbotsample.cqrs.core.CommandHandler;
import swiftbot.ImageSize;
import swiftbot.SwiftBotAPI;

import java.awt.image.BufferedImage;

public class CaptureImageCommandHandler extends CommandHandler<CaptureImageCommand> {

    @Override
    public void handle(CaptureImageCommand command) {
        System.out.println("Capture Image command received.");
        SwiftBotAPI api = command.api;

        try {
            BufferedImage image = api.takeStill(ImageSize.SQUARE_1080x1080);

            if (image != null) {
                System.out.println("Image captured successfully.");

                //Store the image in ImageStore
                ImageStore.setLastCapturedImage(image);
            } else {
                System.out.println("Failed to capture image.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}