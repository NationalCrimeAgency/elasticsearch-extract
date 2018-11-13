/*
National Crime Agency (c) Crown Copyright 2018

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.gov.nca.elasticsearch.extract;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.ingest.IngestDocument;
import org.junit.Test;

public class ExtractProcessorTest {

  @Test
  public void testEmails() throws Exception {
    ExtractSettings settings = new ExtractSettings()
        .withField("source_field")
        .withField("another_source_field")
        .withTargetField("target_field")
        .withProcessorName("io.annot8.components.cyber.processors.Email");

    /* ExtractProcessor processor = new ExtractProcessor(randomAsciiOfLength(10), Set
        .of("source_field", "another_source_field"), "target_field");*/
    ExtractProcessor processor = new ExtractProcessor("abcdefghij", settings);

    Map<String, Object> document = new HashMap<>();
    document.put("source_field", "John (john@example.com) e-mailed jane@example.com last week.");
    document.put("another_source_field", "mary@example.com");
    document.put("dont_process", "peter@example.com");

    // IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
    IngestDocument ingestDocument = new IngestDocument(document, Collections.emptyMap());

    processor.execute(ingestDocument);

    Map<String, Object> data = ingestDocument.getSourceAndMetadata();

    List<String> extracted = (List<String>) data.get("target_field");
    assertThat(extracted, containsInAnyOrder("john@example.com", "jane@example.com", "mary@example.com"));
    assertThat(extracted, not(contains("peter@example.com")));
  }

  //TODO: Switch to full ES Test Framework to properly test in context
}
