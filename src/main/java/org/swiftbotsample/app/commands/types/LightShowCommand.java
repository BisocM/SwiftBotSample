package org.swiftbotsample.app.commands.types;


import org.swiftbotsample.app.BotMenuState;
import org.swiftbotsample.app.ButtonName;
import org.swiftbotsample.cqrs.annotations.CommandAttribute;
import org.swiftbotsample.cqrs.core.Command;
import swiftbot.SwiftBotAPI;

@CommandAttribute(
        menu = BotMenuState.class,
        ordinal = 0,
        priority = 1,
        buttons = {ButtonName.X, ButtonName.Y}
)
public class LightShowCommand implements Command {
    public final SwiftBotAPI api;

    public LightShowCommand(SwiftBotAPI api) {
        this.api = api;
    }
}