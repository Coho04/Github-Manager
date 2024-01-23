package de.goldendeveloper.github.manager.console.commands;

import de.goldendeveloper.github.manager.console.ConsoleReader;
import de.goldendeveloper.github.manager.interfaces.ConsoleCommandInterface;

import java.util.Map;

public class HelpCommand implements ConsoleCommandInterface {

    @Override
    public void run() {
        for (Map.Entry<String, ConsoleCommandInterface> entry : ConsoleReader.commands.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue().commandInfo());
        }
    }

    @Override
    public String commandName() {
        return "help";
    }

    @Override
    public String commandInfo() {
        return "Zeigt diese Hilfe an";
    }
}