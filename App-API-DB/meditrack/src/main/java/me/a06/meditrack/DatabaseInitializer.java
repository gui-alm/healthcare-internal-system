package me.a06.meditrack;

import com.google.gson.JsonArray;
import me.a06.meditrack.securedocument.Protect;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import me.a06.meditrack.repository.PatientDataRepository;
import me.a06.meditrack.domain.PatientData;
import com.google.gson.JsonObject;

@Component
@ConfigurationProperties(prefix = "app.database-initialization")
@PropertySource("classpath:application.properties")
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private PatientDataRepository patientDataRepository;

    private boolean enabled; // Property to control initialization

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) throws Exception {
        if(!enabled) {
            return;
        }

        // Trevor
        JsonArray knownAllergies = new JsonArray();
        knownAllergies.add("Penisilin");
        addPatient("trevor", "male", "1997-03-05", "A", knownAllergies);

        // Jack
        knownAllergies = new JsonArray();
        knownAllergies.add("Peanuts");
        knownAllergies.add("Shellfish");
        addPatient("jack", "male", "1960-02-16", "O-", knownAllergies);

        // Nina
        knownAllergies = new JsonArray();
        knownAllergies.add("gluten");
        addPatient("nina", "female", "2002-10-17", "AB", knownAllergies);

        // Repeat for other patient records
    }

    private void addPatient(String name, String sex, String dateOfBirth,
                            String bloodType, JsonArray knownAllergies) {
        JsonObject contents = new JsonObject();
        contents.addProperty("name", name);
        contents.addProperty("sex", sex);
        contents.addProperty("dateOfBirth", dateOfBirth);
        contents.addProperty("bloodType", bloodType);
        contents.add("knownAllergies", knownAllergies);

        JsonObject generalData = new JsonObject();
        generalData.add("contents", contents);
        generalData.add("metadata", new JsonArray());

        JsonObject jsonDoc = new JsonObject();
        jsonDoc.add("General Data", generalData);

        PatientData patientData = new PatientData(name, "");

        try {
            Protect.encryptJSON(jsonDoc, name, name);
        } catch (Exception e) {
            System.out.println("ERROR IN POPULATE");
            e.printStackTrace();
            return;
        }
        patientData.setJsondocument(jsonDoc.toString());

        patientDataRepository.save(patientData);
    }
}
