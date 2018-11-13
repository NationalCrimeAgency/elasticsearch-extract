# Elasticsearch "Extract" Ingest Plugin

This project provides an [Elasticsearch Ingest Plugin](https://www.elastic.co/guide/en/elasticsearch/plugins/6.4/ingest.html)
that uses the [Annot8](http://annot8.io) framework to perform entity extraction on documents as they
are ingested into Elasticsearch.

It is current under development, and may not support full functionality.

## Requirements

This project requires Java 9 or later, and Elasticsearch 6.4.3.

## Building

To build this project, run the following Apache Maven command:

    mvn clean package

This will produce  `target/extract-1.0-SNAPSHOT.zip`, which is a packaged Elasticsearch plugin ready
to be used in Elasticsearch.

## Installation

To install the plugin, run the following command in the Elasticsearch home directory:

    sudo bin/elasticsearch-plugin install file:///path/to/plugin/extract-1.0-SNAPSHOT.zip

You will need to modify the path to point to the correct location for the zip file built in the
Building stage above.

During installation, you will be asked if you want to grant additional permissions to the plugin.
These are required, and if you do not provide these permissions you will be unable to use the plugin
(although it will install successfully).

If you wish to remove the plugin at some point in the future, you can run:

    sudo bin/elasticsearch-plugin remove extract

## Usage

For a complete reference to using Elasticsearch Ingest Plugins, refer to the
[relevant Elasticsearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/6.4/ingest.html).
The information below may not be complete.

First, we need to define the processing pipeline in Elasticsearch. This can be done with the
following REST command, which, as an example, creates a pipeline that will process the `content` and
`source_content` fields using the `Email` and `EpochTime` processors from Annot8.
The `EpochTime` processor is provided with some additional configuration.

    PUT /_ingest/pipeline/test-pipeline
    {
      "description" : "My test pipeline",
      "processors" : [
        {
          "extract" : {
            "fields": ["content","source_content"],
            "processors": [
              {
                "class" : "io.annot8.components.cyber.processors.Email"
              },
              {
                "class" : "io.annot8.components.cyber.processors.EpochTime",
                "settings" : {
                  "milliseconds" : false
                }
              }
            ]
          }
        }
      ]
    }

Now you need to ingest a document into an index using the pipeline you've just created.
For example:

    PUT /test/_doc/001?pipeline=test-pipeline
    {
      "content": "sally@example.com sent an e-mail to mary@example.com at 1536661822",
      "contact": "james@example.com",
      "report_date": "2018-08-21T1051"
    }

If you now view the document in the index, you will find the additional `extracted` field with
the extracted entities.

    GET /test/_doc/001
    {
        "_index": "test",
        "_type": "_doc",
        "_id": "001",
        "_version": 1,
        "found": true,
        "_source": {
            "contact": "james@example.com",
            "extracted": [
                "mary@example.com",
                "1536661822",
                "sally@example.com"
            ],
            "content": "sally@example.com sent an e-mail to mary@example.com at 1536661822",
            "report_date": "2018-08-21T1051"
        }
    }

## Configuration

Configuration of the plugin is done when you create the pipeline (the first stage in the Usage
section above).

The following options are available when configuring the plugin:

| Setting | Type |Description | Default |
| --- | --- | --- | --- |
| process_all_fields | Boolean | If true, then all fields in a document are processed and the `fields` parameter is ignored. | false |
| fields | List | A list of fields which should be processed (if present). Ignored if `process_all_fields` is true. | *None* |
| target_field | String | The name of the field which extracted entities will be stored in. | extracted |
| processors | List | A list of Annot8 processors (see below) that should be used to process the documents | Email processor |

To configure a processor, you need to provide the following:

| Setting | Type |Description |
| --- | --- | ---  |
| class | String | The fully qualified class of the processor you wish to include. |
| settings | Map | A JSON object representing the settings for the processor. If not provided, an EmptySettings object is used. |

At present, parsing of the `settings` object is fairly naive and will only support very simple
configurations. This is an area that needs substantial improvement.