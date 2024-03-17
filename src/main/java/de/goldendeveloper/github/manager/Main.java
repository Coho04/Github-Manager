package de.goldendeveloper.github.manager;

import de.goldendeveloper.github.manager.console.ConsoleReader;
import io.sentry.ITransaction;
import io.sentry.SpanStatus;
import io.sentry.Sentry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static Config config;

    public static void main(String[] args) {
        config = new Config();
        Sentry.init(options -> {
            options.setDsn(config.getSentryDns());
            options.setTracesSampleRate(1.0);
        });

        ITransaction transaction = Sentry.startTransaction("startProcess()", "task");
        try {
            startProcess();
        } catch (Exception e) {
            transaction.setThrowable(e);
            transaction.setStatus(SpanStatus.INTERNAL_ERROR);
            Sentry.captureException(e);
            throw e;
        } finally {
            transaction.finish();
        }
    }

    private static void startProcess() {
        String device = System.getProperty("os.name").split(" ")[0];
        if (!device.equalsIgnoreCase("windows") && !device.equalsIgnoreCase("Mac")) {
            new Discord();
        }
        new ConsoleReader();
        System.out.println("Starting daily housekeeping daemon");
        Calendar timeOfDay = Calendar.getInstance();
        timeOfDay.set(Calendar.HOUR_OF_DAY, 12);
        timeOfDay.set(Calendar.MINUTE, 0);
        timeOfDay.set(Calendar.SECOND, 0);
        new DailyRunnerDaemon(timeOfDay, Main::runRepositoryProcessor, "daily-housekeeping").start();
    }

    public static void runRepositoryProcessor() {
        try {
            String githubToken = config.getGithubToken();
            String githubUsername = config.getGithubUsername();
            String orgName = "Golden-Developer";
            String orgUrl = "https://api.github.com/orgs/" + orgName;
            String orgReposUrl = orgUrl + "/repos";
            String authHeader = "Bearer " + githubToken;
            String orgResponse = sendGetRequest(orgUrl, authHeader);
            if (orgResponse != null) {
                String orgId = JsonPath.read(orgResponse, "$.id").toString();
                String totalRepos = JsonPath.read(orgResponse, "$.public_repos").toString();

                LoadingBar loadingBar = new LoadingBar(Integer.parseInt(totalRepos));

                String reposResponse = sendGetRequest(orgReposUrl, authHeader);
                if (reposResponse != null) {
                    List<String> repoIds = JsonPath.read(reposResponse, "$.[*].id");
                    for (String repoId : repoIds) {
                        String repoUrl = "https://api.github.com/repositories/" + repoId;
                        String repoResponse = sendGetRequest(repoUrl, authHeader);
                        if (repoResponse != null) {
                            processRepository(repoResponse);
                            loadingBar.updateProgress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("An error occurred performing daily housekeeping");
            System.out.println("ErrorMessage: " + e.getMessage());
            Sentry.captureException(e);
        }
    }

    private static String sendGetRequest(String url, String authHeader) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", authHeader);
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                return in.lines().collect(Collectors.joining());
            }
        } else {
            System.out.println("Failed to send GET request to: " + url);
            System.out.println("Response Code: " + responseCode);
            return null;
        }
    }

    public static Config getConfig() {
        return config;
    }
}
