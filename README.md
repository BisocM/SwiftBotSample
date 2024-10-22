# SwiftBot Sample Project

## Overview

The **SwiftBot Sample Project** is a Java application that demonstrates how to interact with a SwiftBot using commands, capture images, play a mini-game, and manage bot controls via a web interface. This project integrates command handling, event listening, and web technologies to provide an interactive experience with the SwiftBot.

![SwiftBot](https://imagedelivery.net/yMVZazkXH-qQOiCSpkNtBA/9c2e6753-23d7-4afd-6146-0454bea10d00/public) <!-- Replace with an actual image URL if available -->

## Features

- **Obstacle Navigation**: Obstacle navigation utilizing the HC-SR04 ultrasonic sensor.
- **Light Show**: Run a simple light show using the 6 RGB LEDs driven by the SN3218 board, on the bottom plate of the Swift Bot.
- **Image Capture**: Capture images using the SwiftBot's camera and display them on a web interface.
- **Whack-A-Mole Mini-Game**:
    - Play a whack-a-mole style game using the SwiftBot's buttons.
    - Combos increase the score multiplier.
    - Score and max possible score are displayed on the web interface.
- **Web Interface**:
    - View captured images.
    - Monitor game scores in real-time.
    - Execute registered commands via simulated button presses.
- **Command and Event Handling**:
    - Register commands with associated button combinations.
    - Use `ButtonListener` to detect button presses and trigger commands.
    - Handle commands and events asynchronously.

## Getting Started

### Prerequisites

- **Java Development Kit (JDK) 17**.
- **Maven** for project building and dependency management.
- **SwiftBot Hardware** with the appropriate SDK/API available.
- **Internet Browser** to access the web interface.

### Installation

1. **Clone the Repository**

   ```bash
   git clone https://github.com/BisocM/swiftbot-sample-project.git
   cd swiftbot-sample-project
   ```

2. **Build the Project**

   Use Maven to compile the project:

   ```bash
   mvn clean compile
   ```

### Running the Application

Run the application using Maven:

```bash
mvn exec:java -Dexec.mainClass="org.swiftbotsample.app.Main"
```

### Accessing the Web Interface

Once the application is running, open your web browser and navigate to:

```
http://localhost:8080/ # OR localIp:8080
```

If the SwiftBot is connected to a different machine, replace `localhost` with the appropriate IP address.

## Usage

### Web Interface

The web interface provides an interactive way to control and monitor the SwiftBot.

#### Image Viewer

- Click on **"Capture New Image"** to take a new picture using the SwiftBot's camera.
- The captured image will be displayed below the button.
- Images are refreshed each time you capture a new one.

#### Mini-Game Score

- The **Whack-A-Mole** mini-game score and the maximum possible score are displayed and updated in real-time.
- Scores are updated every second.

#### Execute Commands

- A list of registered commands is displayed as buttons.
- Click on any command button to execute it.
- Commands are executed via simulated button presses using the `ButtonListener`.

## Project Structure

- **Main Application**: `org.swiftbotsample.app.Main`
    - Initializes components and starts the web server.

- **Commands**:
    - **CaptureImageCommand**: Command to capture an image.
    - **WhackAMoleCommand**: Command to start the mini-game.
    - **LightShowCommand**: Command to start the light show.
    - **NavigateObstaclesCommand**: Command to initialize obstacle navigation.

- **Web Server**: `org.swiftbotsample.app.web.BotWebServer`
    - Serves the web interface and handles HTTP requests.

- **Utilities**:
    - **ButtonListener**: Listens for button presses and triggers commands.
    - **CommandRegistry**: Registers commands and associates them with button combinations.
    - **GameResultStore**: Stores game scores for access by the web server.
    - **ImageStore**: Stores captured images for display on the web interface.

- **Core Framework**: `org.swiftbotsample.cqrs.core`
    - Implements the command-query responsibility segregation (CQRS) pattern.
    - Provides base classes and interfaces for commands and handlers.

## Extending the Project

### Adding New Commands

1. **Create Command Class**

   ```java
   @CommandAttribute(
        menu = BotMenuState.class,
        ordinal = 0,
        priority = 2,
        buttons = {ButtonName.A, ButtonName.B} )
   public class NewCommand implements Command {
       public final SwiftBotAPI api;

       public NewCommand(SwiftBotAPI api) {
           this.api = api;
       }
   }
   ```

2. **Create Command Handler**

   ```java
   public class NewCommandHandler extends CommandHandler<NewCommand> {
       @Override
       public void handle(NewCommand command) {
           // Implement command logic
       }
   }
   ```

3. **Register Command**

    - The `CommandRegistry` automatically detects and registers commands annotated with `@CommandAttribute`.
    - Ensure your new command is in the package scanned by the registry.

4. **Update Web Interface**

    - Commands are automatically displayed on the web interface.
    - Users can execute the new command via the corresponding button.

## Dependencies

- **SwiftBot API**: Interface to control the SwiftBot hardware.
- **NanoHTTPD**: Lightweight HTTP server for Java.
- **Reflections**: For runtime classpath scanning.
- **Maven**: Build and dependency management.

## Contributing

1. **Fork the Repository**
2. **Create a Feature Branch**

   ```bash
   git checkout -b feature/new-feature
   ```

3. **Commit Changes**

   ```bash
   git commit -am 'Add new feature'
   ```

4. **Push to the Branch**

   ```bash
   git push origin feature/new-feature
   ```

5. **Create a Pull Request**

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **SwiftBot**: For providing the hardware and API for this project.
- **NanoHTTPD**: For the lightweight HTTP server.

---