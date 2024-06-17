package de.goldendeveloper.github.manager;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;

public class Config {

    private final String githubToken;
    private final String discordWebhook;
    private final String sentryDns;
    private final String defaultHomepage;
    private final List<String> ignoredRepositories;
    private final List<String> ignoredLanguages;
    private final List<String> dependabotReviewers;
    private final List<String> defaultTopics;
    private final List<String> organisations;
    private final List<String> users;

    public Config() {
        Dotenv dotenv = Dotenv.load();
        githubToken = dotenv.get("GITHUB_TOKEN");
        discordWebhook = dotenv.get("DISCORD_WEBHOOK");
        sentryDns = dotenv.get("SENTRY_DNS");
        defaultHomepage = dotenv.get("DEFAULT_HOMEPAGE").replace(" ", "");
        ignoredRepositories = List.of(dotenv.get("IGNORED_REPOSITORIES").replace(" ", "").split(","));
        ignoredLanguages = List.of(dotenv.get("IGNORED_LANGUAGES").replace(" ", "").split(","));
        dependabotReviewers = List.of(dotenv.get("DEPENDABOT_REVIEWERS").replace(" ", "").split(","));
        defaultTopics = List.of(dotenv.get("DEFAULT_TOPICS").replace(" ", "").split(","));
        organisations = List.of(dotenv.get("ORGANIZATIONS").replace(" ", "").split(","));
        users = List.of(dotenv.get("USERS").replace(" ", "").split(","));
    }

    public String getGithubToken() {
        return githubToken;
    }

    public String getDiscordWebhook() {
        return discordWebhook;
    }

    public String getSentryDns() {
        return sentryDns;
    }

    public List<String> getDependabotReviewers() {
        return dependabotReviewers;
    }

    public List<String> getIgnoredRepositories() {
        return ignoredRepositories;
    }

    public List<String> getDefaultTopics() {
        return defaultTopics;
    }

    public List<String> getIgnoredLanguages() {
        return ignoredLanguages;
    }

    public String getDefaultHomepage() {
        return defaultHomepage;
    }

    public List<String> getOrganisations() {
        return organisations;
    }

    public List<String> getUsers() {
        return users;
    }
}
