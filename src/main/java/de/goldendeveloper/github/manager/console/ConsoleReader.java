package de.goldendeveloper.github.manager.console;

import de.goldendeveloper.github.manager.interfaces.ConsoleCommandInterface;
import io.sentry.Sentry;
import org.reflections.Reflections;

import java.util.*;

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
                System.out.println(e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Geben Sie 'exit' ein, um zu beenden.");
        while (true) {
            String input = scanner.nextLine();
            ConsoleCommandInterface command = commands.get(input);
            if (command != null) {
                command.run();
            } else {
                System.out.println("Unbekannter Befehl");
            }
            if (input.equalsIgnoreCase("exit")) {
                break;
            }
        }
        scanner.close();
    }
}