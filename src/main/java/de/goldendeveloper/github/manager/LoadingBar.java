package de.goldendeveloper.github.manager;

public class LoadingBar {

    private final int totalSteps;
    private int currentStep;

    public LoadingBar(int totalSteps) {
        this.totalSteps = totalSteps;
        this.currentStep = 0;
    }

    public void updateProgress() {
        currentStep++;
        printProgress(currentStep, totalSteps);
    }

    private void printProgress(int step, int totalSteps) {
        int width = 50;
        int progressMarker = (step * width) / totalSteps;
        System.out.print("\r[");
        for (int i = 0; i < width; i++) {
            if (i < progressMarker) {
                System.out.print("#");
            } else {
                System.out.print(" ");
            }
        }
        System.out.print("] " + (int) (((double) step / totalSteps) * 100) + "%");
    }

}
