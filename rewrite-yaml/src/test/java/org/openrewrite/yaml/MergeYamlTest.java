/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.yaml;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class MergeYamlTest implements RewriteTest {

    @Issue("https://github.com/moderneinc/support-public/issues/5")
    @Test
    void scalarList() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.on",
            //language=yaml
            """
              schedule:
                  - cron: "0 18 * * *"
              """,
            true,
            null
          )),
          yaml(
            """
              name: GitHub Actions workflow
              on: [workflow_dispatch]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1469")
    @Test
    void emptyDocument() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              spring:
                application:
                  name: update
                  description: a description
              """,
            false,
            null
          )),
          yaml(
            "",
            """
              spring:
                application:
                  name: update
                  description: a description
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/905")
    @Test
    void existingMultipleEntryBlock() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              spring:
                application:
                  name: update
                  description: a description
              """,
            false,
            null
          )),
          yaml(
            """
              spring:
                application:
                  name: main
              """,
            """
              spring:
                application:
                  name: update
                  description: a description
              """
          )
        );
    }

    @DocumentExample
    @Test
    void nonExistentBlock() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec",
            //language=yaml
            """
              lifecycleRule:
                  - action:
                        type: Delete
                    condition:
                        age: 7
              """,
            false,
            null
          )),
          yaml(
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
              """,
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
                  lifecycleRule:
                      - action:
                            type: Delete
                        condition:
                            age: 7
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1598")
    @Test
    void mergeList() {
        rewriteRun(spec ->
            spec.recipe(new MergeYaml(
              "$",
              //language=yaml
              """
                widget:
                  list:
                    - item 2
                """,
              false, null
            )),
          yaml(
            """
              widget:
                list:
                  - item 1
                """,
            """
              widget:
                list:
                  - item 1
                  - item 2
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1598")
    @Test
    void mergeListAcceptTheirs() {
        rewriteRun(
          spec -> spec.recipe(new
              MergeYaml(
              "$",
              //language=yaml
              """
                widget:
                  list:
                    - item 2
                """,
              true, null
            )
          ),
          yaml(
            """
              widget:
                list:
                  - item 1
              """
          )
        );
    }

    @Test
    void scalar() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec",
            //language=yaml
            """
              bucketPolicyOnly: true
              """,
            false,
            null
          )),
          yaml(
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: false
              """,
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/418")
    @Test
    void insertYaml() {
        rewriteRun(
          spec -> spec
            .cycles(2)
            .recipe(new MergeYaml(
              "$.spec",
              //language=yaml
              """
                lifecycleRule:
                    - action:
                          type: Delete
                      condition:
                          age: 7
                """,
              false,
              null
            )),
          yaml(
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
              """,
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
                  lifecycleRule:
                      - action:
                            type: Delete
                        condition:
                            age: 7
              """
          )
        );
    }

    @Test
    void insertAtRoot() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            "spec: 0",
            true,
            null
          )),
          yaml(
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              """,
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              spec: 0
              """
          )
        );
    }

    @Test
    void insertInSequenceEntries() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec.containers",
            "imagePullPolicy: Always",
            true,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
                    imagePullPolicy: Always
              """
          )
        );
    }

    @Test
    @Disabled
    void insertInSequenceEntriesMatchingPredicate() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec.containers[?(@.name == 'pod-0')]",
            //language=yaml
            "imagePullPolicy: Always",
            true,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                  - name: pod-1
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                    imagePullPolicy: Always
                  - name: pod-1
              """
          )
        );
    }

    @Test
    void insertBlockInSequenceEntriesWithExistingBlock() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec.containers",
            //language=yaml
            """
              securityContext:
                privileged: false
              """,
            true,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                    securityContext:
                      foo: bar
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                    securityContext:
                      foo: bar
                      privileged: false
              """
          )
        );
    }

    @Test
    void insertNestedBlockInSequenceEntries() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec.containers",
            //language=yaml
            """
              securityContext:
                privileged: false
              """,
            true,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                    securityContext:
                      privileged: false
              """
          )
        );
    }

    @Test
    void mergeMappingEntry() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.steps[?(@.uses == 'actions/setup-java')]",
            //language=yaml
            """
              with:
                cache: 'gradle'
              """,
            false,
            null
          )),
          yaml(
            """
              steps:
                - uses: actions/checkout
                - uses: actions/setup-java
              """,
            """
              steps:
                - uses: actions/checkout
                - uses: actions/setup-java
                  with:
                    cache: 'gradle'
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1275")
    @Test
    void maintainCorrectSequenceIndent() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              darwin:
                logging:
                  - 1
                  - 2
                finches:
                  species:
                    Geospiza:
                      - Sharp-beaked
                      - Common cactus
                    Camarhynchus:
                      - Woodpecker
                      - Mangrove
              """,
            true,
            null
          )),
          yaml(
            """
              com:
                key1: value1
              """,
            """
              com:
                key1: value1
              darwin:
                logging:
                  - 1
                  - 2
                finches:
                  species:
                    Geospiza:
                      - Sharp-beaked
                      - Common cactus
                    Camarhynchus:
                      - Woodpecker
                      - Mangrove
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1292")
    @Test
    void mergeSequenceWithinMap() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              core:
                - map2:
                    value:
                      - 1
                      - 2
              """,
            true,
            null
          )),
          yaml(
            """
              noncore:
                key1: value01
              """,
            """
              noncore:
                key1: value01
              core:
                - map2:
                    value:
                      - 1
                      - 2
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1302")
    @Test
    void mergeSequenceMapWithInMap() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              testing:
                mmap4:
                  - mmmap1: v111
                    mmmap2: v222
                  - nnmap1: v111
                    nnmap2: v222
              """,
            true,
            null
          )),
          yaml(
            """
              com:
                key1: value1
                key3: value3
              testing:
                core:
                  key1: value01
              """,
            """
              com:
                key1: value1
                key3: value3
              testing:
                core:
                  key1: value01
                mmap4:
                  - mmmap1: v111
                    mmmap2: v222
                  - nnmap1: v111
                    nnmap2: v222
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapAddAdditionalObject() {
        rewriteRun(
          spec -> spec
            .expectedCyclesThatMakeChanges(2)
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name: jdk_version
                    value: 18
                """,
              false,
              "name"
            )),
          yaml(
            """
              testing:
                table:
                  - name: build_tool
                    row2key2: maven
              """,
            """
              testing:
                table:
                  - name: build_tool
                    row2key2: maven
                  - name: jdk_version
                    value: 18
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapAddObject() {
        rewriteRun(
          spec -> spec
            .expectedCyclesThatMakeChanges(2)
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name: jdk_version
                    value: 18
                """,
              false,
              "name"
            )),
          yaml(
            """
              testing:
                another: value
              """,
            """
              testing:
                another: value
                table:
                  - name: jdk_version
                    value: 18
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapAddObjectFromRoot() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
              """,
            false,
            null
          )),
          yaml(
            """
              """,
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapWhenOneIdenticalObjectExistsTheSecondIsAdded() {
        rewriteRun(
          spec -> spec
            .expectedCyclesThatMakeChanges(2)
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name: jdk_version
                    value: 18
                  - name: build_tool
                    row2key2: maven
                """,
              false,
              "name"
            )),
          yaml(
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
              """,
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
                  - name: build_tool
                    row2key2: maven
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapWhenOneDifferentObjectExistsValuesAreChanged() {
        rewriteRun(
          spec -> spec
            .expectedCyclesThatMakeChanges(2)
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name: jdk_version
                    value: 17
                """,
              false,
              "name"
            )),
          yaml(
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
              """,
            """
              testing:
                table:
                  - name: jdk_version
                    value: 17
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapAddComplexMapping() {
        rewriteRun(
          spec -> spec
            .expectedCyclesThatMakeChanges(2)
            .recipe(new MergeYaml(
              "$.spec",
              //language=yaml
              """
                serviceClaims:
                  - name: db02
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database-02
                """,
              false,
              "name"
            )),
          yaml(
            """
              spec:
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database
              """,
            """
              spec:
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database
                  - name: db02
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database-02
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapChangeComplexMapping() {
        rewriteRun(
          spec -> spec
            .expectedCyclesThatMakeChanges(2)
            .recipe(new MergeYaml(
              "$.spec",
              //language=yaml
              """
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v2
                      kind: MySQL
                      name: relation-profile-database
                """,
              false,
              "name"
            )),
          yaml(
            """
              spec:
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database
              """,
            """
              spec:
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v2
                      kind: MySQL
                      name: relation-profile-database
              """
          )
        );
    }

}
