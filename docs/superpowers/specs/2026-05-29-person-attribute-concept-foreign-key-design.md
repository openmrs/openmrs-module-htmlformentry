# Design: PersonAttribute Concept Widget — foreignKey Support

**Date:** 2026-05-29
**File:** `api/src/main/java/org/openmrs/module/htmlformentry/element/PersonAttributeSubmissionElement.java`

## Context

`PersonAttributeSubmissionElement.buildConceptWidget()` currently requires an explicit `answerConceptIds` tag attribute listing the valid concept IDs. However, `PersonAttributeType` has a `foreignKey` (Integer) field that—for concept-typed attributes—can reference either a concept set (whose members are the valid answers) or a question concept (whose concept answers are the valid choices). Using `foreignKey` avoids duplicating answer lists between the attribute type definition and every form that uses it.

## Behavior Change

Priority order for populating concept dropdown options:

1. **`answerConceptIds` tag attribute present** → use those IDs exactly (existing behavior, no change).
2. **`attributeType.getForeignKey()` not null** → look up that concept, then:
   - If `concept.getSet()` is `true` → use `concept.getSetMembers(false)` (excludes retired members).
   - Else → iterate `concept.getAnswers(false)` and use each `ConceptAnswer.getAnswerConcept()` (excludes retired answer concepts).
3. **Neither present** → throw `BadFormDesignException` with a message explaining that one of `answerConceptIds` or a `foreignKey` on the attribute type is required.

The blank "choose..." `Option()` is always prepended regardless of source, consistent with the current implementation.

`answerConceptIds` remains optional — no form that currently specifies it needs to change.

## Implementation

### `buildConceptWidget` in `PersonAttributeSubmissionElement`

Replace the current guard that throws when `answerConceptIds` is absent with the three-way resolution above. Extract the concept-list-to-options loop into a private helper to avoid duplication between the `answerConceptIds` branch and the `foreignKey` branch.

Rough structure:

```java
private Widget buildConceptWidget(FormEntryContext context, Map<String, String> parameters)
        throws BadFormDesignException {

    String answerConceptIds = parameters.get("answerConceptIds");
    List<Concept> concepts;

    if (StringUtils.hasText(answerConceptIds)) {
        concepts = resolveConceptsFromIdList(answerConceptIds);           // existing logic
    } else if (attributeType.getForeignKey() != null) {
        concepts = resolveConceptsFromForeignKey(attributeType.getForeignKey());
    } else {
        throw new BadFormDesignException("...");
    }

    DropdownWidget w = new DropdownWidget();
    w.addOption(new Option());
    for (Concept concept : concepts) {
        w.addOption(new Option(concept.getName().getName(), concept.getConceptId().toString(), false));
    }
    if (existingAttribute != null && StringUtils.hasText(existingAttribute.getValue())) {
        w.setInitialValue(existingAttribute.getValue());
    }
    return w;
}

private List<Concept> resolveConceptsFromIdList(String answerConceptIds) throws BadFormDesignException { ... }

private List<Concept> resolveConceptsFromForeignKey(Integer foreignKey) throws BadFormDesignException {
    Concept fk = Context.getConceptService().getConcept(foreignKey);
    if (fk == null) {
        throw new BadFormDesignException("...");
    }
    if (Boolean.TRUE.equals(fk.getSet())) {
        return fk.getSetMembers(false);
    } else {
        return fk.getAnswers(false).stream()
                 .map(ConceptAnswer::getAnswerConcept)
                 .collect(Collectors.toList());
    }
}
```

No changes needed outside `buildConceptWidget` — `handleSubmission`, `toStorageValue`, and the rest of the element are unaffected.

## Test Data

New rows added to `RegressionTest-data-openmrs-2.8.xml`:

```xml
<!-- PersonAttributeType 20: Concept-type with foreignKey → concept set 1004 -->
<person_attribute_type person_attribute_type_id="20"
    name="Test Concept Set Attribute"
    format="org.openmrs.Concept"
    foreign_key="1004"
    searchable="false" creator="1" date_created="2024-01-01 00:00:00.0"
    retired="false" uuid="b1e61b61-5f1a-4a19-8a4c-test00000020" sort_weight="20"/>

<!-- PersonAttributeType 21: Concept-type with foreignKey → question concept 1000 -->
<person_attribute_type person_attribute_type_id="21"
    name="Test Concept Answer Attribute"
    format="org.openmrs.Concept"
    foreign_key="1000"
    searchable="false" creator="1" date_created="2024-01-01 00:00:00.0"
    retired="false" uuid="c2e61b61-5f1a-4a19-8a4c-test00000021" sort_weight="21"/>
```

Existing fixture data used by the tests:

| Concept ID | Name | Role |
|---|---|---|
| 1004 | ANOTHER ALLERGY CONSTRUCT | concept set (foreign key for type 20) |
| 80000 | ALLERGY | set member of 1004 |
| 1119 | ALLERGY DATE | set member of 1004 |
| 1000 | ALLERGY CODED | set member of 1004 |
| 1005 | HYPER-ALLERGY CODED | set member of 1004 |
| 1000 | ALLERGY CODED | question concept (foreign key for type 21) |
| 1001 | PENICILLIN | answer concept of 1000 |
| 1002 | CATS | answer concept of 1000 |
| 1003 | OPENMRS | answer concept of 1000 |

## New Form XML Files

**`personAttributeConceptSetForeignKeyForm.xml`**
```xml
<htmlform>
    Date: <encounterDate/>
    Location: <encounterLocation/>
    Provider: <encounterProvider role="Provider"/>
    Attr: <personAttribute attributeType="b1e61b61-5f1a-4a19-8a4c-test00000020"/>
    <submit/>
</htmlform>
```

**`personAttributeConceptAnswerForeignKeyForm.xml`**
```xml
<htmlform>
    Date: <encounterDate/>
    Location: <encounterLocation/>
    Provider: <encounterProvider role="Provider"/>
    Attr: <personAttribute attributeType="c2e61b61-5f1a-4a19-8a4c-test00000021"/>
    <submit/>
</htmlform>
```

## New Tests in `PersonAttributeTagTest`

Both tests follow the `RegressionTestHelper` pattern used by existing tests in the file.

**`testConceptAttributeWithForeignKeyConceptSet`**
- Uses attribute type 20 (foreign_key → concept 1004, a set).
- ENTER mode: assert the rendered HTML contains the set member names as `<option>` values (ALLERGY, ALLERGY DATE, ALLERGY CODED, HYPER-ALLERGY CODED).
- Submit with one member's concept ID; assert the patient's attribute is saved with that ID.

**`testConceptAttributeWithForeignKeyConceptAnswers`**
- Uses attribute type 21 (foreign_key → concept 1000, a question concept).
- ENTER mode: assert the rendered HTML contains the answer concept names (PENICILLIN, CATS, OPENMRS).
- Submit with one answer concept's ID; assert the patient's attribute is saved with that ID.

## Verification

```bash
# Run the PersonAttributeTagTest suite
mvn test -pl api -Dtest=PersonAttributeTagTest -Dsurefire.failIfNoSpecifiedTests=false
```

All existing tests must continue to pass unchanged.
