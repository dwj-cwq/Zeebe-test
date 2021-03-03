package com.zeebe.demo;

import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;

/**
 * @author Chen Wenqun
 */
public class ModelFactory {

    public BpmnModelInstance getModelInstance() {
        return Bpmn.createExecutableProcess("order-process")
            .startEvent("order-placed").name("order placed")
            .serviceTask("collect-money",
                b -> {
                    b.zeebeJobType("collect-money").zeebeTaskHeader("method", "VISA");
                }).name("collect money")
            .serviceTask("payment-service", b -> {
                b.zeebeJobType("payment-service").zeebeTaskHeader("method", "VISA");
            }).name("payment service")
            .serviceTask("fetch-item", b -> {
                b.zeebeJobType("payment-service").zeebeTaskHeader("method", "VISA");
            }).name("fetch item")
            .serviceTask("ship-parcel", b -> {
                b.zeebeJobType("payment-service").zeebeTaskHeader("method", "VISA");
            }).name("ship parcel")
            .endEvent("order-delivered").name("order delivered")
            .done();
    }
}
