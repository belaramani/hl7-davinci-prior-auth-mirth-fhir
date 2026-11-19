package medibridgepayermock.medibridgepayermock;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/cds-services")
public class CrdServiceController {

    // 1. Standard CDS Hooks Discovery Endpoint
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> discoverServices() {
        Map<String, Object> service = Map.of(
                "hook", "order-sign",
                "title", "Aetna Da Vinci CRD Prior Auth Check",
                "description", "Checks Prior Authorization and documentation requirements in real time.",
                "id", "prior-auth-crd",
                "prefetch", Map.of(
                        "patientToGreet", "Patient/{{context.patientId}}"
                )
        );
        return Map.of("services", List.of(service));
    }

    // 2. Da Vinci CRD Hook Invocation Endpoint
    @PostMapping(value = "/prior-auth-crd", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> evaluateOrderSign(@RequestBody Map<String, Object> request) {
        String cptCode = extractCptCode(request);

        // Rule: High-cost imaging (CPT 72148 - MRI Lumbar Spine) requires Prior Auth
        boolean paRequired = "72148".equals(cptCode);

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("uuid", UUID.randomUUID().toString());
        card.put("summary", paRequired
                ? "Prior Authorization Required for CPT " + cptCode + " (MRI Lumbar Spine)"
                : "No Prior Authorization Required for CPT " + cptCode);
        card.put("indicator", paRequired ? "warning" : "info");
        card.put("detail", paRequired
                ? "Coverage policy requires clinical documentation of >= 6 weeks conservative therapy. Launch DTR or submit via Da Vinci PAS."
                : "Procedure is covered under member's active Gold Plan without prior authorization.");
        card.put("source", Map.of(
                "label", "Aetna Da Vinci CRD Engine",
                "url", "https://payerhub.org/crd-rules"
        ));

        if (paRequired) {
            card.put("links", List.of(Map.of(
                    "label", "Launch DTR Questionnaire & Submit PAS",
                    "url", "http://localhost:8085/dtr/launch?cpt=" + cptCode,
                    "type", "smart"
            )));
        }

        // Da Vinci CRD Coverage-Information Extension / System Action (STU 2.2)
        Map<String, Object> systemAction = Map.of(
                "type", "update",
                "description", "Attach Da Vinci CRD coverage-information",
                "coverageInfo", Map.of(
                        "covered", "covered",
                        "pa-needed", paRequired ? "auth-needed" : "no-auth",
                        "doc-needed", paRequired ? "clinical" : "no-doc",
                        "date", Instant.now().toString()
                )
        );

        return Map.of(
                "cards", List.of(card),
                "systemActions", List.of(systemAction)
        );
    }

    @SuppressWarnings("unchecked")
    private String extractCptCode(Map<String, Object> request) {
        try {
            Map<String, Object> context = (Map<String, Object>) request.get("context");
            Map<String, Object> draftOrders = (Map<String, Object>) context.get("draftOrders");
            List<?> entries = (List<?>) draftOrders.get("entry");
            Map<String, Object> firstEntry = (Map<String, Object>) entries.get(0);
            Map<String, Object> resource = (Map<String, Object>) firstEntry.get("resource");
            Map<String, Object> code = (Map<String, Object>) resource.get("code");
            List<?> coding = (List<?>) code.get("coding");
            Map<String, Object> firstCoding = (Map<String, Object>) coding.get(0);
            return String.valueOf(firstCoding.get("code"));
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
