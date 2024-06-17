package de.goldendeveloper.github.manager;

import de.goldendeveloper.github.manager.console.ConsoleReader;
import de.goldendeveloper.github.manager.utilities.LoadingBar;
import de.goldendeveloper.github.manager.utilities.LogFormatter;
import io.github.coho04.githubapi.Github;
import io.github.coho04.githubapi.entities.GHOrganisation;
import io.github.coho04.githubapi.entities.GHUser;
import io.github.coho04.githubapi.entities.repositories.GHRepository;
import io.sentry.ITransaction;
import io.sentry.SpanStatus;
import io.sentry.Sentry;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static Config config;
    private static Logger logger;

    public static void main(String[] args) {
        logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setUseParentHandlers(false);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        logger.addHandler(handler);

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
            if (config.getDiscordWebhook() != null && !config.getDiscordWebhook().isEmpty()) {
                new Discord();
            }
        }
        new ConsoleReader();
        logger.info("Starting daily housekeeping daemon");
        Calendar timeOfDay = Calendar.getInstance();
        timeOfDay.set(Calendar.HOUR_OF_DAY, 12);
        timeOfDay.set(Calendar.MINUTE, 0);
        timeOfDay.set(Calendar.SECOND, 0);
        new DailyRunnerDaemon(timeOfDay, Main::runRepositoryProcessor, "daily-housekeeping").start();
    }

    public static void runRepositoryProcessor() {
        try {
            Github github = new Github(config.getGithubToken());
            config.getOrganisations().forEach(org -> {
                RepositoryProcessor repositoryProcessor = new RepositoryProcessor();
                GHOrganisation organisation = github.findOrganisationByName(org);
                List<GHRepository> repositories = organisation.getRepositories();
                Main.getLogger().info("Organisation: " + organisation.getName());
                processRepositories(repositoryProcessor, repositories);
            });

            config.getUsers().forEach(user -> {
                RepositoryProcessor repositoryProcessor = new RepositoryProcessor();
                GHUser holder = github.findUserByName(user);
                List<GHRepository> repositories = holder.listRepositories();
                Main.getLogger().info("User: " + holder.getUsername());
                processRepositories(repositoryProcessor, repositories);
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            Sentry.captureException(e);
        }
    }

    private static void processRepositories(RepositoryProcessor repositoryProcessor, List<GHRepository> repositories) {
        Main.getLogger().info("Found " + repositories.size() + " repositories");
        LoadingBar loadingBar = new LoadingBar(repositories.size());
        for (GHRepository ghRepository : repositories) {
            try {
                repositoryProcessor.process(ghRepository);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            loadingBar.updateProgress();
        }
        System.out.println();
    }

    public static Logger getLogger() {
        return logger;
    }

    public static Config getConfig() {
        return config;
    }
}
