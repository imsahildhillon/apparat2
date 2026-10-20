package com.apparat.dao;
import com.apparat.model.Resource;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ResourceDaoScratch{

    public static void main(String[] args) throws SQLException {
        ResourceDao resourceDao = new ResourceDao();

        System.out.println("All resources:");

        List<Resource> resources = resourceDao.findAll();

        for (Resource resource : resources) {
            System.out.println(
                    resource.getId() + " | "
                            + resource.getName() + " | "
                            + resource.getLocation() + " | "
                            + resource.getStatus()
            );
        }

        System.out.println("\nLooking up resource ID 1:");

        Optional<Resource> resource = resourceDao.findById(1);

        if (resource.isPresent()) {
            System.out.println(resource.get().getName());
        } else {
            System.out.println("Resource not found.");
        }

        System.out.println("\nLooking up a nonexistent resource:");

        Optional<Resource> missing = resourceDao.findById(-999);

        System.out.println(
                missing.isEmpty()
                        ? "Not found, as expected."
                        : "Unexpected result."
        );
    }
}