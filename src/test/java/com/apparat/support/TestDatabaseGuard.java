package com.apparat.support;

import com.apparat.config.DatabaseConnection;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Runs before every test class. Several tests insert and delete rows, so they
 * must never run against the database used for the demo. This stops them
 * unless the database name ends with "_test".
 *
 * mvn test already points at apparat2_test. When running a test from an IDE,
 * add this VM option to the run configuration:
 *   -Dapparat.db.url=jdbc:mysql://127.0.0.1:3308/apparat2_test
 */
public class TestDatabaseGuard implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        String url = DatabaseConnection.getUrl();

        if (!url.endsWith("_test")) {
            throw new IllegalStateException(
                    "Tests must run against a database whose name ends with "
                            + "'_test', but the current one is " + url + ". "
                            + "Use 'mvn test', or add the VM option "
                            + "-Dapparat.db.url=jdbc:mysql://127.0.0.1:3308/"
                            + "apparat2_test to your run configuration."
            );
        }
    }
}
