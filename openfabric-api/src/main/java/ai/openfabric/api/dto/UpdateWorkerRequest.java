package ai.openfabric.api.dto;

public class UpdateWorkerRequest {
    private boolean stop;

    // Getter and Setter for 'start'

    public boolean isStopped() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }
}
