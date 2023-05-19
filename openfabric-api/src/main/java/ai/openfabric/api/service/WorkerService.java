package ai.openfabric.api.service;

import ai.openfabric.api.model.Worker;
import ai.openfabric.api.repository.WorkerRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Service
public class WorkerService {
    private DockerClient dockerClient;

    private final WorkerRepository workerRepository;

    @Autowired
    public WorkerService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

       this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    @Transactional
    public void saveWorkersToDatabase() {
        // get all running containers
        List<Container> containers = this.dockerClient.listContainersCmd().exec();
        List<Worker> workers = new ArrayList<>();
        for (Container container : containers) {
            Worker worker = new Worker();
            worker.setContainerId(container.getId());
            worker.setName(container.getNames()[0]);
            workers.add(worker);
        }

        // Filter out workers with existing container IDs
        List<Worker> newWorkers = workers.stream()
                .filter(worker -> !workerExists(worker.getContainerId()))
                .collect(Collectors.toList());

        // Save new workers to the database
        workerRepository.saveAll(newWorkers);
    }

    private boolean workerExists(String containerId) {
        return workerRepository.findByContainerId(containerId).isPresent();
    }

    private Container getContainerByWorkerID(String workerID) {
        String containerID;
        Optional<Worker> worker = workerRepository.findById(workerID);
        if (worker.isPresent()) {
            containerID = worker.get().getContainerId();
        } else {
            containerID = "";
            return null;
        }
        List<Container> containers = this.dockerClient.listContainersCmd().exec();

        Optional<Container> optionalContainer = containers.stream()
                .filter(container -> container.getId().equals(containerID))
                .findFirst();

        return optionalContainer.orElse(null);
    }

    public Worker getWorkerInfo(String workerID) {
        Container container = getContainerByWorkerID(workerID);
        Optional<Worker> worker = workerRepository.findById(workerID);
        if (container != null) {
            worker.ifPresent(value -> value.setContainer(container));
        }
        return worker.orElse(null);
    }

    public String getWorkers(Pageable pageable) throws JsonProcessingException {

        List<Worker> workers = workerRepository.findAll(pageable);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        String json = objectMapper.writeValueAsString(workers);
        return json;
    }

    public Boolean updateWorkerState(String workerID, Boolean stop) throws Exception {
        Container container = getContainerByWorkerID(workerID);
        Worker worker = workerRepository.findById(workerID).orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        if(container == null) {
            throw new NotFoundException("Worker with ID not found");
        }
        Boolean containerRunning = container.getState().equals("running");

        if(stop && containerRunning) {
            this.dockerClient.pauseContainerCmd(container.getId()).exec();
            worker.updatedAt = new Date();
            workerRepository.save(worker);
        } else if(!stop && !containerRunning) {
            this.dockerClient.unpauseContainerCmd(container.getId()).exec();
            worker.updatedAt = new Date();
            workerRepository.save(worker);
        } else {
            return false;
        }

        return true;
    }

    public Statistics getStats(String workerID) throws Exception{
        Container container = getContainerByWorkerID(workerID);
        if (container == null) {
            throw new NotFoundException("Container not found");
        }
        final Statistics[] stats = {new Statistics()};
        CountDownLatch latch = new CountDownLatch(1);
        ResultCallback<Statistics> callback = new ResultCallback<Statistics>() {

            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onNext(Statistics object) {
                stats[0] = object;
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }

            @Override
            public void close() throws IOException {

            }
        };
        this.dockerClient.statsCmd(container.getId()).exec(callback);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return stats[0];
    }


}
