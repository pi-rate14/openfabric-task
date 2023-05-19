package ai.openfabric.api;

import ai.openfabric.api.service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@EnableTransactionManagement
public class Application {
    private final WorkerService workerService;

    @Autowired
    Application(WorkerService workerService) {
        this.workerService = workerService;
    }

    public static void main(String[] args) {

       SpringApplication.run(Application.class, args);

    }

    @EventListener(ContextRefreshedEvent.class)
    public void saveNewContainersToDatabase() {
        workerService.saveWorkersToDatabase();
    }

}
