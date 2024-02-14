package de.goldendeveloper.github.manager;

import de.goldendeveloper.github.manager.console.ConsoleReader;
import io.sentry.ITransaction;
import io.sentry.SpanStatus;
import org.kohsuke.github.*;
import io.sentry.Sentry;

import java.util.Calendar;

public class Main {

    private static Config config;

    public static void main(String[] args) {
        config = new Config();
        Sentry.init(options -> {
            options.setDsn(config.getSentryDns());
            options.setTracesSampleRate(1.0);
        });

        ITransaction transaction = Sentry.startTransaction("startProcess()", "task");
        try {
            startProcess();
        } catch (Exception e) {
            transaction.setThrowable(e);
            transaction.setStatus(SpanStatus.INTERNAL_ERROR);
            Sentry.captureException(e);
            throw e;
        } finally {
            transaction.finish();
        }
    }

    private static void startProcess() {
        String device = System.getProperty("os.name").split(" ")[0];
        if (!device.equalsIgnoreCase("windows") && !device.equalsIgnoreCase("Mac")) {
            new Discord();
        }
        new ConsoleReader();
        System.out.println("Starting daily housekeeping daemon");
        Calendar timeOfDay = Calendar.getInstance();
        timeOfDay.set(Calendar.HOUR_OF_DAY, 12);
        timeOfDay.set(Calendar.MINUTE, 0);
        timeOfDay.set(Calendar.SECOND, 0);
        new DailyRunnerDaemon(timeOfDay, Main::runRepositoryProcessor, "daily-housekeeping").start();
    }

    public static void runRepositoryProcessor() {
        try {
            GitHub github = GitHub.connect(config.getGithubUsername(), config.getGithubToken());
            GHOrganization gdOrganization = github.getOrganization("Golden-Developer");
            RepositoryProcessor processor = new RepositoryProcessor();

            int totalRepos = gdOrganization.listRepositories().toList().size();
            LoadingBar loadingBar = new LoadingBar(totalRepos);

            for (GHRepository repo : gdOrganization.listRepositories()) {
                processor.process(repo);
                loadingBar.updateProgress();
            }
        } catch (Exception e) {
            System.out.println("An error occurred performing daily housekeeping");
            System.out.println("ErrorMessage: " + e.getMessage());
            System.out.println(e.getCause());
            Sentry.captureException(e);
            e.printStackTrace();
        }
    }

    public static Config getConfig() {
        return config;
    }
}
