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

import io.annot8.components.cyber.processors.Email;
import io.annot8.core.components.Processor;
import io.annot8.core.settings.EmptySettings;
import io.annot8.core.settings.Settings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds settings for the Extract processor
 */
public class ExtractSettings {

  /**
   * Default value (false) for the "Process All Fields" setting
   */
  public static final boolean DEFAULT_PROCESS_ALL_FIELDS = false;

  /**
   * Default name (extracted) for the field to which extracted entities are added
   */
  public static final String DEFAULT_TARGET_FIELD = "extracted";

  /**
   * Default Annot8 processors (Email) to use when extracting entities
   */
  public static final List<ProcessorSettingsPair> DEFAULT_PROCESSORS = Collections.singletonList(
    new ProcessorSettingsPair(Email.class, EmptySettings.getInstance())
  );

  private Set<String> fields = new HashSet<>();
  private boolean allFields = DEFAULT_PROCESS_ALL_FIELDS;
  private String targetField = DEFAULT_TARGET_FIELD;
  private List<ProcessorSettingsPair> processors = new ArrayList<>();

  /**
   * Adds field to list of fields to process, and disables the processing
   * of all fields
   */
  public ExtractSettings withField(String field){
    this.fields.add(field);
    this.allFields = false;

    return this;
  }

  /**
   * Adds fields to list of fields to process, and disables the processing
   * of all fields
   */
  public ExtractSettings withFields(Collection<String> fields){
    this.fields.addAll(fields);
    this.allFields = false;

    return this;
  }

  /**
   * Clears the list of fields to process, and enables the processing of all
   * fields
   */
  public ExtractSettings withAllFields(){
    this.fields.clear();
    this.allFields = true;

    return this;
  }

  /**
   * Sets the target field (i.e. where the extracted entities are persisted)
   */
  public ExtractSettings withTargetField(String targetField){
    this.targetField = targetField;

    return this;
  }

  /**
   * Adds an Annot8 processor to the list with no settings (i.e. EmptySettings)
   */
  public ExtractSettings withProcessor(Class<? extends Processor> processor){
    processors.add(new ProcessorSettingsPair(processor, EmptySettings.getInstance()));

    return this;
  }

  /**
   * Adds an Annot8 processor to the list with the provided settings
   */
  public ExtractSettings withProcessor(Class<? extends Processor> processor, Settings processorSettings){
    processors.add(new ProcessorSettingsPair(processor, processorSettings));

    return this;
  }

  /**
   * Adds an Annot8 processor (by name) to the list with no settings (i.e. EmptySettings)
   */
  public ExtractSettings withProcessorName(String processor) throws IllegalArgumentException{
    try{
      processors.add(new ProcessorSettingsPair((Class<? extends Processor>) Class.forName(processor), EmptySettings.getInstance()));
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }

    return this;
  }

  /**
   * Adds an Annot8 processor (by name) to the list with the provided settings
   */
  public ExtractSettings withProcessorName(String processor, Settings processorSettings) throws IllegalArgumentException{
    try{
      processors.add(new ProcessorSettingsPair((Class<? extends Processor>) Class.forName(processor), processorSettings));
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }

    return this;
  }

  /**
   * Adds multiple Annot8 processors to the list with no settings (i.e. EmptySettings)
   */
  public ExtractSettings withProcessors(Collection<Class<? extends Processor>> processors){
    processors.stream()
        .map(p -> new ProcessorSettingsPair(p, EmptySettings.getInstance()))
        .forEach(this.processors::add);

    return this;
  }

  /**
   * Adds multiple Annot8 processors (by name) to the list with no settings (i.e. EmptySettings)
   */
  public ExtractSettings withProcessorNames(Collection<String> processors){
    processors.forEach(this::withProcessorName);

    return this;
  }

  /**
   * Adds a {@link ProcessorSettingsPair} to the list
   */
  public ExtractSettings withProcessorSettingsPair(ProcessorSettingsPair pair){
    this.processors.add(pair);

    return this;
  }

  /**
   * Returns the current set of fields to process
   */
  public Set<String> getFields(){
    return fields;
  }

  /**
   * Returns true if all fields should be processed
   */
  public boolean isAllFields() {
    return allFields;
  }

  /**
   * Returns the current target field
   */
  public String getTargetField() {
    return targetField;
  }

  /**
   * Returns the current set of processors and settings
   */
  public List<ProcessorSettingsPair> getProcessors() {
    if(processors.isEmpty())
      return DEFAULT_PROCESSORS;

    return processors;
  }

  /**
   * Holds an Annot8 processor class (not an instantiated copy of the processor),
   * and the settings to be used for instances created of that class.
   */
  static class ProcessorSettingsPair{
    private final Class<? extends Processor> processor;
    private final Settings processorSettings;

    /**
     * Create a pair using the supplied processor and EmptySettings
     */
    public ProcessorSettingsPair(Class<? extends Processor> processor){
      this.processor = processor;
      this.processorSettings = EmptySettings.getInstance();
    }

    /**
     * Create a pair using the supplied processor and settings
     */
    public ProcessorSettingsPair(Class<? extends Processor> processor, Settings processorSettings){
      this.processor = processor;
      this.processorSettings = processorSettings;
    }

    /**
     * Get processor
     */
    public Class<? extends Processor> getProcessor() {
      return processor;
    }

    /**
     * Get settings
     */
    public Settings getSettings() {
      return processorSettings;
    }

    @Override
    public boolean equals(Object obj) {
      if(!ProcessorSettingsPair.class.isInstance(obj))
        return false;

      ProcessorSettingsPair psp = (ProcessorSettingsPair) obj;
      return psp.getProcessor().equals(this.processor) &&
          psp.getSettings().equals(this.processorSettings);
    }
  }
}
