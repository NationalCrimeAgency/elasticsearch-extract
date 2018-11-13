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

import static org.elasticsearch.ingest.ConfigurationUtils.readBooleanProperty;
import static org.elasticsearch.ingest.ConfigurationUtils.readList;
import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.annot8.common.implementations.context.SimpleContext;
import io.annot8.common.implementations.data.BaseItemFactory;
import io.annot8.common.implementations.data.WrappingBaseItemToItem;
import io.annot8.common.implementations.factories.SimpleItemFactory;
import io.annot8.common.implementations.registries.ContentBuilderFactoryRegistry;
import io.annot8.core.components.Processor;
import io.annot8.core.components.responses.ProcessorResponse;
import io.annot8.core.components.responses.ProcessorResponse.Status;
import io.annot8.core.context.Context;
import io.annot8.core.data.Item;
import io.annot8.core.data.ItemFactory;
import io.annot8.core.exceptions.Annot8Exception;
import io.annot8.core.exceptions.ProcessingException;
import io.annot8.core.settings.Settings;
import io.annot8.core.settings.SettingsClass;
import io.annot8.defaultimpl.content.DefaultText;
import io.annot8.defaultimpl.factories.DefaultBaseItemFactory;
import io.annot8.defaultimpl.factories.DefaultContentBuilderFactoryRegistry;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import uk.gov.nca.elasticsearch.extract.ExtractSettings.ProcessorSettingsPair;

/**
 * Elasticsearch processor which uses Annot8 to extract entities from Elasticsearch documents
 * and appends the extracted entities to the document in a target field so that they can be
 * searched explicitly.
 */
public class ExtractProcessor extends AbstractProcessor {

  /**
   * The type defined by this processor (used for settings)
   */
  public static final String TYPE = "extract";

  /**
   * Name of setting used to control whether all fields should be processed or not
   */
  public static final String CONFIG_PROCESS_ALL_FIELDS = "process_all_fields";

  /**
   * Name of setting used to control which fields are processed
   */
  public static final String CONFIG_FIELDS = "fields";

  /**
   * Name of the setting used to control where extracted entities are saved
   */
  public static final String CONFIG_TARGET_FIELD = "target_field";

  /**
   * Name of setting used to control which processors are used
   */
  public static final String CONFIG_PROCESSORS = "processors";

  private final Set<String> fields;
  private final boolean allFields;
  private final String targetField;
  private final List<Processor> processors = new ArrayList<>();

  private final ItemFactory itemFactory;

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Create a new instance of the ExtractProcessor with the specified tag and settings.
   *
   * The tag does not affect behaviour and is used by Elasticsearch to help with
   * bookkeeping and tracing errors.
   */
  public ExtractProcessor(String tag, ExtractSettings settings) throws Exception{
    super(tag);

    //Store configuration
    this.fields = settings.getFields();
    this.allFields = settings.isAllFields();
    this.targetField = settings.getTargetField();

    //Create Annot8 pipeline
    ContentBuilderFactoryRegistry contentBuilderFactoryRegistry = new DefaultContentBuilderFactoryRegistry();
    contentBuilderFactoryRegistry.register(DefaultText.class, new DefaultText.BuilderFactory());

    BaseItemFactory bif = new DefaultBaseItemFactory(contentBuilderFactoryRegistry);

    itemFactory = new SimpleItemFactory(bif, new WrappingBaseItemToItem(bif));
    for(ProcessorSettingsPair psp : settings.getProcessors()){
      //Instantiate processor
      Processor p = psp.getProcessor().getConstructor().newInstance();

      //Configure processor with appropriate settings
      Context context = new SimpleContext(Collections.singletonList(psp.getSettings()));
      p.configure(context);

      this.processors.add(p);
    }
  }

  @Override
  public void execute(IngestDocument ingestDocument) throws Exception {
    Item item = itemFactory.create();

    // Create Content objects for necessary fields
    if(allFields){
      createTextContents(item, ingestDocument, ingestDocument.getSourceAndMetadata().keySet());
    } else {
      createTextContents(item, ingestDocument, this.fields);
    }

    // Execute each processor in turn
    for(Processor p : processors){
      ProcessorResponse response = p.process(item);

      if(response.getStatus() != Status.OK){
        throw new ProcessingException("Error extracting information with processor "+p.getClass().getName());
      }
    }

    // Get annotations and add to ingestDocument
    Set<String> extracted = new HashSet<>();

    item.getContents(DefaultText.class).forEach(c ->
      c.getAnnotations().getAll().forEach(a ->
        a.getBounds().getData(c).ifPresent(extracted::add)
      )
    );

    ingestDocument.setFieldValue(targetField, new ArrayList<>(extracted));
  }

  /**
   * Creates a new Text Content object for each specified field in the ingest document
   */
  private void createTextContents(Item item, IngestDocument ingestDocument, Set<String> fields) throws Annot8Exception {
    for(String field : fields) {
      if(!ingestDocument.hasField(field))
        continue;

      String content = ingestDocument.getFieldValue(field, String.class);
      item.create(DefaultText.class)
          .withName(field)
          .withData(content)
          .save();
    }
  }

  @Override
  public String getType() {
    return TYPE;
  }

  /**
   * Factory for creating ExtractProcessor classes
   */
  public static final class Factory implements org.elasticsearch.ingest.Processor.Factory {

    @Override
    public ExtractProcessor create(Map<String, org.elasticsearch.ingest.Processor.Factory> factories, String tag, Map<String, Object> config) throws Exception{

      //Create ExtractSettings object from properties
      ExtractSettings settings = new ExtractSettings();

      if(config.containsKey(CONFIG_PROCESS_ALL_FIELDS)){
        boolean allFields = readBooleanProperty(TYPE, tag, config, CONFIG_PROCESS_ALL_FIELDS, false);

        if(allFields){
          settings.withAllFields();
        }else{
          settings.withFields(readList(TYPE, tag, config, CONFIG_FIELDS));
        }
      }else{
        // Process only specified fields
        settings.withFields(readList(TYPE, tag, config, CONFIG_FIELDS));
      }

      if(config.containsKey(CONFIG_TARGET_FIELD))
        settings.withTargetField(readStringProperty(TYPE, tag, config, CONFIG_TARGET_FIELD));


      if(config.containsKey(CONFIG_PROCESSORS)){
        List<Map<String, Object>> l = readList(TYPE, tag, config, CONFIG_PROCESSORS);

        for(Map<String, Object> o : l){
          ProcessorDefinition def = new ProcessorDefinition(o);
          settings.withProcessorSettingsPair(parseProcessorDefinition(def));
        }
      }

      return new ExtractProcessor(tag, settings);
    }

    /**
     * Parses the processor definition and creates the Annot8 processor class and associated
     * settings object.
     *
     * The creation of a settings object is currently done in a naive way, and may not be suitable
     * for more complex settings objects.
     */
    private ProcessorSettingsPair parseProcessorDefinition(ProcessorDefinition def){
      try {
        Class<? extends Processor> processor = (Class<? extends Processor>) Class.forName(def.getClazz());

        if(!def.getSettings().isEmpty()){
          SettingsClass sc = processor.getAnnotation(SettingsClass.class);
          if(sc != null){
            Class<? extends Settings> s = sc.value();

            Settings processorSettings = AccessController.doPrivileged(
                (PrivilegedAction<Settings>) () -> objectMapper.convertValue(def.getSettings(), s));

            return new ProcessorSettingsPair(processor, processorSettings);
          }
        }else{
          return new ProcessorSettingsPair(processor);
        }
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("Could not find processor", e);
      }

      return null;
    }

  }

  /**
   * Holds the definition of a processor as defined in the JSON configuration of the Elasticsearch
   * ingest processor
   */
  private static final class ProcessorDefinition {
    private final String clazz;
    private final Map<String, Object> settings;

    /**
     * Create a new processor definition from a configuration map (e.g. JSON).
     *
     * This map must have a `class` property which defines the processor, and a `settings` property
     * which contains the settings for the processor.
     */
    public ProcessorDefinition(Map<String, Object> config){
      if(!config.containsKey("class"))
        throw new IllegalArgumentException("Property class is required");

      try {
        clazz = (String) config.get("class");
      }catch (ClassCastException e){
        throw new IllegalArgumentException("Property class must be a String");
      }

      if(config.containsKey("settings")){
        try {
          settings = (Map<String, Object>) config.get("settings");
        }catch (ClassCastException e){
          throw new IllegalArgumentException("Property settings must be a Map");
        }
      }else{
        settings = Collections.emptyMap();
      }
    }

    /**
     * Returns the processor class specified by this definition
     */
    public String getClazz() {
      return clazz;
    }

    /**
     * Returns the settings specified by this definition
     */
    public Map<String, Object> getSettings() {
      return settings;
    }

  }
}
