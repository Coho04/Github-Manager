package de.goldendeveloper.github.manager;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {

    private final String githubToken;
    private final String githubUsername;
    private final String discordWebhook;

    public Config() {
        Dotenv dotenv = Dotenv.load();
        githubToken = dotenv.get("GITHUB_TOKEN");
        githubUsername = dotenv.get("GITHUB_USERNAME");
        discordWebhook = dotenv.get("DISCORD_WEBHOOK");
    }

    public String getGithubToken() {
        return githubToken;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public String getDiscordWebhook() {
        return discordWebhook;
    }
}
