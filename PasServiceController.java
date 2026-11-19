
package medibridgepayermock.medibridgepayermock;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/fhir/Claim")
@SuppressWarnings({"rawtypes", "unchecked"})
public class PasServiceController {

    private static final List RECENT_AUTHS = Collections.synchronizedList(new ArrayList());

    // Da Vinci PAS Operation: POST /fhir/Claim/$submit
    @PostMapping(
            value = "/$submit",
            consumes = {"application/fhir+json", MediaType.APPLICATION_JSON_VALUE},
            produces = "application/fhir+json"
    )
    public Map<String, Object> submitPriorAuth(@RequestBody Map<String, Object> requestBundle) {
        String claimId = extractClaimId(requestBundle);
        String patientRef = extractPatientRef(requestBundle);
        String cptCode = extractCptFromClaim(requestBundle);
        String icdCode = extractIcdFromClaim(requestBundle);
        boolean hasDtrForm = hasCompletedQuestionnaireResponse(requestBundle);

        boolean approved = "72148".equals(cptCode) && "M54.50".equalsIgnoreCase(icdCode) && hasDtrForm;
        String authNumber = approved ? "PA-" + (100000 + new Random().nextInt(900000)) : null;

        var claimResponse = new LinkedHashMap<String, Object>();
        claimResponse.put("resourceType", "ClaimResponse");
        claimResponse.put("id", "pas-resp-" + UUID.randomUUID().toString().substring(0, 8));
        claimResponse.put("meta", Map.of(
                "profile", List.of("http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-claimresponse")
        ));
        claimResponse.put("status", "active");
        claimResponse.put("type", Map.of(
                "coding", List.of(Map.of(
                        "system", "http://terminology.hl7.org/CodeSystem/claim-type",
                        "code", "professional"
                ))
        ));
        claimResponse.put("use", "preauthorization");
        claimResponse.put("patient", Map.of("reference", patientRef));
        claimResponse.put("created", Instant.now().toString());
        claimResponse.put("insurer", Map.of("display", "Aetna Health Plan"));
        claimResponse.put("request", Map.of("reference", "Claim/" + claimId));
        claimResponse.put("outcome", approved ? "complete" : "queued");
        claimResponse.put("disposition", approved
                ? "Certified in total - Approved via Da Vinci PAS (DTR Verified)"
                : "Pended - Missing completed DTR QuestionnaireResponse or clinical criteria");

        var itemExtensions = new ArrayList<Object>();
        itemExtensions.add(Map.of(
                "url", "http://hl7.org/fhir/us/davinci-pas/StructureDefinition/extension-reviewAction",
                "extension", List.of(Map.of(
                        "url", "code",
                        "valueCodeableConcept", Map.of(
                                "coding", List.of(Map.of(
                                        "system", "https://codesystem.x12.org/005010/306",
                                        "code", approved ? "A1" : "A4",
                                        "display", approved ? "Certified in total" : "Pended"
                                ))
                        )
                ))
        ));

        if (approved) {
            itemExtensions.add(Map.of(
                    "url", "http://hl7.org/fhir/us/davinci-pas/StructureDefinition/extension-authorizationNumber",
                    "valueString", authNumber
            ));
            itemExtensions.add(Map.of(
                    "url", "http://hl7.org/fhir/us/davinci-pas/StructureDefinition/extension-itemPreAuthPeriod",
                    "valuePeriod", Map.of(
                            "start", LocalDate.now().toString(),
                            "end", LocalDate.now().plusDays(90).toString()
                    )
            ));
        }

        claimResponse.put("item", List.of(Map.of(
                "itemSequence", 1,
                "extension", itemExtensions
        )));

        var responseBundle = new LinkedHashMap<String, Object>();
        responseBundle.put("resourceType", "Bundle");
        responseBundle.put("id", UUID.randomUUID().toString());
        responseBundle.put("meta", Map.of(
                "profile", List.of("http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-pas-response-bundle")
        ));
        responseBundle.put("type", "collection");
        responseBundle.put("timestamp", Instant.now().toString());
        responseBundle.put("entry", List.of(Map.of(
                "fullUrl", "urn:uuid:" + claimResponse.get("id"),
                "resource", claimResponse
        )));

        var record = new LinkedHashMap<String, Object>();
        record.put("timestamp", Instant.now().toString());
        record.put("claimId", claimId);
        record.put("patientRef", patientRef);
        record.put("cptCode", cptCode);
        record.put("icdCode", icdCode);
        record.put("dtrVerified", hasDtrForm);
        record.put("disposition", claimResponse.get("disposition"));
        record.put("authNumber", approved ? authNumber : "PENDED");
        RECENT_AUTHS.add(0, record);

        return responseBundle;
    }

    // Live UI Feed Endpoint: GET /fhir/Claim/feed
    @GetMapping(value = "/feed", produces = MediaType.APPLICATION_JSON_VALUE)
    public List getRecentAuthorizations() {
        return RECENT_AUTHS;
    }

    private boolean hasCompletedQuestionnaireResponse(Map<String, Object> bundle) {
        try {
            var entries = (List) bundle.get("entry");
            for (Object obj : entries) {
                var entry = (Map<String, Object>) obj;
                var res = (Map<String, Object>) entry.get("resource");
                if ("QuestionnaireResponse".equals(res.get("resourceType"))
                        && "completed".equals(res.get("status"))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractClaimId(Map<String, Object> bundle) {
        try {
            var entries = (List) bundle.get("entry");
            var firstEntry = (Map<String, Object>) entries.get(0);
            var firstRes = (Map<String, Object>) firstEntry.get("resource");
            return String.valueOf(firstRes.getOrDefault("id", "claim-1"));
        } catch (Exception e) {
            return "claim-1";
        }
    }

    private String extractPatientRef(Map<String, Object> bundle) {
        try {
            var entries = (List) bundle.get("entry");
            var firstEntry = (Map<String, Object>) entries.get(0);
            var firstRes = (Map<String, Object>) firstEntry.get("resource");
            var patient = (Map<String, Object>) firstRes.get("patient");
            return String.valueOf(patient.get("reference"));
        } catch (Exception e) {
            return "Patient/unknown";
        }
    }

    private String extractCptFromClaim(Map<String, Object> bundle) {
        try {
            var entries = (List) bundle.get("entry");
            var firstEntry = (Map<String, Object>) entries.get(0);
            var firstRes = (Map<String, Object>) firstEntry.get("resource");
            var items = (List) firstRes.get("item");
            var firstItem = (Map<String, Object>) items.get(0);
            var prod = (Map<String, Object>) firstItem.get("productOrService");
            var coding = (List) prod.get("coding");
            var firstCoding = (Map<String, Object>) coding.get(0);
            return String.valueOf(firstCoding.get("code"));
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String extractIcdFromClaim(Map<String, Object> bundle) {
        try {
            var entries = (List) bundle.get("entry");
            var firstEntry = (Map<String, Object>) entries.get(0);
            var firstRes = (Map<String, Object>) firstEntry.get("resource");
            var diagnoses = (List) firstRes.get("diagnosis");
            var firstDiag = (Map<String, Object>) diagnoses.get(0);
            var diagCodeable = (Map<String, Object>) firstDiag.get("diagnosisCodeableConcept");
            var coding = (List) diagCodeable.get("coding");
            var firstCoding = (Map<String, Object>) coding.get(0);
            return String.valueOf(firstCoding.get("code"));
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}