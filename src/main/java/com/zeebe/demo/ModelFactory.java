package com.zeebe.demo;

import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;

/**
 * @author Chen Wenqun
 */
public class ModelFactory {

    public BpmnModelInstance getModelInstance() {
        return Bpmn.createExecutableProcess("order process")
            .startEvent("order-placed").name("order placed")
            .serviceTask("collect-money").name("collect money")
            .serviceTask("payment-service").name("payment service")
            .serviceTask("fetch-item").name("fetch item")
            .serviceTask("ship-parcel").name("ship parcel")
            .endEvent("order-delivered").name("order delivered")
            .done();
    }
}
