package net.bnbdiscord.borderkit.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class DatabaseManager {
    private Dao<Jurisdiction, Long> jurisdictionDao;
    private Dao<Ruleset, Long> rulesetDao;

    public DatabaseManager() {
        try {
            JdbcPooledConnectionSource connectionSource = new JdbcPooledConnectionSource("jdbc:sqlite:borderkit.db");

            TableUtils.createTableIfNotExists(connectionSource, Jurisdiction.class);
            jurisdictionDao = DaoManager.createDao(connectionSource, Jurisdiction.class);

            TableUtils.createTableIfNotExists(connectionSource, Ruleset.class);
            rulesetDao = DaoManager.createDao(connectionSource, Ruleset.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Dao<Jurisdiction, Long> getJurisdictionDao() {
        return jurisdictionDao;
    }

    public Dao<Ruleset, Long> getRulesetDao() {
        return rulesetDao;
    }
}
