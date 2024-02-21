package me.a06.meditrack.domain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "patients")
public class PatientData {

    @Id
    private String name;

    @Lob
    private String jsondocument;

    public PatientData() { }

    public PatientData(String name, String jsondocument) {
        // TODO
        this.name = name;
        this.jsondocument = jsondocument;
    }

    // TODO

    public JsonObject getDocument() {
        return JsonParser.parseString(this.jsondocument).getAsJsonObject();
    }

    public void setJsondocument(String jsondocument) {
        this.jsondocument = jsondocument;
    }


}
