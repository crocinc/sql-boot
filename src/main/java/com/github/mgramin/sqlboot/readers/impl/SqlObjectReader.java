/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2017 mgramin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.mgramin.sqlboot.readers.impl;

import static java.util.stream.Collectors.toMap;

import com.github.mgramin.sqlboot.exceptions.SqlBootException;
import com.github.mgramin.sqlboot.model.DbResource;
import com.github.mgramin.sqlboot.model.DBResourceType;
import com.github.mgramin.sqlboot.readers.AbstractObjectReader;
import com.github.mgramin.sqlboot.readers.DbResourceReader;
import com.github.mgramin.sqlboot.uri.ObjURI;
import com.github.mgramin.sqlboot.util.sql.ISqlHelper;
import com.github.mgramin.sqlboot.util.template_engine.TemplateEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.ToString;
import org.apache.log4j.Logger;

/**
 * Custom-SQL db object reader
 */
@ToString
public class SqlObjectReader extends AbstractObjectReader implements DbResourceReader {

    private final static Logger logger = Logger.getLogger(SqlObjectReader.class);

    final private ISqlHelper sqlHelper;
    final public String sql; // TODO make private
    final private TemplateEngine templateEngine; // TODO move to decorator ?
    final private String prepareSql; // TODO move to decorator ?

    public SqlObjectReader(ISqlHelper sqlHelper, TemplateEngine templateEngine, String sql) {
        this.sqlHelper = sqlHelper;
        this.templateEngine = templateEngine;
        this.sql = sql;
        prepareSql = null;
    }

    @Override
    public Map<String, DbResource> read(ObjURI objURI, DBResourceType type) throws SqlBootException {
        List<String> list = objURI.getObjects();

        Map<String, DbResource> objects = new LinkedHashMap<>();
        try {
            Map<String, Object> data = new HashMap();
            int i = 0;
            templateEngine.setTemplate(sql);
            for (String s : templateEngine.getAllProperties()) {
                try {
                    data.put(s, list.get(i++));
                } catch (Throwable t) {
                    data.put(s, '%');
                }
            }

            templateEngine.setTemplate(sql);
            String prepareSQL = templateEngine.process(data);
            logger.debug(prepareSQL);

            List<Map<String, String>> select = sqlHelper.select(prepareSQL);
            for (Map<String, String> stringStringMap : select) {
                DbResource object = new DbResource();
                object.paths = stringStringMap;
                List<String> objectsForUri = new ArrayList<>();
                for (Map.Entry<String, String> stringStringEntry : stringStringMap.entrySet()) {
                    if (!stringStringEntry.getKey().startsWith("@")) {
                        objectsForUri.add(stringStringEntry.getValue());
                        object.name = stringStringEntry.getValue();
                        object.addProperty(stringStringEntry.getKey(), stringStringEntry.getValue());
                    } else {
                        if (stringStringEntry.getValue() != null) {
                            object.addProperty(stringStringEntry.getKey().substring(1), stringStringEntry.getValue());
                        }
                        else {
                            object.addProperty(stringStringEntry.getKey().substring(1), "");
                        }
                    }
                }
                object.objURI = new ObjURI(type.name, objectsForUri);
                object.type = type;
                objects.put(object.objURI.toString(), object);
                logger.debug("find object " + object.objURI.toString());
            }

        } catch (Exception e) {
            throw new SqlBootException(e);
        }

        if (objURI.getParams() != null) {
            Map<String, String> filtersParam = objURI.getParams().entrySet().stream().filter(p ->
                !p.getKey().equalsIgnoreCase("type"))
                .collect(toMap(p -> p.getKey(), p -> p.getValue()));

            for (Entry<String, String> param : filtersParam.entrySet()) {
                if (param.getKey().startsWith("@")) {
                    objects = objects.entrySet().stream().filter(
                    o -> o.getValue().getHeaders().getProperty(param.getKey().substring(1))
                        .contains(param.getValue()))
                    .collect(toMap(o -> o.getKey(), o -> o.getValue()));
                }
            }
        }

        return objects;
    }

}