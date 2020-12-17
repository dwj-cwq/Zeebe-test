package com.zeebe.demo;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.*;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.client.api.worker.JobHandler;
import io.zeebe.client.api.worker.JobWorker;

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

    private Map<String, Object> testData = new HashMap<String, Object>(){{
        put("orderId", 31243);
        put("orderItems", new int[]{435, 182, 376});
    }};

    public static void main(String[] args) {
        // 初始化客户端，连接broker
        Client client = new Client();
        client.init();

        // 部署业务工作流
        client.deployBPMNModel("order-process.bpmn");

        // 创建工作流实例
        client.createWorkflowInstanceWithData("order-process");

        // 创建 job worker
        client.createJobWorkWithData("payment-service", new ExampleJobHandler());
        client.createJobWorkWithData("inventory-service", new ExampleJobHandler1());
        client.createJobWorkWithData("shipment-service", new ExampleJobHandler2());

        for (int i = 0; i < 10; i++) {
            client.createWorkflowInstanceWithData("order-process");
            client.createWorkflowInstanceWithResult("order-process");
        }

        // 打印集群拓扑
        client.topologyViewer();

        waitUntilSystemInput("e");

        // 关闭客户端
        client.close();
    }

    private void topologyViewer() {
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

    private void init() {
        client = getClient();
    }

    private ZeebeClient getClient() {
        return ZeebeClient.newClientBuilder()
                .gatewayAddress(this.remoteAddress)
                .usePlaintext()
                .build();
    }

    private void close() {
        client.close();
    }

    private void deployBPMNModel(String filename) {
        // after the client is connected

        final DeploymentEvent deployment = client.newDeployCommand()
                .addResourceFromClasspath(filename)
                .send()
                .join();

        final int version = deployment.getWorkflows().get(0).getVersion();
        System.out.println("Workflow deployed. Version: " + version);
    }

    private void createWorkflowInstance(String processId) {
        // after the workflow is deployed

        final WorkflowInstanceEvent wfInstance = client.newCreateInstanceCommand()
                .bpmnProcessId(processId)
                .latestVersion()
                .send()
                .join();

        final long workflowInstanceKey = wfInstance.getWorkflowInstanceKey();

        System.out.println("Workflow instance created. Key: " + workflowInstanceKey);

    }

    private void createWorkflowInstanceWithData(String processId) {
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

    private void createWorkflowInstanceWithResult(String processId) {
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

    private void createJobWork(String jobType) {
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

    private void createJobWorkWithData(String jobType, JobHandler jobHandler) {
        final JobWorker jobWorker = client.newWorker()
                .jobType(jobType)
                .handler(jobHandler)
                .fetchVariables("orderId", "orderItems", "totalPrice", "end")
                .open();


    }

    private static void waitUntilSystemInput(final String exitCode) {
        try (final Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                final String nextLine = scanner.nextLine();
                if (nextLine.contains(exitCode)) {
                    return;
                }
            }
        }
    }

    private static class ExampleJobHandler implements JobHandler {

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

    private static class ExampleJobHandler1 implements JobHandler {

        @Override
        public void handle(final JobClient client, final ActivatedJob job) {
            // here: business logic that is executed with every job
            final Map<String, Object> variables = job.getVariablesAsMap();

            System.out.println(job);

            final Map<String, Object> result = new HashMap<>(2);
            result.put("statue", "OK");

            client.newCompleteCommand(job.getKey())
                    .variables(result)
                    .send()
                    .join();
        }
    }

    private static class ExampleJobHandler2 implements JobHandler {

        @Override
        public void handle(final JobClient client, final ActivatedJob job) {
            // here: business logic that is executed with every job
            final Map<String, Object> variables = job.getVariablesAsMap();

            System.out.println(job);

            final Map<String, Object> result = new HashMap<>(2);
            result.put("end", "OK");

            client.newCompleteCommand(job.getKey())
                    .variables(result)
                    .send()
                    .join();
        }
    }

}
