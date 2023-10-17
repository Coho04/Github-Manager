package de.goldendeveloper;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHContentBuilder;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RepositoryProcessor {

    public void process(GHRepository repo) throws IOException {
        System.out.println("Processing " + repo.getName() + "...");

        checkOrUpload(repo, ".github/ISSUE_TEMPLATE", "bug_report.md", "Create bug_report.md", "src/main/resources/.github/ISSUE_TEMPLATE/bug_report.md");
        checkOrUpload(repo, ".github/ISSUE_TEMPLATE", "feature_request.md", "Create feature_request.md", "src/main/resources/.github/ISSUE_TEMPLATE/feature_request.md");
//        checkOrUpload(repo, ".github", "dependabot.yml", "Create dependabot.yml", "src/main/resources/dependabot.yml"); //TODO: Issue
        checkOrUpload(repo, ".github", "PULL_REQUEST_TEMPLATE.md", "Create PULL_REQUEST_TEMPLATE.md", "src/main/resources/.github/PULL_REQUEST_TEMPLATE.md");
        checkOrUpload(repo, "", "CODE_OF_CONDUCT.md", "Create CODE_OF_CONDUCT.md", "src/main/resources/CODE_OF_CONDUCT.md");
        checkOrUpload(repo, "", "CONTRIBUTING.md", "Create CONTRIBUTING.md", "src/main/resources/CONTRIBUTING.md");
        checkOrUpload(repo, "", "LICENSE", "Create LICENSE", "src/main/resources/LICENSE");
        checkOrUpload(repo, "", "SECURITY.md", "Create SECURITY.md", "src/main/resources/SECURITY.md");

        if (repo.getLanguage() != null) {
            System.out.println("Programming Language: " + repo.getLanguage());
            if (repo.getLanguage().equalsIgnoreCase("Java")) {
//                checkDirectoryNotExistsOrMakeIssue(repo, ".idea", "Remove .idea directory");
            }
        }
        System.out.println("Finished processing " + repo.getName() + "!");
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

    public void uploadFile(GHRepository repo, String content, String commit, String path) throws IOException {
        if (!repo.isArchived()) {
            GHContentBuilder contentBuilder = repo.createContent();
            contentBuilder.branch("main");
            contentBuilder.content(content);
            contentBuilder.message(commit);
            contentBuilder.path(path);
            contentBuilder.commit();
            System.out.println("Uploaded file '" + path + "' to '" + repo.getName() + "'!");
        }
    }

    public String readResource(String localPath) {
        Path path = Paths.get(localPath);
        try {
            StringBuilder builder = new StringBuilder();
            for (String line : Files.readAllLines(path)) {
                builder.append(line).append("\n");
            }
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
