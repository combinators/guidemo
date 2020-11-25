package org.combinators.guidemo.domain.instances;

import org.combinators.guidemo.domain.CoffeeBar;
import org.combinators.guidemo.domain.Database;
import org.combinators.guidemo.domain.DatabaseType;
import org.combinators.guidemo.domain.MenuLayout;

import java.net.MalformedURLException;
import java.net.URL;

public class Dortmund extends CoffeeBar {
    public Dortmund() {
        try {
            super.setBranchName("Finest Coffee @ TU-Dortmund");
            super.setLogoLocation(new URL("https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Technische_Universit%C3%A4t_Dortmund_Logo.svg/320px-Technische_Universit%C3%A4t_Dortmund_Logo.svg.png"));
            super.setMenuLayout(MenuLayout.Any);

            Database db = new Database();
            db.setDatabaseLocation(DatabaseType.JDBC.defaultLocation);
            db.setDatabaseType(DatabaseType.JDBC);
            super.setProductDatabase(db);

        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
