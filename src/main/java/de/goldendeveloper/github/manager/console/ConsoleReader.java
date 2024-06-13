package de.goldendeveloper.github.manager.console;

import de.goldendeveloper.github.manager.Main;
import de.goldendeveloper.github.manager.interfaces.ConsoleCommandInterface;
import io.sentry.Sentry;
import org.reflections.Reflections;

import java.util.*;
import java.util.logging.Level;

public class ConsoleReader implements Runnable {

    public static final Map<String, ConsoleCommandInterface> commands = new HashMap<>();

    public ConsoleReader() {
        Thread thread = new Thread(this);
        thread.setName("ConsoleReader");
        loadCommands();
        thread.start();
    }

    private void loadCommands() {
        Reflections reflections = new Reflections("de.goldendeveloper.github.manager");
        Set<Class<? extends ConsoleCommandInterface>> commandClasses = reflections.getSubTypesOf(ConsoleCommandInterface.class);
        for (Class<? extends ConsoleCommandInterface> commandClass : commandClasses) {
            try {
                ConsoleCommandInterface command = commandClass.getDeclaredConstructor().newInstance();
                commands.put(command.commandName(), command);
            } catch (ReflectiveOperationException e) {
                Sentry.captureException(e);
                Main.getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        Main.getLogger().info("Geben Sie 'exit' ein, um zu beenden.");
        while (true) {
            String input = scanner.nextLine();
            ConsoleCommandInterface command = commands.get(input);
            if (command != null) {
                command.run();
            } else {
                Main.getLogger().log(Level.WARNING, "Unbekannter Befehl");
            }
            if (input.equalsIgnoreCase("exit")) {
                break;
            }
        }
        scanner.close();
    }
}