package de.goldendeveloper;

import org.kohsuke.github.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    private static GitHub github;
    private static GHOrganization gdOrganization;

    public static void main(String[] args) {
        try {
            github = GitHub.connect("Coho04", "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");
            gdOrganization = github.getOrganization("Golden-Developer");
            processRepositories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void processRepositories() throws IOException {
        RepositoryProcessor processor = new RepositoryProcessor();
        for (GHRepository repo : gdOrganization.listRepositories()) {
            processor.process(repo);
        }
    }
}