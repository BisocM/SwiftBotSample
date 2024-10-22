package org.swiftbotsample.app.commands.handlers;

import org.swiftbotsample.app.commands.types.NavigateObstaclesCommand;
import org.swiftbotsample.cqrs.core.CommandHandler;
import swiftbot.SwiftBotAPI;
import swiftbot.Underlight;
import swiftbot.ImageSize;

import java.awt.image.BufferedImage;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

// Define states for the robot
enum NavigationState {
    MOVING_FORWARD,
    AVOIDING_OBSTACLE,
    TURNING,
    BACKTRACKING,
    STUCK,
    STOPPED
}

public class NavigateObstaclesCommandHandler extends CommandHandler<NavigateObstaclesCommand> {

    //Define constants for sensor thresholds and timeouts
    private static final double OBSTACLE_DISTANCE_THRESHOLD = 15.0; //cm
    private static final double SAFE_DISTANCE = 25.0; //cm
    private static final int TURN_DURATION = 300; //ms
    private static final int BACKUP_DURATION = 400; //ms
    private static final int MAX_BACKTRACK_ATTEMPTS = 2;
    private static final int MAX_STUCK_ATTEMPTS = 3;
    private static final int MAX_TURN_ANGLE = 90; //degrees
    private static final int MOVE_SPEED = 80;
    private static final int IMAGE_PROCESSING_INTERVAL = 500; //ms
    private static final int STUCK_CHECK_INTERVAL = 1000; //ms

    //Shared state variables
    private volatile NavigationState state = NavigationState.MOVING_FORWARD;
    private final AtomicBoolean keepNavigating = new AtomicBoolean(true);
    private final AtomicBoolean obstacleDetected = new AtomicBoolean(false);
    private final AtomicBoolean stuckDetected = new AtomicBoolean(false);
    private int backtrackAttempts = 0;
    private int stuckAttempts = 0;
    private double obstacleDirection = 0.0; //-1.0 for left, 1.0 for right, 0.0 for straight ahead

    private static final Logger logger = Logger.getLogger(NavigateObstaclesCommandHandler.class.getName());

    @Override
    public void handle(NavigateObstaclesCommand command) throws InterruptedException, ExecutionException {
        logger.info("Navigate obstacles command received.");
        SwiftBotAPI api = command.api;

        //Executor service for parallel tasks
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        //Start sensor monitoring tasks
        executorService.submit(() -> monitorUltrasonicSensor(api));
        executorService.submit(() -> monitorCamera(api));
        executorService.submit(() -> monitorMovement(api));

        //Start the main navigation loop in a separate thread
        Future<?> navigationTask = executorService.submit(() -> {
            try {
                navigate(api);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Navigation error: ", e);
            }
        });

        //Wait for navigation to complete
        navigationTask.get();

        //Shutdown executor service
        executorService.shutdownNow();

        logger.info("Obstacle navigation completed.");
    }

    /**
     * Main navigation logic.
     */
    private void navigate(SwiftBotAPI api) throws InterruptedException {
        while (keepNavigating.get()) {
            switch (state) {
                case MOVING_FORWARD:
                    handleMovingForward(api);
                    break;

                case AVOIDING_OBSTACLE:
                    handleAvoidingObstacle(api);
                    break;

                case TURNING:
                    handleTurning(api);
                    break;

                case BACKTRACKING:
                    handleBacktracking(api);
                    break;

                case STUCK:
                    handleStuck(api);
                    break;

                case STOPPED:
                    handleStopped(api);
                    break;
            }
            //No unnecessary delay here to improve responsiveness
        }
    }

    private void handleMovingForward(SwiftBotAPI api) {
        logger.info("State: MOVING_FORWARD");
        api.startMove(MOVE_SPEED, MOVE_SPEED);
        setUnderlightsColor(api, new int[]{0, 255, 0}); //Green color

        if (obstacleDetected.get()) {
            //Obstacle detected, switch to avoiding obstacle
            api.stopMove();
            setUnderlightsColor(api, new int[]{255, 0, 0}); //Red color
            state = NavigationState.AVOIDING_OBSTACLE;
        } else if (stuckDetected.get()) {
            //Stuck detected, switch to stuck handling
            api.stopMove();
            setUnderlightsColor(api, new int[]{255, 165, 0}); //Orange color
            state = NavigationState.STUCK;
        }
        sleepWithoutInterrupt(20); //Short sleep to prevent tight loop
    }

    private void handleAvoidingObstacle(SwiftBotAPI api) {
        logger.info("State: AVOIDING_OBSTACLE");

        //Decide which way to turn based on obstacle direction
        if (obstacleDirection < 0) {
            //Obstacle detected on the left, so turn right
            state = NavigationState.TURNING;
            obstacleDirection = 1.0;
        } else if (obstacleDirection > 0) {
            //Obstacle detected on the right, so turn left
            state = NavigationState.TURNING;
            obstacleDirection = -1.0;
        } else {
            //Obstacle straight ahead
            state = NavigationState.BACKTRACKING;
        }
        obstacleDetected.set(false);
        disableUnderlights(api);
    }

    private void handleTurning(SwiftBotAPI api) throws InterruptedException {
        logger.info("State: TURNING");
        setUnderlightsColor(api, new int[]{0, 0, 255}); //Blue color

        //Calculate turn parameters
        int turnSpeed = MOVE_SPEED / 2;
        int angle = (int) (MAX_TURN_ANGLE * obstacleDirection);

        //Perform turn
        logger.info("Turning with angle: " + angle + " degrees");
        turnByAngle(api, angle, turnSpeed);

        //Reset attempts after a successful turn
        backtrackAttempts = 0;
        stuckAttempts = 0;
        obstacleDirection = 0.0;

        state = NavigationState.MOVING_FORWARD;
    }

    private void handleBacktracking(SwiftBotAPI api) throws InterruptedException {
        logger.info("State: BACKTRACKING");
        setUnderlightsColor(api, new int[]{255, 255, 0}); //Yellow color

        if (backtrackAttempts >= MAX_BACKTRACK_ATTEMPTS) {
            //Maximum backtrack attempts reached
            logger.warning("No clear path found. Stopping navigation.");
            state = NavigationState.STOPPED;
            return;
        }

        logger.info("Backtracking... Attempt " + (backtrackAttempts + 1));
        backtrackAttempts++;

        //Back up
        api.startMove(-MOVE_SPEED / 2, -MOVE_SPEED / 2);
        sleepWithoutInterrupt(BACKUP_DURATION);
        api.stopMove();

        //Try turning in an alternate direction
        obstacleDirection = (backtrackAttempts % 2 == 0) ? -1.0 : 1.0; //Alternate between left and right
        state = NavigationState.TURNING;
    }

    private void handleStuck(SwiftBotAPI api) throws InterruptedException {
        logger.info("State: STUCK");
        setUnderlightsColor(api, new int[]{255, 165, 0}); //Orange color

        if (stuckAttempts >= MAX_STUCK_ATTEMPTS) {
            //Maximum stuck attempts reached
            logger.warning("Unable to get unstuck. Stopping navigation.");
            state = NavigationState.STOPPED;
            return;
        }

        logger.info("Attempting to get unstuck... Attempt " + (stuckAttempts + 1));
        stuckAttempts++;

        //Back up slightly
        api.startMove(-MOVE_SPEED / 2, -MOVE_SPEED / 2);
        sleepWithoutInterrupt(BACKUP_DURATION / 2);
        api.stopMove();

        //Turn slightly
        obstacleDirection = (stuckAttempts % 2 == 0) ? -0.5 : 0.5; //Alternate directions
        int angle = (int) (MAX_TURN_ANGLE * obstacleDirection / 2); //Smaller angle
        turnByAngle(api, angle, MOVE_SPEED / 2);

        obstacleDirection = 0.0;
        stuckDetected.set(false);

        state = NavigationState.MOVING_FORWARD;
    }

    private void handleStopped(SwiftBotAPI api) {
        logger.info("State: STOPPED");
        api.stopMove();
        disableUnderlights(api);
        keepNavigating.set(false);
    }

    /**
     * Monitors the ultrasonic sensor in a separate thread.
     */
    private void monitorUltrasonicSensor(SwiftBotAPI api) {
        while (keepNavigating.get()) {
            double distance = api.useUltrasound();
            logger.fine("Ultrasonic sensor distance: " + distance + " cm");
            if (distance < OBSTACLE_DISTANCE_THRESHOLD) {
                logger.info("Ultrasonic obstacle detected at " + distance + " cm");
                obstacleDetected.set(true);
                obstacleDirection = 0.0;
            }
            sleepWithoutInterrupt(30); //Adjust sampling rate as necessary
        }
    }

    /**
     * Monitors the camera for visual obstacles in a separate thread.
     */
    private void monitorCamera(SwiftBotAPI api) {
        long lastProcessingTime = 0;
        while (keepNavigating.get()) {
            if (System.currentTimeMillis() - lastProcessingTime > IMAGE_PROCESSING_INTERVAL) {
                lastProcessingTime = System.currentTimeMillis();
                ObstacleDetectionResult result = detectObstacleWithCamera(api);
                if (result.obstacleDetected) {
                    logger.info("Visual obstacle detected via camera at direction: " + result.direction);
                    obstacleDetected.set(true);
                    obstacleDirection = result.direction;
                }
            }
            sleepWithoutInterrupt(50); //Adjust as necessary
        }
    }

    /**
     * Monitors the robot's movement to detect if it's stuck.
     */
    private void monitorMovement(SwiftBotAPI api) {
        BufferedImage previousImage = null;
        long lastCheckTime = 0;
        while (keepNavigating.get()) {
            if (state == NavigationState.MOVING_FORWARD) {
                if (System.currentTimeMillis() - lastCheckTime > STUCK_CHECK_INTERVAL) {
                    lastCheckTime = System.currentTimeMillis();
                    BufferedImage currentImage = api.takeGrayscaleStill(ImageSize.SQUARE_480x480);
                    if (currentImage != null && previousImage != null) {
                        double difference = calculateImageDifference(previousImage, currentImage);
                        logger.fine("Image difference for stuck detection: " + difference);
                        if (difference < 5.0) { //Threshold for considering the robot is stuck
                            logger.warning("Robot might be stuck. Low image difference detected.");
                            stuckDetected.set(true);
                        } else {
                            stuckDetected.set(false);
                        }
                    }
                    previousImage = currentImage;
                }
            } else {
                previousImage = null; //Reset the previous image when not moving forward
                stuckDetected.set(false);
            }
            sleepWithoutInterrupt(100);
        }
    }

    /**
     * Detects obstacles using the camera and returns the result.
     */
    private ObstacleDetectionResult detectObstacleWithCamera(SwiftBotAPI api) {
        BufferedImage image = api.takeGrayscaleStill(ImageSize.SQUARE_480x480);
        if (image == null) {
            logger.warning("Failed to capture image for processing.");
            return new ObstacleDetectionResult(false, 0.0);
        }
        //Improved image processing to identify obstacle direction
        return processImageForObstacleDirection(image);
    }

    /**
     * Process the image to detect obstacles and determine their direction.
     */
    private ObstacleDetectionResult processImageForObstacleDirection(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        //Divide the image into left, center, and right regions
        int regionWidth = width / 3;
        int[] edgeCount = new int[3]; //0: left, 1: center, 2: right

        int threshold = 50; //Edge detection threshold

        //Process each pixel to detect edges
        for (int y = height / 2; y < height - 1; y++) { //Lower half of the image
            for (int x = 1; x < width - 1; x++) {
                int currentPixel = getGrayValue(image.getRGB(x, y));
                int rightPixel = getGrayValue(image.getRGB(x + 1, y));
                int bottomPixel = getGrayValue(image.getRGB(x, y + 1));

                int diffX = Math.abs(currentPixel - rightPixel);
                int diffY = Math.abs(currentPixel - bottomPixel);

                int magnitude = diffX + diffY;

                if (magnitude > threshold) {
                    int regionIndex = x / regionWidth;
                    regionIndex = Math.min(regionIndex, 2); //Ensure index is within bounds
                    edgeCount[regionIndex]++;
                }
            }
        }

        //Determine if obstacle is present based on edge counts
        int totalEdgeCount = edgeCount[0] + edgeCount[1] + edgeCount[2];
        double edgeDensity = (double) totalEdgeCount / ((height / 2) * width);
        logger.fine("Edge densities - Left: " + edgeCount[0] + ", Center: " + edgeCount[1] + ", Right: " + edgeCount[2]);

        if (edgeDensity > 0.05) { //Adjust overall threshold as needed
            //Determine obstacle direction
            if (edgeCount[0] > edgeCount[2]) {
                //More edges on the left side
                return new ObstacleDetectionResult(true, -1.0); //Obstacle on the left
            } else if (edgeCount[2] > edgeCount[0]) {
                //More edges on the right side
                return new ObstacleDetectionResult(true, 1.0); //Obstacle on the right
            } else {
                //Obstacle straight ahead
                return new ObstacleDetectionResult(true, 0.0);
            }
        }
        return new ObstacleDetectionResult(false, 0.0);
    }

    /**
     * Calculates the difference between two images.
     */
    private double calculateImageDifference(BufferedImage img1, BufferedImage img2) {
        int width = Math.min(img1.getWidth(), img2.getWidth());
        int height = Math.min(img1.getHeight(), img2.getHeight());

        double totalDifference = 0.0;
        int numPixels = 0;

        //Downsample the image for faster computation
        int stepSize = 10;

        for (int y = 0; y < height; y += stepSize) {
            for (int x = 0; x < width; x += stepSize) {
                int gray1 = getGrayValue(img1.getRGB(x, y));
                int gray2 = getGrayValue(img2.getRGB(x, y));
                totalDifference += Math.abs(gray1 - gray2);
                numPixels++;
            }
        }

        return totalDifference / numPixels;
    }

    /**
     * Helper method to extract the grayscale value from an RGB integer.
     */
    private int getGrayValue(int rgb) {
        return (rgb >> 16) & 0xFF; //Since the image is grayscale, we can use red channel
    }

    /**
     * Helper method to perform a turn by a specific angle.
     */
    private void turnByAngle(SwiftBotAPI api, int angle, int speed) throws InterruptedException {
        //Approximate the time needed to turn the specified angle
        //This will depend on the robot's turning rate, which may require calibration
        int turnTime = Math.abs(angle) * TURN_DURATION / MAX_TURN_ANGLE;

        if (angle < 0) {
            //Turn left
            api.startMove(-speed, speed);
        } else if (angle > 0) {
            //Turn right
            api.startMove(speed, -speed);
        }
        sleepWithoutInterrupt(turnTime);
        api.stopMove();
    }

    //Helper method to set underlights color
    private void setUnderlightsColor(SwiftBotAPI api, int[] rgb) {
        try {
            api.fillUnderlights(rgb);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error setting underlights color: ", e);
        }
    }

    //Helper method to disable underlights
    private void disableUnderlights(SwiftBotAPI api) {
        try {
            api.disableUnderlights();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error disabling underlights: ", e);
        }
    }

    //Utility method to sleep without throwing InterruptedException
    private void sleepWithoutInterrupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //Restore the interrupted status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Class to hold obstacle detection results.
     */
    private static class ObstacleDetectionResult {
        boolean obstacleDetected;
        double direction; //-1.0 for left, 1.0 for right, 0.0 for straight ahead

        public ObstacleDetectionResult(boolean obstacleDetected, double direction) {
            this.obstacleDetected = obstacleDetected;
            this.direction = direction;
        }
    }
}