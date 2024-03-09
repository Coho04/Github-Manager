package de.goldendeveloper.github.manager;

import io.sentry.Sentry;
import org.kohsuke.github.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RepositoryProcessor {

    private String branche = "main";

    public void process(GHRepository repo) throws IOException {
        branche = "main";
        checkBranchOrMakeIssue(repo);
        checkDescriptionOrMakeIssue(repo);
        checkOrUpload(repo, ".github/ISSUE_TEMPLATE", "bug_report.md", "Create bug_report.md", ".github/ISSUE_TEMPLATE/bug_report.md");
        checkOrUpload(repo, ".github/ISSUE_TEMPLATE", "feature_request.md", "Create feature_request.md", ".github/ISSUE_TEMPLATE/feature_request.md");

        checkOrUpload(repo, ".github", "PULL_REQUEST_TEMPLATE.md", "Create PULL_REQUEST_TEMPLATE.md", ".github/PULL_REQUEST_TEMPLATE.md");
        checkOrUpload(repo, "", "CODE_OF_CONDUCT.md", "Create CODE_OF_CONDUCT.md", "CODE_OF_CONDUCT.md");
        checkOrUpload(repo, "", "CONTRIBUTING.md", "Create CONTRIBUTING.md", "CONTRIBUTING.md");
        checkOrUpload(repo, "", "LICENSE", "Create LICENSE", "LICENSE");
        checkOrUpload(repo, "", "SECURITY.md", "Create SECURITY.md", "SECURITY.md");

        checkFileNotExistsOrMakeIssue(repo, "", ".env", "Delete .env file");

        if (repo.getLanguage() != null) {
            if (repo.getLanguage().equalsIgnoreCase("Java")) {
                checkDirectoryNotExistsOrMakeIssue(repo, ".idea", "Remove .idea directory");
                checkOrUpload(repo, ".github/workflows", "build.yml", "Create build.yml", ".github/workflows/java/maven.yml");
            }

            if (!repo.getLanguage().equalsIgnoreCase("swift")) {
                checkOrIssue(repo, ".github", "dependabot.yml", "Create dependabot.yml");
            }
        }
        checkWebsite(repo);
        checkTopics(repo);
    }

    private void checkOrUpload(GHRepository repo, String directoryPath, String fileName, String commitMessage, String localPath) throws IOException {
        String content = readResource(localPath);
        String finalContent = content.replace("REPO_NAME", repo.getName());
        try {
            List<GHContent> directory = repo.getDirectoryContent(directoryPath);
            if (directory.isEmpty() || directory.stream().noneMatch(file -> file.getName().equals(fileName))) {
                if (!content.isEmpty()) {
                    String path = fileName;
                    if (!directoryPath.isEmpty()) {
                        path = directoryPath + "/" + fileName;
                    }
                    uploadFile(repo, finalContent, commitMessage, path);
                }
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Not Found")) {
                uploadFile(repo, finalContent, commitMessage, directoryPath + "/" + fileName);
            } else {
                System.out.println("Error in " + repo.getName() + ": " + e.getMessage());
                Sentry.setTag("Repo-Name", repo.getName());
                Sentry.captureException(e);
            }
        }
    }

    private void checkOrIssue(GHRepository repo, String directoryPath, String fileName, String issueTitle) throws IOException {
        try {
            List<GHContent> directory = repo.getDirectoryContent(directoryPath);
            if (directory.isEmpty() || directory.stream().noneMatch(file -> file.getName().equals(fileName))) {
                if (directoryPath.isEmpty()) {
                    makeIssue(repo, issueTitle);
                } else {
                    makeIssue(repo, issueTitle);
                }
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Not Found")) {
                makeIssue(repo, issueTitle);
            } else {
                Sentry.captureException(e);
                System.out.println("Error in " + repo.getName() + ": " + e.getMessage());
            }
        }
    }

    public void uploadFile(GHRepository repo, String content, String commit, String path) throws IOException {
        if (repoIsNotIgnored(repo)) {
            GHContentBuilder contentBuilder = repo.createContent();
            contentBuilder.branch(branche);
            contentBuilder.content(content);
            contentBuilder.message(commit);
            contentBuilder.path(path);
            contentBuilder.commit();
            System.out.println("Uploaded file '" + path + "' to '" + repo.getName() + "'!");
        }
    }

    public void makeIssue(GHRepository repo, String title) throws IOException {
        if (repoIsNotIgnored(repo) && repo.getIssues(GHIssueState.OPEN).stream().noneMatch(issue -> issue.getTitle().equals(title))) {
            repo.createIssue(title).assignee("Coho04").create();
        } else if (!repoIsNotIgnored(repo) && repo.getIssues(GHIssueState.ALL).stream().anyMatch(issue -> issue.getTitle().equals(title))) {
            repo.getIssues(GHIssueState.OPEN).stream().filter(issue -> issue.getTitle().equals(title)).forEach(issue -> {
                try {
                    if (!repo.isArchived()) {
                        issue.close();
                    }
                } catch (IOException e) {
                    Sentry.captureException(e);
                    e.printStackTrace();
                }
            });
        }
    }

    public void checkWebsite(GHRepository repo) throws IOException {
        if (repoIsNotIgnored(repo) && repo.getHomepage() == null) {
            repo.setHomepage("https://Golden-Developer.de");
        }
    }

    public void checkTopics(GHRepository repo) throws IOException {
        if (repoIsNotIgnored(repo)) {
            List<String> defaultTopics = List.of("golden-developer");
            List<String> topics = repo.listTopics();
            for (String topic : defaultTopics) {
                if (!topics.contains(topic)) {
                    topics.add(topic.toLowerCase());
                }
            }
            if (repo.getLanguage() != null && !topics.contains(repo.getLanguage().toLowerCase())) {
                if (!repo.getLanguage().equalsIgnoreCase("c#")) {
                    topics.add(repo.getLanguage().toLowerCase());
                } else {
                    topics.add("csharp");
                }
            }
            try {
                repo.setTopics(topics);
            } catch (IOException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    private void checkFileNotExistsOrMakeIssue(GHRepository repository, String directory, String file, String issueTitle) {
        try {
            List<GHContent> directoryContent = repository.getDirectoryContent(directory);
            if (directoryContent.stream().anyMatch(content -> content.getName().equals(file))) {
                makeIssue(repository, issueTitle);
            }
        } catch (IOException e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }
    }


    private void checkDirectoryNotExistsOrMakeIssue(GHRepository repository, String directory, String issueTitle) {
        try {
            List<GHContent> directoryContent = repository.getDirectoryContent(directory);
            if (!directoryContent.isEmpty()) {
                makeIssue(repository, issueTitle);
            }
        } catch (IOException e) {
            if (!e.getMessage().contains("Not Found")) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }
    }

    private void checkBranchOrMakeIssue(GHRepository repository) {
        try {
            Map<String, GHBranch> branches = repository.getBranches();
            if (branches.values().stream().noneMatch(branch -> branch.getName().equalsIgnoreCase("main"))) {
                if (branches.values().stream().anyMatch(branch -> branch.getName().equalsIgnoreCase("master"))) {
                    makeIssue(repository, "Rename master branch to main");
                    branche = "master";
                } else {
                    makeIssue(repository, "Create main branch");
                }
            }
        } catch (IOException e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }
    }

    private void checkDescriptionOrMakeIssue(GHRepository repository) {
        try {
            if (repository.getDescription() == null || repository.getDescription().isEmpty()) {
                makeIssue(repository, "Add a description to the repository");
            }
        } catch (IOException e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }
    }

    public Boolean repoIsNotIgnored(GHRepository repo) {
        List<String> ignoredRepos = new java.util.ArrayList<>(List.of("TeamSpeakServerInstallScript", "MinecraftInstallScript", ".github", ".github-private"));
        List<String> ignoredLanguages = new java.util.ArrayList<>(List.of("html", "css", "typescript", "shell"));
        ignoredRepos.replaceAll(String::toLowerCase);
        ignoredLanguages.replaceAll(String::toLowerCase);
        if (repo.isArchived() || repo.getLanguage() == null || ignoredLanguages.contains(repo.getLanguage().toLowerCase())) {
            return false;
        }
        return !ignoredRepos.contains(repo.getName().toLowerCase());
    }
}
