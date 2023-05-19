package ai.openfabric.api.controller;

import ai.openfabric.api.dto.UpdateWorkerRequest;
import ai.openfabric.api.model.Worker;
import ai.openfabric.api.service.WorkerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dockerjava.api.model.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${node.api.path}/worker")
public class WorkerController {

    private final WorkerService workerService;

    @Autowired
    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PostMapping(path = "/hello")
    public @ResponseBody String hello(@RequestBody String name) {
        return "Hello!" + name;
    }

    @GetMapping(produces = "application/json")
    public String getWorkers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1") int size) throws JsonProcessingException {
            Pageable pageable = PageRequest.of(page, size);

            return workerService.getWorkers(pageable);
    }

    @GetMapping(value = "/{workerID}", produces = "application/json")
    public ResponseEntity<Worker> getWorker(@PathVariable String workerID) {
        // Logic to retrieve the worker based on the workerID
        Worker worker = workerService.getWorkerInfo(workerID);

        if (worker != null) {
            // Worker found
            return ResponseEntity.ok(worker);
        } else {
            // Worker not found
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/changeState/{workerID}" , produces = "application/json")
    public ResponseEntity<String> updateWorker(@PathVariable String workerID, @RequestBody UpdateWorkerRequest request) {

        try {
            if (workerService.updateWorkerState(workerID, request.isStopped())) {
                return ResponseEntity.ok("Worker updated successfully.");
            } else {
                // Worker not found
                return ResponseEntity.ok("Worker already in desired state");
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/stats/{workerID}" , produces = "application/json")
    public ResponseEntity<Statistics> getWorkerStats(@PathVariable String workerID) throws InterruptedException {
        // Logic to retrieve the worker based on the workerID
        try {
            Statistics statistics = workerService.getStats(workerID);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

}
