package org.swiftbotsample.app.web;

import fi.iki.elonen.NanoHTTPD;
import org.swiftbotsample.app.ButtonListener;
import org.swiftbotsample.app.stores.GameResultStore;
import org.swiftbotsample.app.stores.ImageStore;
import org.swiftbotsample.cqrs.core.Command;
import org.swiftbotsample.cqrs.core.CommandRegistry;
import swiftbot.Button;
import swiftbot.SwiftBotAPI;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class BotWebServer extends NanoHTTPD {

    private final SwiftBotAPI api;
    private final ButtonListener buttonListener;
    private final CommandRegistry commandRegistry;

    public BotWebServer(int port, SwiftBotAPI api, ButtonListener buttonListener, CommandRegistry commandRegistry) {
        super(port);
        this.api = api;
        this.buttonListener = buttonListener;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        if (session.getMethod() == Method.POST) {
            //Parse POST data
            try {
                session.parseBody(new HashMap<>());
                params.putAll(session.getParms());
            } catch (Exception e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
            }
        }

        switch (uri) {
            case "/":
                return serveHomePage();
            case "/image":
                return serveImage();
            case "/captureImage":
                return handleCaptureImage();
            case "/gameScore":
                return serveGameScore();
            case "/simulateButtonPress":
                return handleSimulateButtonPress(params);
            default:
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
        }
    }

    private Response serveHomePage() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h1>SwiftBot Web Interface</h1>");

        //Image Viewer Section
        html.append("<h2>Image Viewer</h2>");
        html.append("<p><a href=\"/captureImage\">Capture New Image</a></p>");
        html.append("<img id=\"capturedImage\" src=\"/image\" alt=\"Captured Image\" style=\"width:640px;height:480px;\"/>");

        //Mini-Game Score Section
        html.append("<h2>Mini-Game Score</h2>");
        html.append("<p>Last Game Score: <span id=\"score\">Loading...</span></p>");
        html.append("<p>Max Possible Score: <span id=\"maxScore\">Loading...</span></p>");

        //Commands Section
        html.append("<h2>Execute Commands</h2>");
        html.append("<div id=\"commands\">");

        //Get registered commands from CommandRegistry
        Set<Class<? extends Command>> commandClasses = commandRegistry.getCommands();
        for (Class<? extends Command> commandClass : commandClasses) {
            String commandName = commandClass.getSimpleName();
            html.append("<button onclick=\"executeCommand('" + commandName + "')\">" + commandName + "</button>");
        }
        html.append("</div>");

        //JavaScript Section
        html.append("<script>");

        //Function to execute commands
        html.append("function executeCommand(commandName) {");
        html.append("  var xhr = new XMLHttpRequest();");
        html.append("  xhr.open('POST', '/simulateButtonPress', true);");
        html.append("  xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');");
        html.append("  xhr.onreadystatechange = function() {");
        html.append("    if (xhr.readyState == XMLHttpRequest.DONE) {");
        html.append("      alert('Command ' + commandName + ' executed.');");
        html.append("    }");
        html.append("  };");
        html.append("  xhr.send('command=' + commandName);");
        html.append("}");

        //Function to update game score
        html.append("function updateScore() {");
        html.append("  fetch('/gameScore').then(response => response.json()).then(data => {");
        html.append("    document.getElementById('score').innerText = data.score;");
        html.append("    document.getElementById('maxScore').innerText = data.maxScore;");
        html.append("  });");
        html.append("}");
        html.append("setInterval(updateScore, 1000);"); //Update score every second

        html.append("</script>");
        html.append("</body></html>");

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveImage() {
        try {
            BufferedImage image = ImageStore.getLastCapturedImage();

            if (image != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "JPEG", baos);
                InputStream is = new ByteArrayInputStream(baos.toByteArray());
                return newChunkedResponse(Response.Status.OK, "image/jpeg", is);
            } else {
                String html = "<html><body>" +
                        "<h1>No Image Available</h1>" +
                        "<p>Please capture an image first.</p>" +
                        "<p><a href=\"/\">Go Back</a></p>" +
                        "</body></html>";
                return newFixedLengthResponse(Response.Status.OK, "text/html", html);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error serving image.");
        }
    }

    private Response handleCaptureImage() {
        try {
            //Simulate button press to capture image
            String commandName = "CaptureImageCommand";
            simulateCommandButtonPress(commandName);

            return newFixedLengthResponse(Response.Status.OK, "text/html",
                    "<html><body>" +
                            "<h1>Image Captured</h1>" +
                            "<p><a href=\"/\">Go Back</a></p>" +
                            "</body></html>");
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error capturing image.");
        }
    }

    private Response serveGameScore() {
        int score = GameResultStore.getLastGameScore();
        int maxScore = GameResultStore.getLastGameMaxScore();
        String json = "{ \"score\": " + score + ", \"maxScore\": " + maxScore + " }";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response handleSimulateButtonPress(Map<String, String> params) {
        String commandName = params.get("command");
        if (commandName == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing 'command' parameter");
        }

        try {
            simulateCommandButtonPress(commandName);
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Command executed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error executing command");
        }
    }

    private void simulateCommandButtonPress(String commandName) {
        //Find the command class by name
        Class<? extends Command> commandClass = null;
        Set<Class<? extends Command>> commandClasses = commandRegistry.getCommands();
        for (Class<? extends Command> cls : commandClasses) {
            if (cls.getSimpleName().equals(commandName)) {
                commandClass = cls;
                break;
            }
        }

        if (commandClass != null) {
            //Get the button combination for the command
            Set<Button> buttonCombination = null;
            for (Map.Entry<Set<Button>, Class<? extends Command>> entry : commandRegistry.getButtonCommandMap().entrySet()) {
                if (entry.getValue().equals(commandClass)) {
                    buttonCombination = entry.getKey();
                    break;
                }
            }

            if (buttonCombination != null) {
                //Simulate button presses
                for (Button button : buttonCombination) {
                    buttonListener.simulateButtonPress(button);
                }
            } else {
                throw new IllegalArgumentException("No button combination found for command: " + commandName);
            }
        } else {
            throw new IllegalArgumentException("Invalid command name: " + commandName);
        }
    }
}