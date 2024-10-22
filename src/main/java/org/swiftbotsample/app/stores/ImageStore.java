package org.swiftbotsample.app.stores;

import java.awt.image.BufferedImage;

public class ImageStore {
    private static BufferedImage lastCapturedImage;

    public static synchronized void setLastCapturedImage(BufferedImage image) {
        lastCapturedImage = image;
    }

    public static synchronized BufferedImage getLastCapturedImage() {
        return lastCapturedImage;
    }
}