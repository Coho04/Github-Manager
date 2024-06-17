package de.goldendeveloper.github.manager;

import io.github.coho04.githubapi.builders.GHFileBuilder;
import io.github.coho04.githubapi.entities.repositories.GHBranch;
import io.github.coho04.githubapi.entities.repositories.GHFile;
import io.github.coho04.githubapi.entities.repositories.GHRepository;
import io.sentry.Sentry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RepositoryProcessor {

    private String branche = "main";

    public void process(GHRepository repo) throws IOException {
        branche = "main";
        checkBranchOrMakeIssue(repo);
        checkDescriptionOrMakeIssue(repo);
        checkOrUploadFiles(repo, "", List.of("CODE_OF_CONDUCT.md", "CONTRIBUTING.md", "LICENSE", "SECURITY.md", "README.md"));
        checkOrUploadFiles(repo, ".github/ISSUE_TEMPLATE", List.of("bug_report.md", "feature_request.md"));
        checkOrUploadFiles(repo, ".github", List.of("PULL_REQUEST_TEMPLATE.md"));
        checkFileNotExistsOrMakeIssue(repo, "", ".env", "Delete .env file");
        if (repo.getLanguage() != null) {
            if (repo.getLanguage().equalsIgnoreCase("Java")) {
                checkDirectoryNotExistsOrMakeIssue(repo, ".idea", "Remove .idea directory");
                checkOrUpload(repo, ".github/workflows", "build.yml", "Create build.yml", ".github/workflows/java/maven.yml");
            }
            checkOrUpdateDependabot(repo);
        }
        checkWebsite(repo);
        checkTopics(repo);
    }

    private void checkOrUpload(GHRepository repo, String directoryPath, String fileName, String commitMessage, String localPath) throws IOException {
        String content = readResource(localPath);
        String finalContent = content.replace("REPO_NAME", repo.getName());
        try {
            List<GHFile> directory = repo.getDirectoryContent(directoryPath);
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
                Main.getLogger().log(Level.SEVERE, e.getMessage(), e);
                Sentry.setTag("Repo-Name", repo.getName());
                Sentry.captureException(e);
            }
        }
    }

    private void checkOrUploadFiles(GHRepository repo, String directoryPath, List<String> files) {
        try {
            List<GHFile> directory = repo.getDirectoryContent(directoryPath);
            for (String fileToUpload : files) {
                String content = readResource((directoryPath.isEmpty() ? "" : directoryPath + "/") + fileToUpload);
                String finalContent = content.replace("REPO_NAME", repo.getName());
                if (directory.isEmpty() || directory.stream().noneMatch(file -> file.getName().equals(fileToUpload))) {
                    if (!content.isEmpty()) {
                        String path = fileToUpload;
                        if (!directoryPath.isEmpty()) {
                            path = directoryPath + "/" + fileToUpload;
                        }
                        uploadFile(repo, finalContent, "Create " + fileToUpload, path);
                    }
                }
            }
        } catch (Exception e) {
            Main.getLogger().log(Level.SEVERE, e.getMessage(), e);
            Sentry.setTag("Repo-Name", repo.getName());
            Sentry.captureException(e);
//            }
        }
    }


    public void uploadFile(GHRepository repo, String content, String commit, String path) {
        if (repoIsNotIgnored(repo)) {
            GHFileBuilder fileBuilder = repo.addFile();
            fileBuilder.setBranch(branche);
            fileBuilder.setContent(content);
            fileBuilder.setMessage(commit);
            fileBuilder.setPath(path);
            fileBuilder.commit();
        }
    }

    public void makeIssue(GHRepository repo, String title) throws IOException {
        if (repoIsNotIgnored(repo) && repo.getIssues().stream().noneMatch(issue -> issue.getTitle().equals(title))) {
            repo.createIssue(title).assignee("Coho04").create();
        } else if (!repoIsNotIgnored(repo) && repo.getIssues().stream().anyMatch(issue -> issue.getTitle().equals(title))) {
            repo.getIssues().stream().filter(issue -> issue.getTitle().equals(title)).forEach(issue -> {
                if (!repo.isArchived()) {
                    issue.close();
                }
            });
        }
    }

    public void checkWebsite(GHRepository repo) {
        if (repoIsNotIgnored(repo) && repo.getHomepage() == null) {
            String homepage = Main.getConfig().getDefaultHomepage();
            if (homepage != null) {
                repo.updateHomePage(homepage);
            }
        }
    }

    public void checkTopics(GHRepository repo) {
        if (repoIsNotIgnored(repo)) {
            List<String> defaultTopics = Main.getConfig().getDefaultTopics();
            List<String> topics = repo.getTopics();
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
            repo.updateTopics(topics);
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
            List<GHFile> directoryContent = repository.getDirectoryContent(directory);
            if (directoryContent.stream().anyMatch(content -> content.getName().equals(file))) {
                makeIssue(repository, issueTitle);
            }
        } catch (IOException e) {
            Sentry.captureException(e);
            Main.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }


    private void checkDirectoryNotExistsOrMakeIssue(GHRepository repository, String directory, String issueTitle) {
        try {
            List<GHFile> directoryContent = repository.getDirectoryContent(directory);
            if (!directoryContent.isEmpty()) {
                makeIssue(repository, issueTitle);
            }
        } catch (IOException e) {
            if (!e.getMessage().contains("Not Found")) {
                Sentry.captureException(e);
                Main.getLogger().log(Level.SEVERE, e.getMessage(), e);
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
            Main.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void checkDescriptionOrMakeIssue(GHRepository repository) {
        try {
            if (repository.getDescription() == null || repository.getDescription().isEmpty()) {
                makeIssue(repository, "Add a description to the repository");
            }
        } catch (IOException e) {
            Sentry.captureException(e);
            Main.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public boolean repoIsNotIgnored(GHRepository repo) {
        List<String> ignoredRepos = Main.getConfig().getIgnoredRepositories().stream().map(String::toLowerCase).toList();
        List<String> ignoredLanguages = Main.getConfig().getIgnoredLanguages().stream().map(String::toLowerCase).toList();
        if (repo.isArchived() || repo.getLanguage() == null || ignoredLanguages.contains(repo.getLanguage().toLowerCase())) {
            return false;
        }
        return !ignoredRepos.contains(repo.getName().toLowerCase());
    }

    public void checkOrUpdateDependabot(GHRepository repo) throws IOException {
        if (!repo.isArchived()) {
            HashMap<String, String> packages = new HashMap<>();
            packages.put("package.json", "npm");
            packages.put("pom.xml", "maven");
            packages.put("build.gradle", "gradle");
            packages.put("composer.json", "composer");
            try {
                List<GHFile> directory = repo.getDirectoryContentWithFileContent(".github");
                if (directory.isEmpty() || directory.stream().noneMatch(file -> file.getName().equals("dependabot.yml"))) {
                    List<GHFile> rootDirectory = repo.getDirectoryContent("");
                    if (!rootDirectory.isEmpty()) {
                        List<GHFile> p = rootDirectory.stream().filter(file -> packages.containsKey(file.getName().toLowerCase())).toList();
                        if (!p.isEmpty()) {
                            StringBuilder builder = new StringBuilder();
                            for (GHFile file : p) {
                                addPackageManager(builder, packages.get(file.getName().toLowerCase()));
                            }
                            List<GHFile> workflows = repo.getDirectoryContent(".github/workflows");
                            if (!workflows.isEmpty()) {
                                addPackageManager(builder, "github-actions");
                            }

                            String content = readResource(".github/dependabot.yml");
                            String finalContent = content.replace("PACKAGES", builder.toString());
                            GHFileBuilder fileBuilder = repo.addFile();
                            fileBuilder.setBranch(branche);
                            fileBuilder.setContent(finalContent);
                            fileBuilder.setMessage("Create dependabot.yml");
                            fileBuilder.setPath(".github/dependabot.yml");
                            fileBuilder.commit();
                        }
                    }
                } else {
                    GHFile dependabot = directory.stream().filter(file -> file.getName().equals("dependabot.yml")).findFirst().orElse(null);
                    if (dependabot != null) {
                        List<GHFile> rootDirectory = repo.getDirectoryContent("");
                        if (!rootDirectory.isEmpty()) {
                            List<GHFile> p = rootDirectory.stream().filter(file -> packages.containsKey(file.getName().toLowerCase())).toList();
                            if (!p.isEmpty()) {
                                String content = dependabot.getContent();
                                StringBuilder builder = new StringBuilder();
                                boolean newPackageManager = false;
                                for (GHFile file : p) {
                                    String packageManager = packages.get(file.getName().toLowerCase());
                                    if (!content.contains(packageManager)) {
                                        newPackageManager = true;
                                        addPackageManager(builder, packages.get(file.getName().toLowerCase()));
                                    }
                                }
                                List<GHFile> workflows = repo.getDirectoryContent(".github/workflows");
                                if (!workflows.isEmpty()) {
                                    if (!content.contains("github-actions")) {
                                        newPackageManager = true;
                                        addPackageManager(builder, "github-actions");
                                    }
                                }
                                if (newPackageManager) {
                                    String finalContent = content + "\n" + builder;
                                    GHFileBuilder fileBuilder = dependabot.updateFile();
                                    fileBuilder.setBranch(branche);
                                    fileBuilder.setContent(finalContent);
                                    fileBuilder.setMessage("Update dependabot.yml");
                                    fileBuilder.setPath(".github/dependabot.yml");
                                    fileBuilder.commit();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (e.getMessage().contains("Not Found")) {
                    makeIssue(repo, "Create dependabot.yml");
                } else {
                    Sentry.captureException(e);
                    Main.getLogger().log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }

    private void addPackageManager(StringBuilder builder, String packageManager) {
        builder.append("\n");
        builder.append("  - package-ecosystem: \"");
        builder.append(packageManager);
        builder.append("\"\n");
        builder.append("    directory: \"/\"\n");
        builder.append("    schedule:\n");
        builder.append("      interval: \"daily\"\n");
        builder.append("    assignees:\n");
        builder.append(Main.getConfig().getDependabotReviewers().stream().map(s -> "    - \"" + s + "\"").collect(Collectors.joining("\n")));
        builder.append("\n");
        builder.append("    reviewers:\n");
        builder.append(Main.getConfig().getDependabotReviewers().stream().map(s -> "    - \"" + s + "\"").collect(Collectors.joining("\n")));
    }
}
