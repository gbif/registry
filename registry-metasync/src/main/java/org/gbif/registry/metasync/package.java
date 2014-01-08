/**
 * DiGIR writes the following Machine tags:
 * <ul>
 *   <li>Installation: {@code code} is the unique code by which to identify a host</li>
 *   <li>Installation: {@code version} is the version of the DiGIR software running on the host</li>
 *   <li>Dataset: {@code code} is the unique code (per Installation) that identifies a Resource/Dataset. We use this to
 *   map between existing and parsed Datasets </li>
 *   <li>Dataset: {@code declaredCount} is the number of records reported by the host</li>
 *   <li>Dataset: {@code maxSearchResponseRecords} is the maximum number of records delivered in a single search
 *   response. Can be used to adapt paging.</li>
 *   <li>Dataset: {@code dateLastUpdated} is reported by the host</li>
 *   <li>Dataset: {@code conceptualSchema} (may occur multiple times) are the URLs of the conceptual schemas this
 *   resource supports (not the namespace!)</li>
 * </ul>
 *
 * TAPIR writes the following Machine tags:
 * <ul>
 *   <li>Installation: {@code softwareName} is the reported name of the host software</li>
 *   <li>Installation: {@code version} is the version of the host software</li>
 *   <li>Dataset: {@code conceptualSchema} (may occur multiple times) are the namespaces of the conceptual schemas this
 *   dataset supports</li>
 * </ul>
 *
 * BioCASe writes the following Machine tags:
 * <ul>
 *   <li>Endpoint: {@code conceptualSchema} will only occur once and will be either the namespace for
 *   ABCD 1.2 or 2.06</li>
 * </ul>
 */
package org.gbif.registry.metasync;
