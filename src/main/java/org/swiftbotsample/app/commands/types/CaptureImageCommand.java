package org.swiftbotsample.app.commands.types;

import org.swiftbotsample.app.BotMenuState;
import org.swiftbotsample.app.ButtonName;
import org.swiftbotsample.cqrs.annotations.CommandAttribute;
import org.swiftbotsample.cqrs.core.Command;
import swiftbot.SwiftBotAPI;

@CommandAttribute(
        menu = BotMenuState.class,
        ordinal = 0,
        priority = 2,
        buttons = {ButtonName.A, ButtonName.B}
)
public class CaptureImageCommand implements Command {
    public final SwiftBotAPI api;

    public CaptureImageCommand(SwiftBotAPI api) {
        this.api = api;
    }
}