
# HL7 Da Vinci Prior Authorization Engine (CRD, DTR, PAS)

End-to-end **CMS-0057-F Interoperability & Prior Authorization** prototype bridging legacy **HL7 v2 (`ORM^O01`)** clinical orders with **HL7 FHIR R4** and the **Da Vinci Burden Reduction Implementation Guides (CRD, DTR, PAS)** using **Mirth Connect 4.5.2**, **Java Spring Boot 3**, **HAPI FHIR R4**, and **MS SQL Server**.

---

## Architecture & End-to-End Flow

```text
[Hospital EHR: HL7 v2 ORM^O01]
        │
        ▼ (MLLP / TCP Port 6661)
[Mirth Channel 1: 01_HL7_ORM_to_FHIR]
        ├──► POST Atomic FHIR Transaction Bundle ──► [HAPI FHIR R4 Server]
        │    (Patient, Practitioner, Coverage, Condition, ServiceRequest)
        ▼
[Mirth Channel 2: 02_DaVinci_CRD_Hook]
        ├──► POST CDS Hooks (order-sign) ─────────► [Spring Boot Payer Hub :8085]
        │    Returns Coverage Information ("pa-needed": "auth-needed")
        ▼
[Mirth Channel 3: 03_DaVinci_PAS_Submit]
        ├──► Attaches Completed DTR QuestionnaireResponse
        ├──► POST /fhir/Claim/$submit ────────────► [Spring Boot Payer Hub :8085]
        │    Adjudicates Medical Necessity (X12 306: A1 Certified / A4 Pended)
        ├──► INSERT Audit Trail ──────────────────► [MS SQL Server: PriorAuthAuditLog]
        └──► Live Dashboard Feed ─────────────────► http://localhost:8085/index.html
