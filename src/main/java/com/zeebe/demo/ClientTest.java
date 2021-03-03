package com.zeebe.demo;

import com.zeebe.demo.jobs.ExampleJobHandler;
import com.zeebe.demo.jobs.ExampleJobHandler1;
import com.zeebe.demo.jobs.ExampleJobHandler2;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;

import java.io.File;

import static com.zeebe.demo.Client.waitUntilSystemInput;

/**
 * @author Chen Wenqun
 */
public class ClientTest {

    public static void main(String[] args) {
        test3();
    }

    public static void test() {
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

    public static void test1() {
        final ModelFactory modelFactory = new ModelFactory();
        final BpmnModelInstance modelInstance = modelFactory.getModelInstance();

        // 初始化客户端，连接broker
        Client client = new Client();
        client.init();

        // 部署业务工作流
        client.deployBPMNModel(modelInstance, "order-process.bpmn");

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

    public static void test3() {
        final ModelFactory modelFactory = new ModelFactory();
        final BpmnModelInstance modelInstance = modelFactory.getModelInstance();
        String path = "/Users/dwj/Sourcetree/myProject/Zeebe-test/src/main/resources/order-process-test.bpmn";
        Bpmn.writeModelToFile(new File(path), modelInstance);
    }
}
