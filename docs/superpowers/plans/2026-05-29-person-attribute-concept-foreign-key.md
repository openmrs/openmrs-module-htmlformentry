# PersonAttribute Concept ForeignKey Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend `PersonAttributeSubmissionElement.buildConceptWidget()` to populate concept dropdown options from `PersonAttributeType.foreignKey` (a concept set or question concept) when `answerConceptIds` is not specified on the tag.

**Architecture:** Priority chain: (1) explicit `answerConceptIds` tag attribute → (2) `attributeType.getForeignKey()` resolves to either set members or concept answers → (3) `BadFormDesignException`. Three private helpers (`resolveAnswerConcepts`, `resolveConceptsFromIdList`, `resolveConceptsFromForeignKey`) replace the current inline logic in `buildConceptWidget`. Test fixture gets two new `PersonAttributeType` rows; two new form XML files and two new integration tests cover the new paths.

**Tech Stack:** Java 8+, OpenMRS API (`Concept`, `ConceptAnswer`, `PersonAttributeType`), JUnit 4 / `RegressionTestHelper`, DBUnit XML fixtures.

---

## File Map

| Action | Path |
|--------|------|
| Modify | `api/src/main/java/org/openmrs/module/htmlformentry/element/PersonAttributeSubmissionElement.java` |
| Modify | `api/src/test/resources/org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml` |
| Create | `api/src/test/resources/org/openmrs/module/htmlformentry/include/personAttributeConceptSetForeignKeyForm.xml` |
| Create | `api/src/test/resources/org/openmrs/module/htmlformentry/include/personAttributeConceptAnswerForeignKeyForm.xml` |
| Modify | `api/src/test/java/org/openmrs/module/htmlformentry/PersonAttributeTagTest.java` |

---

### Task 1: Add test PersonAttributeTypes to fixture

New rows reuse existing concept data already in the fixture:
- Concept **1004** (`is_set=true`, "ANOTHER ALLERGY CONSTRUCT") → members 80000/ALLERGY, 1119/ALLERGY DATE, 1000/ALLERGY CODED, 1005/HYPER-ALLERGY CODED
- Concept **1000** ("ALLERGY CODED", `is_set=false`) → answers 1001/PENICILLIN, 1002/CATS, 1003/OPENMRS

**Files:**
- Modify: `api/src/test/resources/org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml`

- [ ] **Step 1: Locate the PersonAttributeType section in the fixture**

Search for `person_attribute_type_id="19"` — that is the existing custom type block. Add two new rows immediately after it.

- [ ] **Step 2: Insert two new PersonAttributeType rows**

Add after the existing `person_attribute_type_id="19"` row (and its closing `/>`) but still inside the `<dataset>` element:

```xml
<!-- PersonAttributeType 20: Concept-type attribute; foreignKey → concept set 1004 -->
<person_attribute_type person_attribute_type_id="20"
    name="Test Concept Set Attribute"
    description="Test attribute type whose answers come from a concept set"
    format="org.openmrs.Concept"
    foreign_key="1004"
    searchable="false" creator="1" date_created="2024-01-01 00:00:00.0"
    retired="false" uuid="b1e61b61-5f1a-4a19-8a4c-test00000020" sort_weight="20"/>

<!-- PersonAttributeType 21: Concept-type attribute; foreignKey → question concept 1000 -->
<person_attribute_type person_attribute_type_id="21"
    name="Test Concept Answer Attribute"
    description="Test attribute type whose answers come from a question concept's answers"
    format="org.openmrs.Concept"
    foreign_key="1000"
    searchable="false" creator="1" date_created="2024-01-01 00:00:00.0"
    retired="false" uuid="c2e61b61-5f1a-4a19-8a4c-test00000021" sort_weight="21"/>
```

- [ ] **Step 3: Commit**

```bash
git add api/src/test/resources/org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml
git commit -m "test: add PersonAttributeType fixture rows 20 and 21 for foreignKey concept tests"
```

---

### Task 2: Create form XML files for the two new attribute types

**Files:**
- Create: `api/src/test/resources/org/openmrs/module/htmlformentry/include/personAttributeConceptSetForeignKeyForm.xml`
- Create: `api/src/test/resources/org/openmrs/module/htmlformentry/include/personAttributeConceptAnswerForeignKeyForm.xml`

- [ ] **Step 1: Create `personAttributeConceptSetForeignKeyForm.xml`**

```xml
<htmlform>
    Date: <encounterDate/>
    Location: <encounterLocation/>
    Provider: <encounterProvider role="Provider"/>
    Attr: <personAttribute attributeType="b1e61b61-5f1a-4a19-8a4c-test00000020"/>
    <submit/>
</htmlform>
```

Note: no `answerConceptIds` — options come entirely from `foreignKey=1004` (the concept set).

- [ ] **Step 2: Create `personAttributeConceptAnswerForeignKeyForm.xml`**

```xml
<htmlform>
    Date: <encounterDate/>
    Location: <encounterLocation/>
    Provider: <encounterProvider role="Provider"/>
    Attr: <personAttribute attributeType="c2e61b61-5f1a-4a19-8a4c-test00000021"/>
    <submit/>
</htmlform>
```

Note: no `answerConceptIds` — options come from `foreignKey=1000` (the question concept's answers).

- [ ] **Step 3: Commit**

```bash
git add api/src/test/resources/org/openmrs/module/htmlformentry/include/personAttributeConceptSetForeignKeyForm.xml
git add api/src/test/resources/org/openmrs/module/htmlformentry/include/personAttributeConceptAnswerForeignKeyForm.xml
git commit -m "test: add form XML files for foreignKey concept attribute tests"
```

---

### Task 3: Write failing tests

**Files:**
- Modify: `api/src/test/java/org/openmrs/module/htmlformentry/PersonAttributeTagTest.java`

- [ ] **Step 1: Add two UUID constants after the existing `LOCATION_ATTR_TYPE_UUID` constant**

In `PersonAttributeTagTest.java`, after line 46 (`LOCATION_ATTR_TYPE_UUID`):

```java
/** PersonAttributeType 20 – format=org.openmrs.Concept, foreignKey → concept set 1004 */
private static final String CONCEPT_SET_FK_ATTR_TYPE_UUID = "b1e61b61-5f1a-4a19-8a4c-test00000020";

/** PersonAttributeType 21 – format=org.openmrs.Concept, foreignKey → question concept 1000 */
private static final String CONCEPT_ANSWER_FK_ATTR_TYPE_UUID = "c2e61b61-5f1a-4a19-8a4c-test00000021";
```

- [ ] **Step 2: Add the two new test methods**

Add them in the "Concept attribute type tests" section (after `shouldEditConceptPersonAttribute`, before the Location section). The tests intentionally fail until Task 4 implements the foreignKey logic.

```java
/**
 * When a Concept-type PersonAttributeType has a foreignKey pointing to a concept set,
 * the dropdown should contain the set's non-retired members and submission should save
 * the selected member's concept ID.
 *
 * <p>PersonAttributeType 20 has foreignKey=1004 (concept set "ANOTHER ALLERGY CONSTRUCT").
 * Set members: 80000/ALLERGY, 1119/ALLERGY DATE, 1000/ALLERGY CODED, 1005/HYPER-ALLERGY CODED.
 */
@Test
public void shouldBuildConceptDropdownFromForeignKeyConceptSet() throws Exception {
    new RegressionTestHelper() {

        @Override
        public Patient getPatient() {
            return Context.getPatientService().getPatient(PATIENT_ID);
        }

        @Override
        public String getFormName() {
            return "personAttributeConceptSetForeignKeyForm";
        }

        @Override
        public String[] widgetLabels() {
            return new String[] { "Date:", "Location:", "Provider:", "Attr:" };
        }

        @Override
        public void testBlankFormHtml(String html) {
            // Verify that set members appear as dropdown options
            assertTrue("Expected ALLERGY DATE (concept 1119) in dropdown", html.contains("ALLERGY DATE"));
            assertTrue("Expected HYPER-ALLERGY CODED (concept 1005) in dropdown", html.contains("HYPER-ALLERGY CODED"));
        }

        @Override
        public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
            request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
            request.addParameter(widgets.get("Location:"), "2");
            request.addParameter(widgets.get("Provider:"), "502");
            // Submit concept 80000 (ALLERGY) — a member of set 1004
            request.addParameter(widgets.get("Attr:"), "80000");
        }

        @Override
        public void testResults(SubmissionResults results) {
            results.assertNoErrors();
            Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
            PersonAttributeType type = Context.getPersonService()
                    .getPersonAttributeTypeByUuid(CONCEPT_SET_FK_ATTR_TYPE_UUID);
            PersonAttribute attr = patient.getAttribute(type);
            assertNotNull("Expected attribute to be created after ENTER", attr);
            assertEquals("Stored value should be the submitted concept ID", "80000", attr.getValue());
        }
    }.run();
}

/**
 * When a Concept-type PersonAttributeType has a foreignKey pointing to a question concept,
 * the dropdown should contain that concept's non-retired answer concepts and submission
 * should save the selected answer concept's ID.
 *
 * <p>PersonAttributeType 21 has foreignKey=1000 (concept "ALLERGY CODED", not a set).
 * Answers: 1001/PENICILLIN, 1002/CATS, 1003/OPENMRS.
 */
@Test
public void shouldBuildConceptDropdownFromForeignKeyConceptAnswers() throws Exception {
    new RegressionTestHelper() {

        @Override
        public Patient getPatient() {
            return Context.getPatientService().getPatient(PATIENT_ID);
        }

        @Override
        public String getFormName() {
            return "personAttributeConceptAnswerForeignKeyForm";
        }

        @Override
        public String[] widgetLabels() {
            return new String[] { "Date:", "Location:", "Provider:", "Attr:" };
        }

        @Override
        public void testBlankFormHtml(String html) {
            // Verify that answer concepts appear as dropdown options
            assertTrue("Expected PENICILLIN (answer concept 1001) in dropdown", html.contains("PENICILLIN"));
            assertTrue("Expected CATS (answer concept 1002) in dropdown", html.contains("CATS"));
            assertTrue("Expected OPENMRS (answer concept 1003) in dropdown", html.contains("OPENMRS"));
        }

        @Override
        public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
            request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
            request.addParameter(widgets.get("Location:"), "2");
            request.addParameter(widgets.get("Provider:"), "502");
            // Submit concept 1002 (CATS) — an answer of question concept 1000
            request.addParameter(widgets.get("Attr:"), "1002");
        }

        @Override
        public void testResults(SubmissionResults results) {
            results.assertNoErrors();
            Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
            PersonAttributeType type = Context.getPersonService()
                    .getPersonAttributeTypeByUuid(CONCEPT_ANSWER_FK_ATTR_TYPE_UUID);
            PersonAttribute attr = patient.getAttribute(type);
            assertNotNull("Expected attribute to be created after ENTER", attr);
            assertEquals("Stored value should be the submitted concept ID", "1002", attr.getValue());
        }
    }.run();
}
```

- [ ] **Step 3: Run the two new tests — verify they fail**

Expected: both fail because `buildConceptWidget` currently throws `BadFormDesignException` when `answerConceptIds` is absent.

```bash
mvn test -pl api -Dtest="PersonAttributeTagTest#shouldBuildConceptDropdownFromForeignKeyConceptSet+shouldBuildConceptDropdownFromForeignKeyConceptAnswers" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected output contains `FAILURES` or `ERRORS` for both tests.

- [ ] **Step 4: Update the existing no-answers-no-foreignKey error test comment**

The test `shouldRenderFormDesignErrorForConceptTypeWithoutAnswerConceptIds` (around line 744) still passes after Task 4 because Civil Status (type 8) has no `foreignKey`. Update its javadoc to reflect the new condition:

```java
/**
 * A Concept-type PersonAttributeType with neither {@code answerConceptIds} on the tag
 * nor a {@code foreignKey} set on the attribute type results in a rendered form-design error.
 * Civil Status (type 8) has no foreignKey, so this case exercises the third branch.
 */
@Test
public void shouldRenderFormDesignErrorForConceptTypeWithoutAnswerConceptIdsOrForeignKey() throws Exception {
    // Civil Status (type 8) has format=org.openmrs.Concept and no foreignKey set.
    String xml = "<htmlform><encounterDate/><encounterLocation/><encounterProvider/>"
            + "<personAttribute attributeType=\"" + CONCEPT_ATTR_TYPE_UUID + "\"/></htmlform>";
    Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
    FormEntrySession session = new FormEntrySession(patient, xml, null);
    String html = session.getHtmlToDisplay();
    assertTrue("Expected form-design error to be rendered in HTML", html.contains("error"));
}
```

- [ ] **Step 5: Commit**

```bash
git add api/src/test/java/org/openmrs/module/htmlformentry/PersonAttributeTagTest.java
git commit -m "test: add failing tests for foreignKey concept set and concept answer widget population"
```

---

### Task 4: Implement foreignKey concept resolution in `buildConceptWidget`

**Files:**
- Modify: `api/src/main/java/org/openmrs/module/htmlformentry/element/PersonAttributeSubmissionElement.java`

- [ ] **Step 1: Add two imports at the top of the file**

After the existing `import org.openmrs.Concept;` line, add:

```java
import org.openmrs.ConceptAnswer;
```

After `import java.util.Map;`, add:

```java
import java.util.stream.Collectors;
```

- [ ] **Step 2: Replace `buildConceptWidget` and add three helper methods**

Replace the entire `buildConceptWidget` method with this version, and add the three helpers after it (still inside the "Widget builders" section, before the "Helpers" section):

```java
private Widget buildConceptWidget(FormEntryContext context, Map<String, String> parameters)
        throws BadFormDesignException {

    DropdownWidget w = new DropdownWidget();
    w.addOption(new Option());

    for (Concept concept : resolveAnswerConcepts(parameters)) {
        w.addOption(new Option(concept.getName().getName(), concept.getConceptId().toString(), false));
    }

    if (existingAttribute != null && StringUtils.hasText(existingAttribute.getValue())) {
        w.setInitialValue(existingAttribute.getValue());
    }

    return w;
}

private List<Concept> resolveAnswerConcepts(Map<String, String> parameters) throws BadFormDesignException {
    String answerConceptIds = parameters.get("answerConceptIds");
    if (StringUtils.hasText(answerConceptIds)) {
        return resolveConceptsFromIdList(answerConceptIds);
    }
    if (attributeType.getForeignKey() != null) {
        return resolveConceptsFromForeignKey(attributeType.getForeignKey());
    }
    throw new BadFormDesignException(
        "<personAttribute> tag: PersonAttributeType \"" + attributeType.getName()
        + "\" (format=" + Concept.class.getName() + ") requires either an \"answerConceptIds\" "
        + "tag attribute or a foreignKey on the attribute type pointing to a concept set or question concept");
}

private List<Concept> resolveConceptsFromIdList(String answerConceptIds) throws BadFormDesignException {
    List<Concept> concepts = new ArrayList<>();
    for (String idOrUuid : answerConceptIds.split(",")) {
        String trimmed = idOrUuid.trim();
        if (trimmed.isEmpty()) {
            continue;
        }
        Concept concept = HtmlFormEntryUtil.getConcept(trimmed);
        if (concept == null) {
            throw new BadFormDesignException(
                "<personAttribute> tag: cannot find concept for answerConceptIds value \"" + trimmed + "\"");
        }
        concepts.add(concept);
    }
    return concepts;
}

private List<Concept> resolveConceptsFromForeignKey(Integer foreignKey) throws BadFormDesignException {
    Concept fk = Context.getConceptService().getConcept(foreignKey);
    if (fk == null) {
        throw new BadFormDesignException(
            "<personAttribute> tag: cannot find concept for foreignKey=" + foreignKey
            + " on PersonAttributeType \"" + attributeType.getName() + "\"");
    }
    if (Boolean.TRUE.equals(fk.getSet())) {
        return fk.getSetMembers(false);
    }
    return fk.getAnswers(false).stream()
             .map(ConceptAnswer::getAnswerConcept)
             .collect(Collectors.toList());
}
```

The old `buildConceptWidget` contained both the guard for `answerConceptIds` and the loop — both are now replaced by these four methods. The new `buildConceptWidget` is purely about building the widget; resolution logic lives in the helpers.

- [ ] **Step 3: Run the two new tests — verify they pass**

```bash
mvn test -pl api -Dtest="PersonAttributeTagTest#shouldBuildConceptDropdownFromForeignKeyConceptSet+shouldBuildConceptDropdownFromForeignKeyConceptAnswers" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: `BUILD SUCCESS`, both tests `PASS`.

- [ ] **Step 4: Run the full PersonAttributeTagTest suite — verify no regressions**

```bash
mvn test -pl api -Dtest=PersonAttributeTagTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: `BUILD SUCCESS`, all tests `PASS`.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/org/openmrs/module/htmlformentry/element/PersonAttributeSubmissionElement.java
git commit -m "feat: support foreignKey on PersonAttributeType for concept widget answer population

When answerConceptIds is not specified on the tag, the concept dropdown now
resolves its options from PersonAttributeType.foreignKey: if the referenced
concept is a set, its non-retired members are used; otherwise the concept's
non-retired answers are used. answerConceptIds still takes precedence when present."
```

---

## Verification

Run the complete suite after all tasks:

```bash
mvn test -pl api -Dtest=PersonAttributeTagTest -Dsurefire.failIfNoSpecifiedTests=false
```

All tests should be `PASS` with `BUILD SUCCESS`. There should be no regressions in the existing concept, string, or location attribute tests.
