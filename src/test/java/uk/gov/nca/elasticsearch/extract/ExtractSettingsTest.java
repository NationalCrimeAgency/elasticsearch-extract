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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.annot8.components.base.processors.Regex;
import io.annot8.components.base.processors.Regex.RegexSettings;
import io.annot8.components.cyber.processors.IPv4;
import io.annot8.components.cyber.processors.IPv6;
import io.annot8.components.cyber.processors.Url;
import io.annot8.core.settings.EmptySettings;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import uk.gov.nca.elasticsearch.extract.ExtractSettings.ProcessorSettingsPair;

public class ExtractSettingsTest {
  @Test
  public void testFields(){
    ExtractSettings settings = new ExtractSettings();

    //Test that we can add new fields
    assertFalse(settings.isAllFields());
    assertTrue(settings.getFields().isEmpty());

    settings.withField("field1");
    assertFalse(settings.getFields().isEmpty());
    assertTrue(settings.getFields().contains("field1"));

    settings.withFields(Arrays.asList("field2", "field3"));
    assertTrue(settings.getFields().contains("field1"));
    assertTrue(settings.getFields().contains("field2"));
    assertTrue(settings.getFields().contains("field3"));

    //Test all fields
    settings.withAllFields();
    assertTrue(settings.isAllFields());
    assertTrue(settings.getFields().isEmpty());

    //Test adding a field - should disable allFields
    settings.withField("field1");
    assertFalse(settings.isAllFields());
  }

  @Test
  public void testTargetField(){
    ExtractSettings settings = new ExtractSettings();

    assertEquals(ExtractSettings.DEFAULT_TARGET_FIELD, settings.getTargetField());
    settings.withTargetField("target");
    assertEquals("target", settings.getTargetField());
  }

  @Test
  public void testProcessors(){
    ExtractSettings settings = new ExtractSettings();

    assertEquals(ExtractSettings.DEFAULT_PROCESSORS, settings.getProcessors());

    RegexSettings regexSettings = new RegexSettings(Pattern.compile("[0-9]"), 0, "digit");

    settings.withProcessor(IPv4.class);
    settings.withProcessor(Regex.class, regexSettings);
    settings.withProcessors(Arrays.asList(IPv6.class, Url.class));

    List<ProcessorSettingsPair> processors = settings.getProcessors();
    assertEquals(4, processors.size());

    ProcessorSettingsPair ipv4 = new ProcessorSettingsPair(IPv4.class);
    ProcessorSettingsPair regex = new ProcessorSettingsPair(Regex.class, regexSettings);
    ProcessorSettingsPair ipv6 = new ProcessorSettingsPair(IPv6.class);
    ProcessorSettingsPair url = new ProcessorSettingsPair(Url.class);

    assertTrue(processors.contains(ipv4));
    assertTrue(processors.contains(regex));
    assertTrue(processors.contains(ipv6));
    assertTrue(processors.contains(url));
  }

  @Test
  public void testProcessorNames(){
    ExtractSettings settings = new ExtractSettings();

    assertEquals(ExtractSettings.DEFAULT_PROCESSORS, settings.getProcessors());

    RegexSettings regexSettings = new RegexSettings(Pattern.compile("[0-9]"), 0, "digit");

    settings.withProcessorName("io.annot8.components.cyber.processors.IPv4");
    settings.withProcessorName("io.annot8.components.base.processors.Regex", regexSettings);
    settings.withProcessorNames(Arrays.asList("io.annot8.components.cyber.processors.IPv6", "io.annot8.components.cyber.processors.Url"));

    try{
      // Test non-existent class
      settings.withProcessorName("not.a.real.Class");
      fail("Expected exception not thrown");
    }catch (IllegalArgumentException iae){
      //Expected exception, do nothing
    }

    try{
      // Test non-existent class with settings
      settings.withProcessorName("not.a.real.Class", EmptySettings.getInstance());
      fail("Expected exception not thrown");
    }catch (IllegalArgumentException iae){
      //Expected exception, do nothing
    }

    List<ProcessorSettingsPair> processors = settings.getProcessors();
    assertEquals(4, processors.size());

    ProcessorSettingsPair ipv4 = new ProcessorSettingsPair(IPv4.class);
    ProcessorSettingsPair regex = new ProcessorSettingsPair(Regex.class, regexSettings);
    ProcessorSettingsPair ipv6 = new ProcessorSettingsPair(IPv6.class);
    ProcessorSettingsPair url = new ProcessorSettingsPair(Url.class);

    assertTrue(processors.contains(ipv4));
    assertTrue(processors.contains(regex));
    assertTrue(processors.contains(ipv6));
    assertTrue(processors.contains(url));
  }

  @Test
  public void testProcessorSettingsPair(){
    ProcessorSettingsPair psp1 = new ProcessorSettingsPair(IPv4.class);
    ProcessorSettingsPair psp2 = new ProcessorSettingsPair(IPv4.class, EmptySettings.getInstance());
    ProcessorSettingsPair psp3 = new ProcessorSettingsPair(IPv6.class);

    assertEquals(psp1, psp2);
    assertNotEquals(psp1, psp3);
    assertNotEquals(psp1, "Hello world");
  }

}
