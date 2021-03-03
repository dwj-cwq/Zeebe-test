package com.zeebe.demo.jobs;

import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.client.api.worker.JobHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chen Wenqun
 */
public class ExampleJobHandler implements JobHandler {

    @Override
    public void handle(final JobClient client, final ActivatedJob job) {
        // here: business logic that is executed with every job
        final Map<String, Object> variables = job.getVariablesAsMap();

        System.out.println("Process order: " + variables.get("orderId"));
        double price = 46.50;
        System.out.println("Collect money: $" + price);


        final Map<String, Object> result = new HashMap<>(2);
        result.put("totalPrice", price);

        client.newCompleteCommand(job.getKey())
            .variables(result)
            .send()
            .join();
    }

}
