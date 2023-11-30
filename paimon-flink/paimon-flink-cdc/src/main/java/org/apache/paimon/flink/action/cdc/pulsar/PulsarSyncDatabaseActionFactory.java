/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.flink.action.cdc.pulsar;

import org.apache.paimon.flink.action.Action;
import org.apache.paimon.flink.action.ActionFactory;
import org.apache.paimon.flink.action.cdc.TypeMapping;

import org.apache.flink.api.java.utils.MultipleParameterTool;

import java.util.Optional;

/** Factory to create {@link PulsarSyncDatabaseAction}. */
public class PulsarSyncDatabaseActionFactory implements ActionFactory {

    public static final String IDENTIFIER = "pulsar-sync-database";

    @Override
    public String identifier() {
        return IDENTIFIER;
    }

    @Override
    public Optional<Action> create(MultipleParameterTool params) {
        checkRequiredArgument(params, "pulsar-conf");

        PulsarSyncDatabaseAction action =
                new PulsarSyncDatabaseAction(
                        getRequiredValue(params, "warehouse"),
                        getRequiredValue(params, "database"),
                        optionalConfigMap(params, "catalog-conf"),
                        optionalConfigMap(params, "pulsar-conf"));

        action.withTableConfig(optionalConfigMap(params, "table-conf"))
                .withTablePrefix(params.get("table-prefix"))
                .withTableSuffix(params.get("table-suffix"))
                .includingTables(params.get("including-tables"))
                .excludingTables(params.get("excluding-tables"));

        if (params.has("type-mapping")) {
            String[] options = params.get("type-mapping").split(",");
            action.withTypeMapping(TypeMapping.parse(options));
        }

        return Optional.of(action);
    }

    @Override
    public void printHelp() {
        System.out.println(
                "Action \"pulsar-sync-database\" creates a streaming job "
                        + "with a Flink Pulsar source and multiple Paimon table sinks "
                        + "to synchronize multiple tables into one Paimon database.\n"
                        + "Only tables with primary keys will be considered. ");
        System.out.println();

        System.out.println("Syntax:");
        System.out.println(
                "  pulsar-sync-database --warehouse <warehouse-path> --database <database-name> "
                        + "[--table-prefix <paimon-table-prefix>] "
                        + "[--table-suffix <paimon-table-suffix>] "
                        + "[--including-tables <table-name|name-regular-expr>] "
                        + "[--excluding-tables <table-name|name-regular-expr>] "
                        + "[--type-mapping <option1,option2...>] "
                        + "[--pulsar-conf <pulsar-source-conf> [--pulsar-conf <pulsar-source-conf> ...]] "
                        + "[--catalog-conf <paimon-catalog-conf> [--catalog-conf <paimon-catalog-conf> ...]] "
                        + "[--table-conf <paimon-table-sink-conf> [--table-conf <paimon-table-sink-conf> ...]]");
        System.out.println();

        System.out.println(
                "--table-prefix is the prefix of all Paimon tables to be synchronized. For example, if you want all "
                        + "synchronized tables to have \"ods_\" as prefix, you can specify `--table-prefix ods_`.");
        System.out.println("The usage of --table-suffix is same as `--table-prefix`");
        System.out.println();

        System.out.println(
                "--including-tables is used to specify which source tables are to be synchronized. "
                        + "You must use '|' to separate multiple tables. Regular expression is supported.");
        System.out.println(
                "--excluding-tables is used to specify which source tables are not to be synchronized. "
                        + "The usage is same as --including-tables.");
        System.out.println(
                "--excluding-tables has higher priority than --including-tables if you specified both.");
        System.out.println();

        System.out.println(
                "--type-mapping is used to specify how to map MySQL type to Paimon type. Please see the doc for usage.");
        System.out.println();

        System.out.println("pulsar source conf syntax:");
        System.out.println("  key=value");
        System.out.println(
                "'topic', 'value.format', 'pulsar.client.serviceUrl', 'pulsar.admin.adminUrl', 'pulsar.consumer.subscriptionName' "
                        + "are required configurations, others are optional.");
        System.out.println(
                "For a complete list of supported configurations, "
                        + "see https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/pulsar/#source-configurable-options");
        System.out.println();
        System.out.println();

        System.out.println("Paimon catalog and table sink conf syntax:");
        System.out.println("  key=value");
        System.out.println("All Paimon sink table will be applied the same set of configurations.");
        System.out.println(
                "For a complete list of supported configurations, "
                        + "see https://paimon.apache.org/docs/master/maintenance/configurations/");
        System.out.println();

        System.out.println("Examples:");
        System.out.println(
                "  pulsar-sync-database \\\n"
                        + "    --warehouse hdfs:///path/to/warehouse \\\n"
                        + "    --database test_db \\\n"
                        + "    --pulsar-conf topic=order,logistic,user \\\n"
                        + "    --pulsar-conf value.format=canal-json \\\n"
                        + "    --pulsar-conf pulsar.client.serviceUrl=pulsar://127.0.0.1:6650 \\\n"
                        + "    --pulsar-conf pulsar.admin.adminUrl=http://127.0.0.1:8080 \\\n"
                        + "    --pulsar-conf pulsar.consumer.subscriptionName=paimon-tests \\\n"
                        + "    --catalog-conf metastore=hive \\\n"
                        + "    --catalog-conf uri=thrift://hive-metastore:9083 \\\n"
                        + "    --table-conf bucket=4 \\\n"
                        + "    --table-conf changelog-producer=input \\\n"
                        + "    --table-conf sink.parallelism=4");
    }
}
