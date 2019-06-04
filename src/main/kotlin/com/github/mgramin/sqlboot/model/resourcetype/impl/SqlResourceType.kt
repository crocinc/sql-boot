/*
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2016-2019 Maksim Gramin
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.mgramin.sqlboot.model.resourcetype.impl

import com.github.mgramin.sqlboot.model.connection.Endpoint
import com.github.mgramin.sqlboot.model.dialect.Dialect
import com.github.mgramin.sqlboot.model.resource.DbResource
import com.github.mgramin.sqlboot.model.resource.impl.DbResourceImpl
import com.github.mgramin.sqlboot.model.resourcetype.Metadata
import com.github.mgramin.sqlboot.model.resourcetype.ResourceType
import com.github.mgramin.sqlboot.model.uri.Uri
import com.github.mgramin.sqlboot.model.uri.impl.DbUri
import com.github.mgramin.sqlboot.model.uri.impl.FakeUri
import com.github.mgramin.sqlboot.sql.select.SelectQuery
import com.github.mgramin.sqlboot.sql.select.impl.SimpleSelectQuery
import com.github.mgramin.sqlboot.sql.select.wrappers.JdbcSelectQuery
import com.github.mgramin.sqlboot.sql.select.wrappers.OrderedSelectQuery
import com.github.mgramin.sqlboot.sql.select.wrappers.PaginatedSelectQuery
import com.github.mgramin.sqlboot.sql.select.wrappers.RestSelectQuery
import com.github.mgramin.sqlboot.template.generator.impl.GroovyTemplateGenerator
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.apache.commons.lang3.StringUtils.strip
import reactor.core.publisher.Flux

/**
 * Created by MGramin on 12.07.2017.
 */
class SqlResourceType(
        sql: String,
        private val endpoints: List<Endpoint>,
        private val dialects: List<Dialect>
) : ResourceType {

    private val simpleSelectQuery = SimpleSelectQuery(GroovyTemplateGenerator(sql))

    override fun aliases(): List<String> {
        return listOf(simpleSelectQuery.properties()["name"]!!)
    }

    override fun path(): List<String> {
        return simpleSelectQuery.columns().keys
                .filter { v -> v.startsWith("@") }
                .map { v -> strip(v, "@") }
                .ifEmpty { listOf(simpleSelectQuery.columns().keys.first()) }
    }

    override fun read(uri: Uri): Flux<DbResource> {
        val specificDialect = simpleSelectQuery.properties()["dialect"]
        return Flux.merge(
                endpoints
                        .map { connection ->
                            return@map createQuery(uri, connection, specificDialect
                                    ?: connection.properties()["sql.dialect"].toString()).execute(hashMapOf("uri" to uri))
                                    .map<Map<String, Any>?> {
                                        val toMutableMap = it.toMutableMap()
                                        toMutableMap["endpoint"] = connection.name()
                                        toMutableMap
                                    }
                        }
                        .toList())
                .map { o ->
                    val path = o!!.entries
                            .filter { v -> v.key.startsWith("@") }
                            .map { it.value.toString() }
                    val headers = o.entries
                            .map { strip(it.key, "@") to it.value }
                            .toMap()
                    val name = if (path.isEmpty()) {
                        headers.asSequence().first().key
                    } else {
                        path[path.size - 1]
                    }
                    DbResourceImpl(name, this, DbUri(headers["endpoint"].toString(), this.name(), path), headers) as DbResource
                }
    }

    override fun metaData(uri: Uri): List<Metadata> =
            simpleSelectQuery
                    .columns()
                    .map { Metadata(it.key, it.value) } +
                    Metadata("endpoint", """{"label": "Endpoint", "description": "Endpoint name", "visible": false}""")

    override fun toJson(): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.addProperty("name", name())
        jsonObject.addProperty("aliases", aliases().toString())
        jsonObject.addProperty("query", simpleSelectQuery.query())
        jsonObject.add("properties", Gson().toJsonTree(simpleSelectQuery.properties()))
        val jsonArray = JsonArray()
        metaData(FakeUri()).forEach { jsonArray.add(it.toJson()) }
        jsonObject.add("metadata", jsonArray)
        return jsonObject
    }

    private fun createQuery(uri: Uri, endpoint: Endpoint, dialect: String): SelectQuery {
        val paginationQueryTemplate = dialects.first { it.name() == dialect }.paginationQueryTemplate()
        val baseQuery =
                PaginatedSelectQuery(
                        OrderedSelectQuery(
                                simpleSelectQuery,
                                uri.orderedColumns()),
                        uri,
                        paginationQueryTemplate)
        return if (simpleSelectQuery.properties()["executor"] == "http") {
            RestSelectQuery(
                    baseQuery,
                    endpoint = "http://${endpoint.host()}:${endpoint.properties()["os.query.rest.port"]}")
        } else {
            JdbcSelectQuery(
                    baseQuery,
                    dataSource = endpoint.getDataSource())
        }
    }

}
