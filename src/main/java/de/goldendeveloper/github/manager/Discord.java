package de.goldendeveloper.github.manager;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

import java.util.Date;

public class Discord {

    public Discord() {
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setColor(0x00FF00);
        embed.addField(new WebhookEmbed.EmbedField(false, "[Status]", "ONLINE"));
        embed.setAuthor(new WebhookEmbed.EmbedAuthor("Github-Manager", null, "https://Golden-Developer.de"));
        embed.addField(new WebhookEmbed.EmbedField(false, "Gestartet als", "Github-Manager"));
        embed.addField(new WebhookEmbed.EmbedField(false, "Status", "\uD83D\uDFE2 Gestartet"));
        embed.setFooter(new WebhookEmbed.EmbedFooter("@Golden-Developer", null));
        embed.setTimestamp(new Date().toInstant());
        new WebhookClientBuilder(Main.getConfig().getDiscordWebhook()).build().send(embed.build());
    }
}
