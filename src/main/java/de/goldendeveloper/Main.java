package de.goldendeveloper;

import org.kohsuke.github.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        GitHub github = GitHub.connect("Coho04", "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");
        GHOrganization gdOrganization = github.getOrganization("Golden-Developer");
        RepositoryProcessor processor = new RepositoryProcessor();
        for (GHRepository repo : gdOrganization.listRepositories()) {
            processor.process(repo);
        }
    }
}