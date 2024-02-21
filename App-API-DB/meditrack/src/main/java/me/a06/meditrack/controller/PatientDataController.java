package me.a06.meditrack.controller;

import me.a06.meditrack.service.PatientDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PatientDataController {

    @Autowired
    private PatientDataService patientDataService;

    @PostMapping("/read")
    public String readPatientData(@RequestBody String payload) {
        System.out.println("RECEIVED READ REQUEST!");
        System.out.println("PRINTING PAYLOAD");
        System.out.println(payload);
        return patientDataService.readPatientData(payload);
    }

    @PostMapping("/register")
    public String registerConsultation(@RequestBody String payload) {
        System.out.println("RECEIVED REGISTER REQUEST!");
        System.out.println("PRINTING PAYLOAD");
        System.out.println(payload);
        return patientDataService.registerConsultation(payload);
    }

    @PutMapping("/change")
    public String changePermission(@RequestBody String payload) {
        System.out.println("RECEIVED CHANGE REQUEST!");
        System.out.println("PRINTING PAYLOAD");
        System.out.println(payload);
        return patientDataService.changePermission(payload);
    }

}
