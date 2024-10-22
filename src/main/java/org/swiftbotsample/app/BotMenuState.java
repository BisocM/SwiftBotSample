package org.swiftbotsample.app;

import org.swiftbotsample.cqrs.annotations.MenuState;

//This is technically useless since we do not have a CLI to interface with the bot anyway.
//It IS meant to output a nice menu from which the user can select a given command.
public enum BotMenuState implements MenuState {
    Init,
}