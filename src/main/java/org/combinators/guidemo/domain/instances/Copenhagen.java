package org.combinators.guidemo.domain.instances;

import org.combinators.guidemo.domain.CoffeeBar;
import org.combinators.guidemo.domain.Database;
import org.combinators.guidemo.domain.DatabaseType;
import org.combinators.guidemo.domain.MenuLayout;

import java.net.MalformedURLException;
import java.net.URL;

public class Copenhagen extends CoffeeBar {
    public Copenhagen() {
        try {
            super.setBranchName("Finest Coffee @ DIKU");
            super.setLogoLocation(new URL("https://upload.wikimedia.org/wikipedia/en/5/59/DIKU_logo.png"));
            super.setMenuLayout(MenuLayout.RadioButtons);

            Database db = new Database();
            db.setDatabaseLocation(DatabaseType.RestJSON.defaultLocation);
            db.setDatabaseType(DatabaseType.RestJSON);
            super.setProductDatabase(db);

        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
