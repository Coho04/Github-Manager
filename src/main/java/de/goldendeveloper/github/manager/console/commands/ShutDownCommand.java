package de.goldendeveloper.github.manager.console.commands;

import de.goldendeveloper.github.manager.interfaces.ConsoleCommandInterface;

public class ShutDownCommand implements ConsoleCommandInterface {

    @Override
    public void run() {
        System.exit(0);
    }

    @Override
    public String commandName() {
        return "shutdown";
    }

    @Override
    public String commandInfo() {
        return "Beendet das Programm";
    }
}
