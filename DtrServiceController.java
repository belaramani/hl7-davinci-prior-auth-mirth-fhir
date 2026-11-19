package medibridgepayermock.medibridgepayermock;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/fhir/Questionnaire")
public class DtrServiceController {

    // Da Vinci DTR Endpoint: GET /fhir/Questionnaire/lumbar-mri-pa
    @GetMapping(value = "/lumbar-mri-pa", produces = "application/fhir+json")
    public Map<String, Object> getLumbarMriQuestionnaire() {
        var q = new LinkedHashMap<String, Object>();
        q.put("resourceType", "Questionnaire");
        q.put("id", "lumbar-mri-pa");
        q.put("url", "http://localhost:8085/fhir/Questionnaire/lumbar-mri-pa");
        q.put("name", "LumbarSpineMriPriorAuthDTR");
        q.put("title", "Aetna Medical Necessity Questionnaire - MRI Lumbar Spine (CPT 72148)");
        q.put("status", "active");
        q.put("subjectType", List.of("Patient"));

        var item1 = new LinkedHashMap<String, Object>();
        item1.put("linkId", "1.1");
        item1.put("text", "Has the patient completed at least 6 weeks of conservative physical therapy?");
        item1.put("type", "boolean");
        item1.put("required", true);

        var item2 = new LinkedHashMap<String, Object>();
        item2.put("linkId", "1.2");
        item2.put("text", "Duration of persistent low back pain (in weeks)");
        item2.put("type", "integer");
        item2.put("required", true);

        var item3 = new LinkedHashMap<String, Object>();
        item3.put("linkId", "1.3");
        item3.put("text", "Clinical rationale / red flag neurological symptoms");
        item3.put("type", "string");
        item3.put("required", true);

        q.put("item", List.of(item1, item2, item3));
        return q;
    }
}
