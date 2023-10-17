package de.goldendeveloper;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.List;

public class RepositoryProcessor {

    public void process(GHRepository repo) throws IOException {
        System.out.println("Processing " + repo.getName() + "...");

//        checkOrMakeIssue(repo, ".github/ISSUE_TEMPLATE", "bug_report.md", "Create bug_report.md");
//        checkOrMakeIssue(repo, ".github/ISSUE_TEMPLATE", "feature_request.md", "Create feature_request.md");
//        checkOrMakeIssue(repo, ".github", "dependabot.yml", "Create dependabot.yml");
//        checkOrMakeIssue(repo, ".github", "PULL_REQUEST_TEMPLATE.md", "Create PULL_REQUEST_TEMPLATE.md");
//        checkOrMakeIssue(repo, ".github", "CODE_OF_CONDUCT.md", "Create CODE_OF_CONDUCT.md");
//        checkOrMakeIssue(repo, ".github", "CONTRIBUTING.md", "Create CONTRIBUTING.md");
//        checkOrMakeIssue(repo, ".github", "LICENSE", "Create LICENSE");
        // ... und so weiter für andere Überprüfungen
        if (repo.getLanguage() != null) {
            System.out.println("Programming Language: " + repo.getLanguage());
            if (repo.getLanguage().equalsIgnoreCase("Java")) {
                checkOrMakeIssue(repo, ".github/workflows", "build.yml", "Create build.yml");
            }
        }
        System.out.println("Finished processing " + repo.getName() + "!");
    }

    private void checkOrMakeIssue(GHRepository repo, String directoryPath, String fileName, String issueTitle) throws IOException {
        try {
            List<GHContent> directory = repo.getDirectoryContent(directoryPath);
            if (directory.isEmpty() || directory.stream().noneMatch(file -> file.getName().equals(fileName))) {
                makeIssue(repo, issueTitle);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Not Found")) {
                makeIssue(repo, issueTitle);
            } else {
                System.out.println("Error in " + repo.getName() + ": " + e.getMessage());
            }
        }
    }

    private void makeIssue(GHRepository repo, String title) throws IOException {
        if (repo.getIssues(GHIssueState.ALL).stream().noneMatch(issue -> issue.getTitle().equals(title))) {
            System.out.println("Creating issue '" + title + "' in '" + repo.getName() + "'...");
            // repo.createIssue(title).assignee("Coho04").create();
        }
    }
}
