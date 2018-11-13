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

import java.util.Map;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.ingest.Processor.Factory;
import org.elasticsearch.plugins.IngestPlugin;
import org.elasticsearch.plugins.Plugin;

/**
 * Implementation of Elasticsearch Ingest Plugin
 */
public class ExtractPlugin extends Plugin implements IngestPlugin {

  @Override
  public Map<String, Factory> getProcessors(Processor.Parameters parameters) {
    return MapBuilder.<String, Processor.Factory>newMapBuilder()
        .put(ExtractProcessor.TYPE, new ExtractProcessor.Factory())
        .immutableMap();
  }
}
