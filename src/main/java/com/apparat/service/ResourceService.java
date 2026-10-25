package com.apparat.service;

import com.apparat.dao.ResourceDao;
import com.apparat.model.Resource;

import java.sql.SQLException;
import java.util.List;

public class ResourceService {

    private final ResourceDao resourceDao = new ResourceDao();

    public List<Resource> listResources() throws SQLException {
        return resourceDao.findAll();
    }
}
