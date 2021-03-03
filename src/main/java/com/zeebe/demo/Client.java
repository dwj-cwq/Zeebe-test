package com.zeebe.demo;

import com.zeebe.demo.jobs.ExampleJobHandler;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.Topology;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.client.api.response.WorkflowInstanceResult;
import io.zeebe.client.api.worker.JobHandler;
import io.zeebe.client.api.worker.JobWorker;
import io.zeebe.model.bpmn.BpmnModelInstance;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * @author dwj
 * @date 2020/11/3 19:12
 */
public class Client {

    private static ZeebeClient client;

    private String remoteAddress = "127.0.0.1:26500";

    private Map<String, Object> testData = new HashMap<String, Object>() {{
        put("orderId", 31243);
        put("orderItems", new int[]{435, 182, 376});
    }};

    public void topologyViewer() {
        System.out.println("Requesting topology with initial contact point " + this.remoteAddress);

        final Topology topology = client.newTopologyRequest().send().join();

        System.out.println("Topology:");
        topology
            .getBrokers()
            .forEach(b -> {
                System.out.println("    " + b.getAddress());
                b.getPartitions()
                    .forEach(p ->
                        System.out.println(
                            "      " + p.getPartitionId() + " - " + p.getRole()));
            });

        System.out.println("Done.");
    }

    public void init() {
        client = getClient();
    }

    private ZeebeClient getClient() {
        return ZeebeClient.newClientBuilder()
            .gatewayAddress(this.remoteAddress)
            .usePlaintext()
            .build();
    }

    public void close() {
        client.close();
    }

    public void deployBPMNModel(String filename) {
        // after the client is connected

        final DeploymentEvent deployment = client.newDeployCommand()
            .addResourceFromClasspath(filename)
            .send()
            .join();

        final int version = deployment.getWorkflows().get(0).getVersion();
        System.out.println("Workflow deployed. Version: " + version);
    }

    public void deployBPMNModel(BpmnModelInstance bpmnModelInstance, String resourceName) {
        // after the client is connected

        final DeploymentEvent deployment = client.newDeployCommand()
            .addWorkflowModel(bpmnModelInstance, resourceName)
            .send()
            .join();

        final int version = deployment.getWorkflows().get(0).getVersion();
        System.out.println("Workflow deployed. Version: " + version);
    }

    public void createWorkflowInstance(String processId) {
        // after the workflow is deployed

        final WorkflowInstanceEvent wfInstance = client.newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .send()
            .join();

        final long workflowInstanceKey = wfInstance.getWorkflowInstanceKey();

        System.out.println("Workflow instance created. Key: " + workflowInstanceKey);

    }

    public void createWorkflowInstanceWithData(String processId) {
        // after the workflow is deployed

        final WorkflowInstanceEvent wfInstance = client.newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(testData)
            .send()
            .join();

        final long workflowInstanceKey = wfInstance.getWorkflowInstanceKey();

        System.out.println("Workflow instance created. Key: " + workflowInstanceKey);

    }

    public void createWorkflowInstanceWithResult(String processId) {
        final WorkflowInstanceResult result = client.newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(testData)
            .withResult()
            .send()
            .join();

        Map<String, Object> variablesAsMap = result.getVariablesAsMap();
        System.out.println("The result:" + variablesAsMap);
    }

    public void createJobWork(String jobType) {
        // after the workflow instance is created

        try (final JobWorker workerRegistration =
                 client.newWorker()
                     .jobType(jobType)
                     .handler(new ExampleJobHandler())
                     .timeout(Duration.ofSeconds(10))
                     .open()) {
            System.out.println("Job worker opened and receiving jobs.");

            // run until System.in receives exit command
            waitUntilSystemInput("exit");
        }
    }

    public void createJobWorkWithData(String jobType, JobHandler jobHandler) {
        final JobWorker jobWorker = client.newWorker()
            .jobType(jobType)
            .handler(jobHandler)
            .fetchVariables("orderId", "orderItems", "totalPrice", "end")
            .open();


    }

    public static void waitUntilSystemInput(final String exitCode) {
        try (final Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                final String nextLine = scanner.nextLine();
                if (nextLine.contains(exitCode)) {
                    return;
                }
            }
        }
    }

}
