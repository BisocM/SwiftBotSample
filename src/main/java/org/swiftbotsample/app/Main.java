package org.swiftbotsample.app;

import fi.iki.elonen.NanoHTTPD;
import org.swiftbotsample.app.web.BotWebServer;
import org.swiftbotsample.cqrs.core.CommandRegistry;
import org.swiftbotsample.cqrs.core.MenuManager;
import swiftbot.SwiftBotAPI;

import java.io.IOException;

//SSH Maven Config: clean install exec:java

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        SwiftBotAPI swiftBot = new SwiftBotAPI();

        //Dynamically get the package name
        String assemblyName = Main.class.getPackage().getName();

        //Register all the commands
        CommandRegistry registry = new CommandRegistry("org.swiftbotsample.app");
        MenuManager menuManager = new MenuManager("org.swiftbotsample.app");

        //Initialize and start the button listener
        ButtonListener buttonListener = new ButtonListener(swiftBot, registry, menuManager);

        //Start the web server
        BotWebServer webServer = new BotWebServer(8080, swiftBot, buttonListener, registry);
        webServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Web server started on http://localhost:8080/");

        //Keep the program running indefinitely
        synchronized (Main.class) {
            Main.class.wait();
        }
    }
}