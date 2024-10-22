package org.swiftbotsample.app.commands.types;

import org.swiftbotsample.app.BotMenuState;
import org.swiftbotsample.app.ButtonName;
import org.swiftbotsample.cqrs.annotations.CommandAttribute;
import org.swiftbotsample.cqrs.core.Command;
import swiftbot.SwiftBotAPI;

@CommandAttribute(
        menu = BotMenuState.class,
        ordinal = 0,
        priority = 0,
        buttons = {ButtonName.A, ButtonName.X}
)
public class NavigateObstaclesCommand implements Command {
    public SwiftBotAPI api;

    public NavigateObstaclesCommand(SwiftBotAPI api) {
        this.api = api;
    }
}