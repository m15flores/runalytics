package com.runalitycs.activity.controller;

import com.runalitycs.activity.kafka.ActivityProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    private final ActivityProducer producer;

    public ActivityController(ActivityProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    public String ingest(@RequestBody String rawJson) {
        producer.publishActivity(rawJson);
        return "OK";
    }
}
