package org.swiftbotsample.app.commands.types;

import org.swiftbotsample.app.BotMenuState;
import org.swiftbotsample.app.ButtonName;
import org.swiftbotsample.cqrs.annotations.CommandAttribute;
import org.swiftbotsample.cqrs.core.Command;
import swiftbot.SwiftBotAPI;

@CommandAttribute(
        menu = BotMenuState.class,
        ordinal = 0,
        priority = 3,
        buttons = {ButtonName.B, ButtonName.X}
)
public class WhackAMoleCommand implements Command {
    public final SwiftBotAPI api;

    public WhackAMoleCommand(SwiftBotAPI api) {
        this.api = api;
    }
}