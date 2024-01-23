package de.goldendeveloper.github.manager;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHContentBuilder;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class RepositoryProcessor {


    public void process(GHRepository repo) throws IOException {
        checkOrUpload(repo, ".github/ISSUE_TEMPLATE", "bug_report.md", "Create bug_report.md", "src/main/resources/.github/ISSUE_TEMPLATE/bug_report.md");
        checkOrUpload(repo, ".github/ISSUE_TEMPLATE", "feature_request.md", "Create feature_request.md", "src/main/resources/.github/ISSUE_TEMPLATE/feature_request.md");

        checkOrUpload(repo, ".github", "PULL_REQUEST_TEMPLATE.md", "Create PULL_REQUEST_TEMPLATE.md", "src/main/resources/.github/PULL_REQUEST_TEMPLATE.md");
        checkOrUpload(repo, "", "CODE_OF_CONDUCT.md", "Create CODE_OF_CONDUCT.md", "src/main/resources/CODE_OF_CONDUCT.md");
        checkOrUpload(repo, "", "CONTRIBUTING.md", "Create CONTRIBUTING.md", "src/main/resources/CONTRIBUTING.md");
        checkOrUpload(repo, "", "LICENSE", "Create LICENSE", "src/main/resources/LICENSE");
        checkOrUpload(repo, "", "SECURITY.md", "Create SECURITY.md", "src/main/resources/SECURITY.md");

        checkOrIssue(repo, ".github", "dependabot.yml", "Create dependabot.yml");

        if (repo.getLanguage() != null) {
            if (repo.getLanguage().equalsIgnoreCase("Java")) {
//                checkDirectoryNotExistsOrMakeIssue(repo, ".idea", "Remove .idea directory");
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
                    if (directoryPath.isEmpty()) {
                        uploadFile(repo, finalContent, commitMessage, fileName);
                    } else {
                        uploadFile(repo, finalContent, commitMessage, directoryPath + "/" + fileName);
                    }
                }
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Not Found")) {
                uploadFile(repo, finalContent, commitMessage, directoryPath + "/" + fileName);
            } else {
                System.out.println("Error in " + repo.getName() + ": " + e.getMessage());
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
                System.out.println("Error in " + repo.getName() + ": " + e.getMessage());
            }
        }
    }

    public void uploadFile(GHRepository repo, String content, String commit, String path) throws IOException {
        if (repoIsNotIgnored(repo)) {
            GHContentBuilder contentBuilder = repo.createContent();
            contentBuilder.branch("main");
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
                    topics.add(topic);
                }
            }
            if (repo.getLanguage() != null && !topics.contains(repo.getLanguage().toLowerCase())) {
                topics.add(repo.getLanguage().toLowerCase());
            }
            repo.setTopics(topics);
        }
    }

    public String readResource(String localPath) throws IOException {
        return Files.lines(Paths.get(localPath))
                .collect(Collectors.joining("\n"));
    }

    public Boolean repoIsNotIgnored(GHRepository repo) {
        List<String> ignoredRepos = new java.util.ArrayList<>(List.of("TeamSpeakServerInstallScript", "MinecraftInstallScript", ".github", ".github-private"));
        List<String> ignoredLanguages = new java.util.ArrayList<>(List.of("html", "css", "typescript", "shell"));
        ignoredRepos.replaceAll(String::toLowerCase);
        ignoredLanguages.replaceAll(String::toLowerCase);
        if (repo.isArchived()) {
            return false;
        }
        if (repo.getLanguage() == null) {
            return false;
        }
        if (ignoredLanguages.contains(repo.getLanguage().toLowerCase())) {
            return false;
        }
        return !ignoredRepos.contains(repo.getName().toLowerCase());
    }
}
