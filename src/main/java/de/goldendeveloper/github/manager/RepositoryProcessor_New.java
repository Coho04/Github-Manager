package de.goldendeveloper.github.manager;

import de.goldendeveloper.github.manager.dataobject.GHRepository;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class RepositoryProcessor_New {

    private String branch = "main";
    private String githubToken = "YOUR_GITHUB_TOKEN";

    public void process(JSONObject repoName) throws IOException {
        GHRepository repository = new GHRepository(repoName);
        checkBranchOrMakeIssue(repoName);
        checkDescriptionOrMakeIssue(repoName);
        checkOrUpload(repoName, ".github/ISSUE_TEMPLATE", "bug_report.md", "Create bug_report.md", ".github/ISSUE_TEMPLATE/bug_report.md");
        checkOrUpload(repoName, ".github/ISSUE_TEMPLATE", "feature_request.md", "Create feature_request.md", ".github/ISSUE_TEMPLATE/feature_request.md");
        checkOrUpload(repoName, ".github", "PULL_REQUEST_TEMPLATE.md", "Create PULL_REQUEST_TEMPLATE.md", ".github/PULL_REQUEST_TEMPLATE.md");
        checkOrUpload(repoName, "", "CODE_OF_CONDUCT.md", "Create CODE_OF_CONDUCT.md", "CODE_OF_CONDUCT.md");
        checkOrUpload(repoName, "", "CONTRIBUTING.md", "Create CONTRIBUTING.md", "CONTRIBUTING.md");
        checkOrUpload(repoName, "", "LICENSE", "Create LICENSE", "LICENSE");
        checkOrUpload(repoName, "", "SECURITY.md", "Create SECURITY.md", "SECURITY.md");
        checkOrUpload(repoName, "", "README.md", "Create README.md", "README.md");
        checkFileNotExistsOrMakeIssue(repoName, "", ".env", "Delete .env file");

        if (getLanguage(repoName) != null) {
            if (getLanguage(repoName).equalsIgnoreCase("Java")) {
                checkDirectoryNotExistsOrMakeIssue(repoName, ".idea", "Remove .idea directory");
                checkOrUpload(repoName, ".github/workflows", "build.yml", "Create build.yml", ".github/workflows/java/maven.yml");
            }

            if (!getLanguage(repoName).equalsIgnoreCase("swift")) {
                checkOrIssue(repoName, ".github", "dependabot.yml", "Create dependabot.yml");
            }
        }
        checkWebsite(repoName);
        checkTopics(repoName);
    }

    private String getLanguage(String repoName) throws IOException {
        String url = "https://api.github.com/repos/" + repoName;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                // Parsing der Antwort, um die Sprache zu erhalten
                // Hier müssen Sie die Logik implementieren, um die Sprache aus der Antwort zu extrahieren
                // Beispiel: {"language": "Java"}
                // Hier wird angenommen, dass die Sprache im JSON-Format als "language" enthalten ist
                // Sie müssen diese Logik entsprechend anpassen, basierend auf der tatsächlichen Antwortstruktur von GitHub
                String responseBody = response.toString();
                String language = ""; // Hier den richtigen Wert setzen
                return language;
            }
        } else {
            throw new IOException("Failed to fetch repository details: " + responseCode);
        }
    }

    private void checkBranchOrMakeIssue(String repoName) throws IOException {
        String url = "https://api.github.com/repos/" + repoName + "/branches/" + branch;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            // Branch nicht gefunden - Issue erstellen
            makeIssue(repoName, "Create main branch");
        } else if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to check branch: " + responseCode);
        }
    }

    private void checkDescriptionOrMakeIssue(String repoName) throws IOException {
        String url = "https://api.github.com/repos/" + repoName;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                String description = response.toString();
                if (description == null || description.isEmpty()) {
                    // Beschreibung fehlt - Issue erstellen
                    makeIssue(repoName, "Add a description to the repository");
                }
            }
        } else {
            throw new IOException("Failed to check repository: " + responseCode);
        }
    }

    private void makeIssue(String repoName, String issueTitle) throws IOException {
        String url = "https://api.github.com/repos/" + repoName + "/issues";
        String requestBody = "{\"title\":\"" + issueTitle + "\"}";

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        con.getOutputStream().write(requestBody.getBytes());

        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            throw new IOException("Failed to create issue: " + responseCode);
        }
    }

    private void checkOrUpload(String repoName, String directoryPath, String fileName, String commitMessage, String localPath) throws IOException {
        String content = readResource(localPath);
        String finalContent = content.replace("REPO_NAME", repoName);
        String url = "https://api.github.com/repos/" + repoName + "/contents/" + directoryPath + "/" + fileName;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            // Datei nicht gefunden - hochladen
            uploadFile(repoName, content, commitMessage, directoryPath, fileName);
        } else if (responseCode == HttpURLConnection.HTTP_OK) {
            // Datei gefunden - überprüfen, ob ein Upload erforderlich ist
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                // Hier können Sie den Inhalt der Antwort analysieren, um festzustellen, ob der Inhalt geändert werden muss
                // Dies kann beispielsweise durch Vergleichen des Inhalts mit dem finalen Inhalt erfolgen
            }
        } else {
            throw new IOException("Failed to check file: " + responseCode);
        }
    }


    private void uploadFile(String repoName, String content, String commitMessage, String directoryPath, String fileName) throws IOException {
        String url = "https://api.github.com/repos/" + repoName + "/contents/" + directoryPath + "/" + fileName;
        String requestBody = "{\"message\":\"" + commitMessage + "\",\"content\":\"" + Base64.getEncoder().encodeToString(content.getBytes()) + "\"}";

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        con.getOutputStream().write(requestBody.getBytes());

        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            throw new IOException("Failed to upload file: " + responseCode);
        }
    }

    private void checkFileNotExistsOrMakeIssue(String repoName, String directory, String file, String issueTitle) throws IOException {
        String url = "https://api.github.com/repos/" + repoName + "/contents/" + directory;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                // Hier können Sie die Antwort analysieren, um festzustellen, ob die Datei vorhanden ist
                // Dies kann beispielsweise durch Vergleichen der Dateinamen erfolgen
                // Wenn die Datei nicht gefunden wird, Issue erstellen
                // Beispiel: makeIssue(repoName, issueTitle);
            }
        } else {
            throw new IOException("Failed to check directory: " + responseCode);
        }
    }

    private void checkDirectoryNotExistsOrMakeIssue(String repoName, String directory, String issueTitle) throws IOException {
        String url = "https://api.github.com/repos/" + repoName + "/contents/" + directory;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                // Hier können Sie die Antwort analysieren, um festzustellen, ob das Verzeichnis vorhanden ist
                // Dies kann beispielsweise durch Vergleichen der Verzeichnisnamen erfolgen
                // Wenn das Verzeichnis nicht gefunden wird, Issue erstellen
                // Beispiel: makeIssue(repoName, issueTitle);
            }
        } else {
            throw new IOException("Failed to check directory: " + responseCode);
        }
    }

    private void checkWebsite(String repoName) throws IOException {
        String url = "https://api.github.com/repos/" + repoName;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                // Hier können Sie die Antwort analysieren, um festzustellen, ob die Website festgelegt ist
                // Dies kann beispielsweise durch Überprüfen des Homepage-Felds erfolgen
                // Wenn die Website nicht festgelegt ist, setzen Sie sie und erstellen Sie gegebenenfalls ein Issue
            }
        } else {
            throw new IOException("Failed to check repository: " + responseCode);
        }
    }

    private void checkOrIssue(String repoName, String directoryPath, String fileName, String issueTitle) throws IOException {
        String url = "https://api.github.com/repos/" + repoName + "/contents/" + directoryPath + "/" + fileName;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            // Datei nicht gefunden - Issue erstellen
            makeIssue(repoName, issueTitle);
        } else if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to check file: " + responseCode);
        }
    }

    private void checkTopics(String repoName) throws IOException {
        String url = "https://api.github.com/repos/" + repoName + "/topics";
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + githubToken);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                // Hier können Sie die Antwort analysieren, um festzustellen, welche Themen festgelegt sind
                // Wenn Themen fehlen, fügen Sie sie hinzu und aktualisieren Sie die Themenliste
            }
        } else {
            throw new IOException("Failed to check topics: " + responseCode);
        }
    }


    private String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            }
        }
    }

}
