package de.goldendeveloper.github.manager;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {

    private final String githubToken;
    private final String discordWebhook;
    private final String sentryDns;

    public Config() {
        Dotenv dotenv = Dotenv.load();
        githubToken = dotenv.get("GITHUB_TOKEN");
        discordWebhook = dotenv.get("DISCORD_WEBHOOK");
        sentryDns = dotenv.get("SENTRY_DNS");
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
}
