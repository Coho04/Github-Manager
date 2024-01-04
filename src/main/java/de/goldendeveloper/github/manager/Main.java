package de.goldendeveloper.github.manager;

import org.kohsuke.github.*;

import java.util.Calendar;

public class Main {

    private static Config config;

    public static void main(String[] args) {
        config = new Config();
        String device = System.getProperty("os.name").split(" ")[0];
        if (!device.equalsIgnoreCase("windows") && !device.equalsIgnoreCase("Mac")) {
            new Discord();
        }
        Calendar timeOfDay = Calendar.getInstance();
        timeOfDay.set(Calendar.HOUR_OF_DAY, 12);
        timeOfDay.set(Calendar.MINUTE, 0);
        timeOfDay.set(Calendar.SECOND, 0);
        new DailyRunnerDaemon(timeOfDay, () -> {
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
                System.out.println(e.getMessage());
                System.out.println("An error occurred performing daily housekeeping");
            }
        }, "daily-housekeeping").start();
    }

    public static Config getConfig() {
        return config;
    }
}
