package com.github.mgramin.sqlboot.rest;

import com.github.mgramin.sqlboot.model.DBSchemaObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.mgramin.sqlboot.util.ZipHelper.compress;

/**
 * Created by mgramin on 17.12.2016.
 */
public class ZipAggregator implements IAggregator {

    @Override
    public byte[] aggregate(List<DBSchemaObject> objects) {
        Map<String, byte[]> files = new HashMap<>();
        for (DBSchemaObject o : objects) {
            if (o.getProp("file_name") != null && !o.getProp("file_name").isEmpty())
                files.put(o.getProp("file_name").toLowerCase(), o.ddl.getBytes());
        }
        return compress(files);
    }

}
