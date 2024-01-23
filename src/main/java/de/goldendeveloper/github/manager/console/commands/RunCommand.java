package de.goldendeveloper.github.manager.console.commands;

import de.goldendeveloper.github.manager.Main;
import de.goldendeveloper.github.manager.interfaces.ConsoleCommandInterface;

public class RunCommand implements ConsoleCommandInterface {

    @Override
    public void run() {
        System.out.println("Prozess wird manuell ausgeführt");
        Main.runRepositoryProcessor();
        System.out.println("Prozess wurde beendet");
    }

    @Override
    public String commandName() {
        return "run";
    }

    @Override
    public String commandInfo() {
        return "Führt den Prozess manuell aus";
    }
}
